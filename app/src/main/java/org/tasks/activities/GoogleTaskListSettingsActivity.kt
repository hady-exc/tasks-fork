package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.SnackbarHostState
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.textfield.TextInputEditText
import com.google.api.services.tasks.model.TaskList
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.dao.GoogleTaskListDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.databinding.ActivityGoogleTaskListSettingsBinding
import org.tasks.extensions.Context.hideKeyboard
import org.tasks.extensions.Context.toast
import org.tasks.filters.GtasksFilter
import org.tasks.themes.TasksIcons
import org.tasks.compose.drawer.ListSettingsDrawer
import org.tasks.compose.drawer.DrawerSnackBar
import org.tasks.compose.drawer.ListSettingsDrawer
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.GoogleTaskListDao
import org.tasks.themes.CustomIcons
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GoogleTaskListSettingsActivity : BaseListSettingsActivity() {
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var name: TextInputEditText
    private lateinit var progressView: ProgressBar

    private var isNewList = false
    private lateinit var gtasksList: CaldavCalendar
    private val createListViewModel: CreateListViewModel by viewModels()
    private val renameListViewModel: RenameListViewModel by viewModels()
    private val deleteListViewModel: DeleteListViewModel by viewModels()
    override val defaultIcon = TasksIcons.LIST

    override val compose: Boolean
        get() = true
    val snackbar = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {
        gtasksList = intent.getParcelableExtra(EXTRA_STORE_DATA)
                ?: CaldavCalendar(
                    account = intent.getParcelableExtra<CaldavAccount>(EXTRA_ACCOUNT)!!.username
                ).apply {
                    isNewList = true
                }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            selectedColor = gtasksList.color
            selectedIcon.update { gtasksList.icon }
        }

        if (!isNewList) textState.value = gtasksList.name!!

        /*
                if (isNewList) {
                    name.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT)
                } else {
                    name.setText(gtasksList.name)
                }
        */

        if (createListViewModel.inProgress
                || renameListViewModel.inProgress
                || deleteListViewModel.inProgress) {
            showProgressIndicator()
        }

        createListViewModel.observe(this, this::onListCreated, this::requestFailed)
        renameListViewModel.observe(this, this::onListRenamed, this::requestFailed)
        deleteListViewModel.observe(this, this::onListDeleted, this::requestFailed)

        setContent {
            MdcTheme {
                ListSettingsDrawer(
                    title = toolbarTitle,
                    isNew = isNewList,
                    text = textState,
                    error = errorState,
                    color = colorState,
                    icon = iconState,
                    delete = { lifecycleScope.launch { promptDelete() } },
                    save = { lifecycleScope.launch { save() } },
                    selectColor = { showThemePicker() },
                    clearColor = { clearColor() },
                    selectIcon = { showIconPicker() },
                    showProgress = showProgress
                )

                DrawerSnackBar(state = snackbar)

            }
        }

        updateTheme()
    }

    override val isNew: Boolean
        get() = isNewList

    override val toolbarTitle: String
        get() = if (isNew) "NEW " + getString(R.string.new_list) else gtasksList.name!!

    private fun showProgressIndicator() {
        showProgress.value = true
    }

    private fun hideProgressIndicator() {
        showProgress.value = false
    }

    private fun requestInProgress() = showProgress.value

    override suspend fun save() {
        if (requestInProgress()) {
            return
        }
        val newName = newName
        if (isNullOrEmpty(newName)) {
            errorState.value = getString(R.string.name_cannot_be_empty)
            return
        }
        when {
            isNewList -> {
                showProgressIndicator()
                createListViewModel.createList(gtasksList.account!!, newName)
            }
            nameChanged() -> {
                showProgressIndicator()
                renameListViewModel.renameList(gtasksList, newName)
            }
            else -> {
                if (colorChanged() || iconChanged()) {
                    gtasksList.color = selectedColor
                    googleTaskListDao.insertOrReplace(
                        gtasksList.copy(
                            icon = selectedIcon.value
                        )
                    )
                    localBroadcastManager.broadcastRefresh()
                    setResult(
                            Activity.RESULT_OK,
                            Intent(TaskListFragment.ACTION_RELOAD)
                                    .putExtra(MainActivity.OPEN_FILTER, GtasksFilter(gtasksList)))
                }
                finish()
            }
        }
    }

    override fun finish() {
        super.finish()
    }

    override fun bind(): View { TODO() }

/*
    override fun bind() = ActivityGoogleTaskListSettingsBinding.inflate(layoutInflater).let {
        name = it.name
        progressView = it.progressBar.progressBar
        it.root
    }
*/

    override fun promptDelete() {
        if (!requestInProgress()) {
            super.promptDelete()
        }
    }

    override suspend fun delete() {
        showProgressIndicator()
        deleteListViewModel.deleteList(gtasksList)
    }

    override fun discard() {
        if (!requestInProgress()) {
            super.discard()
        }
    }

    private val newName: String
        get() = textState.value.trim { it <= ' ' }

    override fun hasChanges(): Boolean =
        if (isNewList) {
            selectedColor >= 0 || !isNullOrEmpty(newName)
        } else colorChanged() || nameChanged() || iconChanged()

    private fun colorChanged() = selectedColor != gtasksList.color

    private fun iconChanged() = selectedIcon.value != gtasksList.icon

    private fun nameChanged() = newName != gtasksList.name

    private suspend fun onListCreated(taskList: TaskList) {
        val result = gtasksList.copy(
            uuid = taskList.id,
            name = taskList.title,
            color = selectedColor,
            icon = selectedIcon.value,
        )
        val id = googleTaskListDao.insertOrReplace(result)

        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(MainActivity.OPEN_FILTER, GtasksFilter(result.copy(id = id))))
        finish()
    }

    private fun onListDeleted(deleted: Boolean) {
        if (deleted) {
            lifecycleScope.launch {
                withContext(NonCancellable) {
                    taskDeleter.delete(gtasksList)
                }
                setResult(Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED))
                finish()
            }
        }
    }

    private suspend fun onListRenamed(taskList: TaskList) {
        val result = gtasksList.copy(
            name = taskList.title,
            color = selectedColor,
            icon = selectedIcon.value,
        )
        googleTaskListDao.insertOrReplace(result)

        setResult(
                Activity.RESULT_OK,
                Intent(TaskListFragment.ACTION_RELOAD)
                    .putExtra(MainActivity.OPEN_FILTER, GtasksFilter(result)))
        finish()
    }

    private fun requestFailed(error: Throwable) {
        Timber.e(error)
        hideProgressIndicator()
        lifecycleScope.launch { snackbar.showSnackbar(getString(R.string.gtasks_GLA_errorIOAuth)) }
        //toast(R.string.gtasks_GLA_errorIOAuth)
        return
    }

    companion object {
        const val EXTRA_ACCOUNT = "extra_account"
        const val EXTRA_STORE_DATA = "extra_store_data"
    }
}