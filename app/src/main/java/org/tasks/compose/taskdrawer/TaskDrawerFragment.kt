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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.activity.TaskEditFragment
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskMover
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.calendars.CalendarPicker
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.compose.edit.TaskEditDrawer
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
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_DAY
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_TIME
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.GtasksFilter
import org.tasks.location.GeofenceApi
import org.tasks.location.LocationPickerActivity.Companion.launch
import org.tasks.location.LocationPickerActivity.Companion.registerForLocationPickerResult
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.repeats.CustomRecurrenceActivity
import org.tasks.tags.TagPickerActivity
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import org.tasks.ui.TaskListEventBus
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
    @Inject lateinit var caldavDao: CaldavDao

    private lateinit var filterPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var tagsPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var customRecurrencePickerLauncher: ActivityResultLauncher<Intent>

    val vm: TaskDrawerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        vm.initFilter(filter)
        val composeView = ComposeView(context)
        composeView.setContent { TaskEditDrawerContent() }
        initLaunchers()
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.resetTask()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_START_DATE -> if (resultCode == RESULT_OK) {
                data?.let { intent ->
                    vm.startDay = intent.getLongExtra(EXTRA_DAY, 0L)
                    vm.startTime = intent.getIntExtra(EXTRA_TIME, 0)
                }
            }
            TaskEditFragment.REQUEST_CODE_PICK_CALENDAR -> {
                if (resultCode == RESULT_OK) {
                    vm.selectedCalendar =
                        data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID)
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
        filterPickerLauncher = registerForListPickerResult { list -> vm.setFilter(list) }
        locationPickerLauncher = registerForLocationPickerResult { place ->
            val location = vm.location
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
            vm.location = Location(geofence, place)
        }
        tagsPickerLauncher = registerForActivityResult<Intent,ActivityResult>(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { intent ->
                @Suppress("DEPRECATION")
                (intent.getParcelableArrayListExtra<TagData>(TagPickerActivity.EXTRA_SELECTED)
                    ?: ArrayList<TagData>())
                    .let {
                        vm.selectedTags = it
                    }
            }
        }
        customRecurrencePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val result = result.data?.getStringExtra(CustomRecurrenceActivity.EXTRA_RRULE)
                    vm.recurrence = result
                    if (result?.isNotBlank() == true && vm.dueDate == 0L ) {
                        vm.dueDate = (currentTimeMillis().startOfDay())
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

    private fun launchCalendarPicker() {
        CalendarPicker
            .newCalendarPicker(
                this,
                TaskEditFragment.REQUEST_CODE_PICK_CALENDAR,
                vm.selectedCalendar,
            )
            .show(
                this.parentFragmentManager,
                TaskEditFragment.FRAG_TAG_CALENDAR_PICKER
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
                    if (vm.isChanged()) promptDiscard = true
                    else dismiss()
                },
                hideConfirmation = {
                    if (vm.isChanged()) {
                        promptDiscard = true
                        false
                    } else {
                        true
                    }
                }
            ) { hide ->
                TaskEditDrawer(
                    state = vm,
                    save = {
                        lifecycleScope.launch {
                            vm.saveTask(
                                filter = vm.filter.value,
                                task = vm.retrieveTask(),
                                location = vm.location
                            )
                        }
                    },
                    edit = {
                        sendTaskToEdit(vm.retrieveTask())
                        hide()
                    },
                    close = hide,
                    pickList = {
                        filterPickerLauncher.launch(
                            context = requireContext(),
                            selectedFilter = vm.filter.value,
                            listsOnly = true
                        )
                    },
                    pickTags = { launchTagPicker(requireContext(),vm.selectedTags) },
                    pickLocation = {
                        locationPickerLauncher.launch(
                            context = requireContext(),
                            selectedLocation = vm.location
                        )
                    },
                    pickStartDateTime = {
                        launchStartDateTimePicker(vm.startDay, vm.startTime)
                    },
                    pickCustomRecurrence = {
                        lifecycleScope.launch {
                            val accountType = vm.filter.value
                                .let {
                                    when (it) {
                                        is CaldavFilter -> it.account
                                        is GtasksFilter -> it.account
                                        else -> null
                                    }
                                }
                                ?.let { caldavDao.getAccountByUuid(it) }
                                ?.accountType
                                ?: CaldavAccount.TYPE_LOCAL

                            customRecurrencePickerLauncher.launch(
                                CustomRecurrenceActivity.newIntent(
                                    context = requireContext(),
                                    rrule = vm.recurrence,
                                    dueDate = vm.dueDate,
                                    accountType = accountType
                                )
                            )
                        }
                    },
                    pickCalendar = { launchCalendarPicker() }
                )
            }
        }
    }

    companion object {
        const val FRAG_TAG_TASK_DRAWER = "frag_tag_task_drawer"
        const val EXTRA_TASK = "extra_task"
        const val REQUEST_EDIT_TASK = 11100

        fun newTaskDrawer(target: Fragment, rc: Int, filter: Filter): DialogFragment {
            return TaskDrawerFragment(filter).apply {
                setTargetFragment(target, rc)
            }
        }
    }
}