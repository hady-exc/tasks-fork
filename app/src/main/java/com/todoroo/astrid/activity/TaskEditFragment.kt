package com.todoroo.astrid.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.calendars.CalendarPicker
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.edit.TaskEditScreen
import org.tasks.compose.taskdrawer.BottomSheet
import org.tasks.compose.taskdrawer.PromptDiscard
//import org.tasks.compose.taskdrawer.TaskDrawerFragment
import org.tasks.compose.taskdrawer.TaskEditDrawer
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.dialogs.DateTimePicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.Linkify
import org.tasks.filters.CaldavFilter
import org.tasks.markdown.MarkdownProvider
import org.tasks.notifications.NotificationManager
import org.tasks.play.PlayServices
import org.tasks.preferences.Preferences
import org.tasks.repeats.CustomRecurrenceActivity
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import org.tasks.ui.ChipProvider
import org.tasks.ui.TaskEditViewModel
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TaskEditFragment : Fragment() {
    @Inject lateinit var userActivityDao: UserActivityDao
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var linkify: Linkify
    @Inject lateinit var locale: Locale
    @Inject lateinit var chipProvider: ChipProvider
    @Inject lateinit var playServices: PlayServices
    @Inject lateinit var theme: Theme

    private val editViewModel: TaskEditViewModel by viewModels()
    private val mainViewModel: MainActivityViewModel by activityViewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = content {
        TasksTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val viewState = editViewModel.viewState.collectAsStateWithLifecycle().value
            if (editViewModel.rowView.value) {
                LaunchedEffect(viewState.isNew) {
                    if (!viewState.isNew) {
                        notificationManager.cancel(viewState.task.id)
                    }
                }
                val context = LocalContext.current
                val keyboard = LocalSoftwareKeyboardController.current

                TaskEditScreen(
                    editViewModel = editViewModel,
                    viewState = viewState,
                    comments = userActivityDao
                        .watchComments(viewState.task.uuid)
                        .collectAsStateWithLifecycle(emptyList())
                        .value,
                    save = {
                        keyboard?.hide()
                        lifecycleScope.launch { save() }
                    },
                    discard = {
                        keyboard?.hide()
                        if (editViewModel.hasChanges()) {
                            dialogBuilder
                                .newDialog(R.string.discard_confirmation)
                                .setPositiveButton(R.string.keep_editing, null)
                                .setNegativeButton(R.string.discard) { _, _ -> discard() }
                                .show()
                        } else {
                            discard()
                        }
                    },
                    delete = {
                        keyboard?.hide()
                        dialogBuilder
                            .newDialog(R.string.DLG_delete_this_task_question)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                lifecycleScope.launch {
                                    editViewModel.delete()
                                    mainViewModel.setTask(null)
                                }
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    },
                    dismissBeastMode = { editViewModel.hideBeastModeHint(click = false) },
                    deleteComment = {
                        lifecycleScope.launch {
                            userActivityDao.delete(it)
                        }
                    },
                    markdownProvider = remember { MarkdownProvider(context, preferences) },
                    linkify = if (viewState.linkify) linkify else null,
                    onClickDueDate = {
                        DateTimePicker
                            .newDateTimePicker(
                                target = this@TaskEditFragment,
                                rc = REQUEST_DATE,
                                current = editViewModel.dueDate.value,
                                autoClose = preferences.getBoolean(
                                    R.string.p_auto_dismiss_datetime_edit_screen,
                                    false
                                ),
                                hideNoDate = viewState.task.isRecurring,
                            )
                            .show(parentFragmentManager, FRAG_TAG_DATE_PICKER)
                    },
                    colorProvider = { chipProvider.getColor(it) },
                    locale = remember { locale },
                )
            } else {
                editViewModel.rowView.value = false

                var promptDiscard by remember { mutableStateOf(false) }
                PromptDiscard(
                    show = promptDiscard,
                    cancel = { promptDiscard = false },
                    discard = { discard() }
                )

                BottomSheet(
                    dismiss = { discard() },
                    onDismissRequest = {
                        if (editViewModel.hasChanges()) promptDiscard = true
                        else discard()
                    },
                    hideConfirmation = {
                        if (editViewModel.hasChanges()) {
                            promptDiscard = true
                            false
                        } else {
                            true
                        }
                    }
                ) { hide ->
                    TaskEditDrawer(
                        state = editViewModel,
                        viewState = viewState,
                        save = {
                            lifecycleScope.launch {
                                save()
                                // TODO() call viewModel.reset()
                            }
                        },
/*                      {
                            lifecycleScope.launch {
                                editViewModel.saveTask(
                                    filter = vm.filter.value,
                                    task = vm.retrieveTask(),
                                    location = vm.location
                                )
                            }
*/

                        edit = {
                            editViewModel.rowView.value = true
/*
                            sendTaskToEdit(vm.retrieveTask())
                            hide()
*/
                        },
                        close = hide,
                        pickList = {
/*
                            filterPickerLauncher.launch(
                                context = requireContext(),
                                selectedFilter = vm.filter.value,
                                listsOnly = true
                            )
*/
                        },
                        pickTags = {},  // this@TaskDrawerFragment::pickTags,
                        pickLocation = {}, //this@TaskDrawerFragment::pickLocation,
                        pickStartDateTime = {
                            //launchStartDateTimePicker(vm.startDay, vm.startTime)
                        },
                        pickCustomRecurrence = {},
/*
                        {
                            lifecycleScope.launch {
                                val accountType = vm.filter.value
                                    .let {
                                        if (it is CaldavFilter) it.account else null
                                    }
                                    ?.let { caldavDao.getAccountByUuid(it.uuid!!) }
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
*/
                        pickCalendar = {}, //this@TaskDrawerFragment::pickCalendar,
                        setTimer = {} //this@TaskDrawerFragment::setTimer
                    )
                }
            }
        }
    }

    suspend fun save(remove: Boolean = true) {
        editViewModel.save()
        if (remove) {
            mainViewModel.setTask(null)
        }
        activity?.let { playServices.requestReview(it) }
    }

    private fun discard() = lifecycleScope.launch {
        editViewModel.discard()
        mainViewModel.setTask(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_DATE -> {
                if (resultCode == Activity.RESULT_OK) {
                    editViewModel.setDueDate(data!!.getLongExtra(DateTimePicker.EXTRA_TIMESTAMP, 0L))
                }
            }
            REQUEST_CODE_PICK_CALENDAR -> {
                if (resultCode == Activity.RESULT_OK) {
                    editViewModel.setCalendar(data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID))
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val EXTRA_TASK = "extra_task"

        const val FRAG_TAG_CALENDAR_PICKER = "frag_tag_calendar_picker"
        private const val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker"
        const val REQUEST_CODE_PICK_CALENDAR = 70
        private const val REQUEST_DATE = 504
    }
}
