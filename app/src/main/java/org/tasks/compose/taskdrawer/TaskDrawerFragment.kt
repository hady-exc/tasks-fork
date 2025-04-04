package org.tasks.compose.taskdrawer

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskMover
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.compose.edit.TaskEditDrawer
import org.tasks.compose.edit.TaskEditDrawerState
import org.tasks.data.Location
import org.tasks.data.createGeofence
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.FORCE_CALDAV_SYNC
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.getLocation
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_DAY
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.GtasksFilter
import org.tasks.location.GeofenceApi
import org.tasks.location.LocationPickerActivity.Companion.launch
import org.tasks.location.LocationPickerActivity.Companion.registerForLocationPickerResult
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.tags.TagPickerActivity
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.ui.TaskListEvent
import org.tasks.ui.TaskListEventBus
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TaskDrawerFragment(val filter: Filter): DialogFragment() {
    @Inject lateinit var context: Activity
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var geofenceApi: GeofenceApi
    @Inject lateinit var gCalHelper: GCalHelper
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var alarmService: AlarmService
    @Inject lateinit var taskMover: TaskMover
    @Inject lateinit var taskAttachmentDao: TaskAttachmentDao
    @Inject lateinit var taskListEvents: TaskListEventBus

    private lateinit var taskEditDrawerState: TaskEditDrawerState
    private lateinit var filterPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var tagsPickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val composeView = ComposeView(context)
        composeView.setContent { TaskEditDrawerContent() }
        initLaunchers()
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taskEditDrawerState = TaskEditDrawerState(filter)
        lifecycleScope.launch {
            val task = taskCreator.createWithValues(filter, "")
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

            val targetList = defaultFilterProvider.getList(task)
            val currentLocation = locationDao.getLocation(task, preferences)
            val currentTags = tagDataDao.getTags(task)
            val currentAlarms = alarmDao.getAlarms(task)

            taskEditDrawerState.setTask(
                task,
                targetList,
                currentLocation,
                currentTags,
                currentAlarms
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_START_DATE -> if (resultCode == RESULT_OK) {
                data?.let { intent ->
                    taskEditDrawerState.startDay = intent.getLongExtra(EXTRA_DAY, 0L)
                    taskEditDrawerState.startTime = intent.getIntExtra(EXTRA_TIME, 0)
                }
            }

            else -> {
                @Suppress("DEPRECATION")
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    private fun initLaunchers() // must be called from onCreateView
    {
        filterPickerLauncher = registerForListPickerResult { list ->
            taskEditDrawerState.filter.value = list
        }
        locationPickerLauncher = registerForLocationPickerResult { place ->
            val location = taskEditDrawerState.location
            val geofence = if (location == null) {
                createGeofence(place.uid, preferences)
            } else {
                val existing = location.geofence
                Geofence(
                    place = place.uid,
                    isArrival = existing.isArrival,
                    isDeparture = existing.isDeparture,
                )
            }
            taskEditDrawerState.location = Location(geofence, place)
        }
        tagsPickerLauncher = registerForActivityResult<Intent,ActivityResult>(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { intent ->
                @Suppress("DEPRECATION")
                (intent.getParcelableArrayListExtra<TagData>(TagPickerActivity.EXTRA_SELECTED)
                    ?: ArrayList<TagData>())
                    .let {
                        taskEditDrawerState.selectedTags = it
                    }
            }
        }
    }

    private fun launchTagPicker(context: Context, current: ArrayList<TagData>)
    {
        tagsPickerLauncher.launch(
            Intent(context, TagPickerActivity::class.java)
                .putParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED, current)
        )
    }

    /* Dialog fragments launching and listening. Hack. TODO(replace by a regular solution)  */
    private val REQUEST_START_DATE = 11011 // StartDateControlSet.REQUEST_START_DATE
    private val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker" // StartDateControlSet.FRAG_TAG_DATE_PICKER

    private fun launchStartDateTimePicker(date: Long, time: Int)
    {
        val fragmentManager = parentFragmentManager
        if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
            StartDatePicker.newDateTimePicker(
                this,
                REQUEST_START_DATE,
                date,
                time,
                preferences.getBoolean(
                    R.string.p_auto_dismiss_datetime_edit_screen,
                    false
                )
            )
                .show(fragmentManager, FRAG_TAG_DATE_PICKER)
        }
    }

    private fun sendTaskToEdit(task: Task) {
        val intent = Intent()
        intent.putExtra(EXTRA_TASK,task)
        targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, intent)
    }

    private fun close() = dismiss()

    /** This is technically a copy of the TaskEditViewModel.save(), specialized with Task.isNew == true */
    suspend fun saveNewTask(
        filter: Filter,
        task: Task,
        location: Location? = null,
        selectedCalendar: String? = null,
        selectedAlarms: List<Alarm> = emptyList<Alarm>(),
        selectedAttachments: List<TaskAttachment> = emptyList<TaskAttachment>()
    ) = withContext(NonCancellable) {
/*
        TODO: Get sure that all tasks came here have changes, e.g. UI calls this fun only when user changed something
*/
        if (task.title.isNullOrBlank()) task.title = resources.getString(R.string.no_title)

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

        assert(filter is CaldavFilter || filter is GtasksFilter)
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TaskEditDrawerContent()
    {
        TasksTheme {
            var promptDiscard by remember { mutableStateOf(false) }
            PromptDiscard(
                show = promptDiscard,
                cancel = { promptDiscard = false },
                discard = { close() }
            )

            BottomSheet(
                dismiss = { close() },
                onDismissRequest = {
                    if (taskEditDrawerState.isChanged()) promptDiscard = true
                    else close()
                },
                hideConfirmation = {
                    if (taskEditDrawerState.isChanged()) {
                        promptDiscard = true
                        false
                    } else {
                        true
                    }
                }
            ) { hide ->
                TaskEditDrawer(
                    state = taskEditDrawerState,
                    save = {
                        lifecycleScope.launch {
                            saveNewTask(
                                filter = taskEditDrawerState.filter.value,
                                task = taskEditDrawerState.retrieveTask(),
                                location = taskEditDrawerState.location
                            )
                        }
                    },
                    edit = {
                        sendTaskToEdit(taskEditDrawerState.retrieveTask())
                        hide()
                    },
                    close = hide,
                    pickList = {
                        filterPickerLauncher.launch(
                            context = requireContext(),
                            selectedFilter = taskEditDrawerState.filter.value,
                            listsOnly = true
                        )
                    },
                    pickTags = { launchTagPicker(requireContext(),taskEditDrawerState.selectedTags) },
                    pickLocation = {
                        locationPickerLauncher.launch(
                            context = requireContext(),
                            selectedLocation = taskEditDrawerState.location
                        )
                    },
                    pickStartDateTime = {
                        launchStartDateTimePicker(taskEditDrawerState.startDay, taskEditDrawerState.startTime)
                    }
                )
            }
        }
    }

    companion object {
        const val FRAG_TAG_TASK_DRAWER = "frag_tag_task_drawer"
        const val EXTRA_FILTER = "extra_filter"
        const val EXTRA_TASK = "extra_task"
        const val REQUEST_EDIT_TASK = 11100

        fun newTaskDrawer(target: Fragment, rc: Int, filter: Filter): DialogFragment {
            return TaskDrawerFragment(filter).apply {
                setTargetFragment(target, rc)
            }
        }
    }
}