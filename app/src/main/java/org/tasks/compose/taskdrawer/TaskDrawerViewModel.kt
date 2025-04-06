package org.tasks.compose.taskdrawer

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskDeleter
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.timers.TimerPlugin
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.calendars.CalendarEventProvider
import org.tasks.data.GoogleTask
import org.tasks.data.Location
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.FORCE_CALDAV_SYNC
import org.tasks.data.entity.Place
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.getLocation
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.GtasksFilter
import org.tasks.location.GeofenceApi
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay
import org.tasks.ui.MainActivityEventBus
import org.tasks.ui.TaskListEvent
import org.tasks.ui.TaskListEventBus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TaskDrawerViewModel
@Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val taskDeleter: TaskDeleter,
    private val timerPlugin: TimerPlugin,
    private val permissionChecker: PermissionChecker,
    private val calendarEventProvider: CalendarEventProvider,
    private val gCalHelper: GCalHelper,
    private val taskMover: TaskMover,
    private val locationDao: LocationDao,
    private val geofenceApi: GeofenceApi,
    private val tagDao: TagDao,
    private val tagDataDao: TagDataDao,
    private val preferences: Preferences,
    private val googleTaskDao: GoogleTaskDao,
    private val caldavDao: CaldavDao,
    private val taskCompleter: TaskCompleter,
    private val alarmService: AlarmService,
    private val taskListEvents: TaskListEventBus,
    private val mainActivityEvents: MainActivityEventBus,
    private val firebase: Firebase? = null,
    private val userActivityDao: UserActivityDao,
    private val alarmDao: AlarmDao,
    private val taskAttachmentDao: TaskAttachmentDao,
    private val taskCreator: TaskCreator,
    private val defaultFilterProvider: DefaultFilterProvider,
    ) : ViewModel()
{
    /* "Static" state */
    private lateinit var _originalFilter: Filter  // TODO: rethink, try to avoid originalFilter
    val originalFilter get() = _originalFilter
    private lateinit var _initialFilter: Filter
    val initialFilter get() = _initialFilter
    private lateinit var initialTask: Task
    private lateinit var initialList: Filter
    private var initialLocation: Location? = null
    private lateinit var _initialTags: ArrayList<TagData>
    val initialTags get() = _initialTags
    private lateinit var initialAlarms: ArrayList<Alarm>
    private lateinit var _filter: MutableState<Filter>

    private var initializer: Job? = null
    fun initFilter(filter: Filter) {
        _originalFilter = filter
        _initialFilter = filter
        this._filter = mutableStateOf(filter)
        initializer = viewModelScope.launch {
            initialTask = taskCreator.createWithValues(filter, "")
            initialTask.hideUntil = when (preferences.getIntegerFromString(
                R.string.p_default_hideUntil_key,
                Task.HIDE_UNTIL_NONE
            )) {
                Task.HIDE_UNTIL_DUE -> DUE_DATE
                Task.HIDE_UNTIL_DUE_TIME -> DUE_TIME
                Task.HIDE_UNTIL_DAY_BEFORE -> DAY_BEFORE_DUE
                Task.HIDE_UNTIL_WEEK_BEFORE -> WEEK_BEFORE_DUE
                else -> 0L
            }

            initialList = defaultFilterProvider.getList(initialTask)
            initialLocation = locationDao.getLocation(initialTask, preferences)
            _initialTags = tagDataDao.getTags(initialTask)
            initialAlarms = alarmDao.getAlarms(initialTask) // TODO: rethink, may be just an empty list?
        }
    }

    /** State for @composable components */
    lateinit var task: Task
    private val initialTitle get() = initialTask.title ?: ""
    private val initialDescription get() = initialTask.notes ?: ""
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var dueDate by mutableLongStateOf(0L)
    var priority by mutableIntStateOf(0)
    var selectedTags by mutableStateOf<ArrayList<TagData>>( ArrayList() )
    var location by mutableStateOf<Location?>(null)
    val filter: State<Filter> get() = _filter
    var startDay by mutableLongStateOf(0L)
    var startTime by mutableIntStateOf(0)
    val startDate: Long get() {
        if (dueDate == 0L && startDay < 0) return 0L
        val due = dueDate.takeIf { it > 0 }?.toDateTime()
        return when (startDay) {
            DUE_DATE -> due?.withMillisOfDay(startTime)?.millis ?: 0
            DUE_TIME -> due?.millis ?: 0
            DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(startTime)?.millis ?: 0
            WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(startTime)?.millis ?: 0
            else -> startDay + startTime
        }
    }

    fun setFilter(value: Filter) { _filter.value = value }

    fun resetTask() {
        viewModelScope.launch {
            initializer?.join()
            task = initialTask.copy(remoteId = Task.NO_UUID)

            title = initialTitle
            description = initialDescription
            dueDate = task.dueDate
            location = initialLocation
            priority = task.priority
            selectedTags = initialTags
            task.hideUntil = when (preferences.getIntegerFromString(
                R.string.p_default_hideUntil_key,
                Task.HIDE_UNTIL_NONE
            )) {
                Task.HIDE_UNTIL_DUE -> DUE_DATE
                Task.HIDE_UNTIL_DUE_TIME -> DUE_TIME
                Task.HIDE_UNTIL_DAY_BEFORE -> DAY_BEFORE_DUE
                Task.HIDE_UNTIL_WEEK_BEFORE -> WEEK_BEFORE_DUE
                else -> 0L
            }
            setStartDate(task.dueDate, task.hideUntil)
            _initialFilter = defaultFilterProvider.getList(task)
            setFilter(_initialFilter)
        }
    }

    private fun setStartDate(dueDate: Long, startDate: Long)
    {
        if (startDate <= 0L) {
            startDay = startDate
            startTime = 0
        } else {
            val dateTime = startDate.toDateTime()
            val dueDay = dueDate.startOfDay()
            startDay = dateTime.startOfDay().millis
            startTime = dateTime.millisOfDay
            startDay = when (startDay) {
                dueDay -> if (startTime == dueDate.millisOfDay) {
                    startTime = StartDatePicker.NO_TIME
                    DUE_TIME
                } else {
                    DUE_DATE
                }
                dueDay.toDateTime().minusDays(1).millis ->
                    DAY_BEFORE_DUE
                dueDay.toDateTime().minusDays(7).millis ->
                    WEEK_BEFORE_DUE
                else -> startDay
            }
        }
    }

    fun tagsChanged(): Boolean = (initialTags.toHashSet() != selectedTags.toHashSet())

    fun isChanged(): Boolean =
        title.trim() != initialTitle.trim()
            || description.trim() != initialDescription.trim()
            || dueDate != initialTask.dueDate
            || filter.value != initialFilter
            || initialLocation != location
            || initialTask.priority != priority
            || tagsChanged()
            || initialTask.hideUntil != startDate

    fun retrieveTask(): Task {
        task.title = title
        task.notes = description
        task.dueDate = dueDate
        task.priority = priority
        task.hideUntil = startDate
        location?.let { location ->
            task.putTransitory(Place.KEY, location.place.uid!!)
        }
        when (filter.value) {
            is GtasksFilter -> task.putTransitory(
                GoogleTask.KEY,
                (filter.value as GtasksFilter).remoteId
            )
            is CaldavFilter -> task.putTransitory(
                CaldavTask.KEY,
                (filter.value as CaldavFilter).uuid
            )
            else -> {}
        }
        task.putTransitory(Tag.KEY, selectedTags.mapNotNull { it.name })
        if (isChanged()) task.putTransitory(Task.TRANS_IS_CHANGED, "")
        return task
    }

    /** This is technically a copy of the TaskEditViewModel.save(), specialized with Task.isNew == true */
    suspend fun saveTask(
        filter: Filter,
        task: Task,

        location: Location? = null,
        selectedCalendar: String? = null,
        selectedAlarms: List<Alarm> = emptyList<Alarm>(),
        selectedAttachments: List<TaskAttachment> = emptyList<TaskAttachment>()
    ) = withContext(NonCancellable) {
        /* TODO: Get sure that all tasks came here have changes, e.g. UI calls this fun only when user changed something */
        if (task.title.isNullOrBlank()) task.title = context.getString(R.string.no_title)

        val currentLocation = location
        val tags = task.tags.mapNotNull { tagDataDao.getTagByName(it) }

        /* applyCalendarChanges() -- inlined below */
        if (permissionChecker.canAccessCalendars()) {
            if (task.hasDueDate()) {
                selectedCalendar?.let {
                    try {
                        task.calendarURI = gCalHelper.createTaskEvent(task, it)?.toString()
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
        }

        taskDao.createNew(task)

        currentLocation?.let { location ->
            val place = location.place
            locationDao.insert(
                location.geofence.copy(
                    task = task.id,
                    place = place.uid,
                )
            )
            geofenceApi.update(place)
            task.putTransitory(FORCE_CALDAV_SYNC, true)
            task.modificationDate = currentTimeMillis()
        }

        if (tags.isNotEmpty()) {
            tagDao.applyTags(task, tagDataDao, tags)
            task.modificationDate = currentTimeMillis()
        }

        var _selectedAlarms = selectedAlarms
        if (!task.hasStartDate()) {
            _selectedAlarms = _selectedAlarms.filterNot { a -> a.type == TYPE_REL_START }
        }
        if (!task.hasDueDate()) {
            _selectedAlarms = selectedAlarms.filterNot { a -> a.type == TYPE_REL_END }
        }
        if (_selectedAlarms.isNotEmpty()) {
            alarmService.synchronizeAlarms(task.id, _selectedAlarms.toMutableSet())
            task.putTransitory(FORCE_CALDAV_SYNC, true)
            task.modificationDate = currentTimeMillis()
        }

        taskDao.save(task, null)

        assert(filter is CaldavFilter || filter is GtasksFilter)  // already helped one time
        task.parent = 0
        taskMover.move(listOf(task.id), filter)

        /* Subtasks are not supposed to be created or edited before this save */

        if (selectedAttachments.isNotEmpty()) {
            selectedAttachments
                .map {
                    Attachment(
                        task = task.id,
                        fileId = it.id!!,
                        attachmentUid = it.remoteId,
                    )
                }
                .let { taskAttachmentDao.insert(it) }
        }

        val model = task
        taskListEvents.emit(TaskListEvent.TaskCreated(model.uuid))
        model.calendarURI?.takeIf { it.isNotBlank() }?.let {
            taskListEvents.emit(TaskListEvent.CalendarEventCreated(model.title, it))
        }
    }
}