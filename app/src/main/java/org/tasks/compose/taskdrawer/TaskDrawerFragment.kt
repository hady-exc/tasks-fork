package org.tasks.compose.taskdrawer

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
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
import androidx.core.content.IntentCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.activity.TaskEditFragment
import com.todoroo.astrid.activity.TaskListFragment.Companion.EXTRA_FILTER
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.timers.TimerPlugin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.calendars.CalendarPicker
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.data.Location
import org.tasks.data.createGeofence
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_DAY
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.location.GeofenceApi
import org.tasks.location.LocationPickerActivity
import org.tasks.location.LocationPickerActivity.Companion.EXTRA_PLACE
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.repeats.CustomRecurrenceActivity
import org.tasks.tags.TagPickerActivity
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay
import org.tasks.ui.TaskEditViewModel
import org.tasks.ui.TaskEditViewModel.Companion.TASK_EDIT_CONTROL_SET_FRAGMENTS
import org.tasks.ui.TaskListEventBus
import javax.inject.Inject

@AndroidEntryPoint
class TaskDrawerFragment: DialogFragment() {
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
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var timerPlugin: TimerPlugin

    private lateinit var filterPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var tagsPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var customRecurrencePickerLauncher: ActivityResultLauncher<Intent>

    //val vm: TaskDrawerViewModel by viewModels()
    val vm: TaskEditViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
/*
        val arguments = requireArguments()
        val filter = arguments.getParcelable<Filter>(EXTRA_FILTER)!!
        val task = arguments.getParcelable<Task>(EXTRA_TASK)!!
        vm.initViewModel(filter, task, getOrder())
*/
        val composeView = ComposeView(context)
        composeView.setContent { TaskEditDrawerContent() }
        initLaunchers()
        return composeView
    }

    private fun getOrder(): List<Int> {
        return TASK_EDIT_CONTROL_SET_FRAGMENTS
            .associateBy { context.getString(it) }
            .let { controlSetStrings ->
                BeastModePreferences
                    .constructOrderedControlList(preferences, context)
                    .let { items ->
                        items
                            .subList(
                                0,
                                items.indexOf(context.getString(R.string.TEA_ctrl_hide_section_pref))
                            )
                            .also { it.add(0, context.getString(R.string.TEA_ctrl_title)) }
                    }
                    .mapNotNull { controlSetStrings[it] }
                    .toPersistentList()
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //vm.resetTask()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_START_DATE -> if (resultCode == RESULT_OK) {
                data?.let { intent ->
                    val date = intent.getLongExtra(EXTRA_DAY, 0L)
                    val time = intent.getIntExtra(EXTRA_TIME, 0)
                    val due = vm.dueDate.value.takeIf { it > 0 }?.toDateTime()
                    vm.setStartDate(
                        when (date) {
                            DUE_DATE -> due?.withMillisOfDay(time)?.millis ?: 0
                            DUE_TIME -> due?.millis ?: 0
                            DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(time)?.millis ?: 0
                            WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(time)?.millis ?: 0
                            else -> date + time
                        }
                    )
                }
            }
            TaskEditFragment.REQUEST_CODE_PICK_CALENDAR -> {
                if (resultCode == RESULT_OK) {
                    vm.setCalendar(data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID))
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
        filterPickerLauncher = registerForListPickerResult { list -> vm.setList(list) }
        tagsPickerLauncher = registerForActivityResult<Intent,ActivityResult>(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { intent ->
                @Suppress("DEPRECATION")
                (intent.getParcelableArrayListExtra<TagData>(TagPickerActivity.EXTRA_SELECTED)
                    ?: ArrayList<TagData>())
                    .let {
                        vm.setTags(it.toPersistentSet())
                    }
            }
        }
        customRecurrencePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val rrule = result.data?.getStringExtra(CustomRecurrenceActivity.EXTRA_RRULE)
                    vm.setRecurrence(rrule)
/*
                    if (rrule?.isNotBlank() == true && vm.dueDate == 0L ) {
                        vm.dueDate = (currentTimeMillis().startOfDay())
                    }
*/
                }
            }
        locationPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == RESULT_OK)
                    result.data?.let { intent ->
                        IntentCompat
                            .getParcelableExtra(intent, EXTRA_PLACE, Place::class.java)
                            ?.let { place ->
                                val location = vm.viewState.value.location
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
                                vm.setLocation(Location(geofence, place))
                            }
                    }
                }
    }

    private fun pickTags()
    {
        tagsPickerLauncher.launch(
            Intent(context, TagPickerActivity::class.java)
                .putParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED, ArrayList(vm.viewState.value.tags))
        )
    }

    private fun pickCalendar() {
        CalendarPicker
            .newCalendarPicker(
                this,
                TaskEditFragment.REQUEST_CODE_PICK_CALENDAR,
                vm.viewState.value.calendar,
            )
            .show(
                this.parentFragmentManager,
                TaskEditFragment.FRAG_TAG_CALENDAR_PICKER
            )

    }

    /* Dialog fragments launching and listening. Hack. TODO(replace by a regular solution)  */
    private val REQUEST_START_DATE = 11011 // StartDateControlSet.REQUEST_START_DATE
    private val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker" // StartDateControlSet.FRAG_TAG_DATE_PICKER

    private fun launchStartDateTimePicker(startDate: Long, dueDate: Long)
    {
        var day = 0L
        var time = 0
        val dueDay = dueDate.startOfDay()
        val dueTime = dueDate.millisOfDay
        if (startDate <= 0) {
            day = when (preferences.getIntegerFromString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_NONE)) {
                Task.HIDE_UNTIL_DUE -> DUE_DATE
                Task.HIDE_UNTIL_DUE_TIME -> DUE_TIME
                Task.HIDE_UNTIL_DAY_BEFORE -> DAY_BEFORE_DUE
                Task.HIDE_UNTIL_WEEK_BEFORE -> WEEK_BEFORE_DUE
                else -> 0L
            }
        } else {
            val hideUntil = startDate.toDateTime()
            day = hideUntil.startOfDay().millis
            time = hideUntil.millisOfDay
            day = when (day) {
                dueDay -> if (time == dueTime) {
                    time = StartDatePicker.NO_TIME
                    DUE_TIME
                } else {
                    DUE_DATE
                }
                dueDay.toDateTime().minusDays(1).millis ->
                    DAY_BEFORE_DUE
                dueDay.toDateTime().minusDays(7).millis ->
                    WEEK_BEFORE_DUE
                else -> day
            }
        }

        val fragmentManager = parentFragmentManager
        if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
            StartDatePicker.newDateTimePicker(
                this,
                REQUEST_START_DATE,
                day,
                time,
                preferences.getBoolean(
                    R.string.p_auto_dismiss_datetime_edit_screen,
                    false
                ),
                showDueDate = !vm.viewState.value.list.account.isOpenTasks,
            )
                .show(fragmentManager, FRAG_TAG_DATE_PICKER)
        }
    }

    private fun pickLocation() {
        locationPickerLauncher.launch(
            Intent(context, LocationPickerActivity::class.java)
                .putExtra(EXTRA_PLACE, vm.viewState.value.location?.place as Parcelable?)
        )

    }

    private fun sendTaskToEdit(task: Task) {
        val intent = Intent()
        intent.putExtra(EXTRA_TASK,task)
        targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, intent)
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
                discard = { dismiss() }
            )

            BottomSheet(
                dismiss = { dismiss() },
                onDismissRequest = {
                    if (vm.hasChanges()) promptDiscard = true
                    else dismiss()
                },
                hideConfirmation = {
                    if (vm.hasChanges()) {
                        promptDiscard = true
                        false
                    } else {
                        true
                    }
                }
            ) { dismiss ->
                TaskEditDrawer(
                    vm = vm,
                    state = vm.viewState.collectAsStateWithLifecycle(),
                    save = {
                        lifecycleScope.launch {
                            vm.save()
                            vm.resetToOriginal()
/*
                            vm.saveTask(
                                filter = vm.filter.value,
                                task = vm.retrieveTask(),
                                location = vm.location
                            )
*/
                        }
                    },
                    edit = {
                        sendTaskToEdit(vm.getTask())
                        dismiss()
                    },
                    close = dismiss,
                    pickList = {
                        filterPickerLauncher.launch(
                            context = requireContext(),
                            selectedFilter = vm.viewState.value.list,
                            listsOnly = true
                        )
                    },
                    pickTags = this@TaskDrawerFragment::pickTags,
                    pickLocation = this@TaskDrawerFragment::pickLocation,
                    pickStartDateTime = {
                        launchStartDateTimePicker(
                            startDate = vm.startDate.value,
                            dueDate = vm.dueDate.value
                        )
                    },
                    pickCustomRecurrence = {
                        lifecycleScope.launch {
                            val accountType = vm.viewState.value.list
                                .let {
                                    if (it is CaldavFilter) it.account else null
                                }
                                ?.let { caldavDao.getAccountByUuid(it.uuid!!) }
                                ?.accountType
                                ?: CaldavAccount.TYPE_LOCAL

                            customRecurrencePickerLauncher.launch(
                                CustomRecurrenceActivity.newIntent(
                                    context = requireContext(),
                                    rrule = vm.viewState.value.task.recurrence,
                                    dueDate = vm.viewState.value.task.dueDate,
                                    accountType = accountType
                                )
                            )
                        }
                    },
                    pickCalendar = this@TaskDrawerFragment::pickCalendar,
                    setTimer = this@TaskDrawerFragment::setTimer
                )
            }
        }
    }

    private fun setTimer(on: Boolean) {
        if (on) {
            if (vm.timerStarted.value == 0L) {
                vm.timerStarted.update { currentTimeMillis() }
            }
        } else {
            vm.timerStarted.update { 0L }
        }
    }

    companion object {
        const val FRAG_TAG_TASK_DRAWER = "frag_tag_task_drawer"
        const val EXTRA_TASK = "extra_task"
        const val REQUEST_EDIT_TASK = 11100

        fun newTaskDrawer(
            target: Fragment,
            rc: Int,
            filter: Filter,
            task: Task): DialogFragment {
            return TaskDrawerFragment().apply {
                setTargetFragment(target, rc)
                arguments = Bundle().apply {
                    putParcelable(EXTRA_TASK, task)
                    putParcelable(EXTRA_FILTER, filter)
                }
            }
        }
    }
}