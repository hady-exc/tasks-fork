package org.tasks.caldav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import at.bitfire.dav4jvm.exception.HttpException
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.service.TaskDeleter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.activities.BaseListSettingsActivity
import org.tasks.compose.DeleteButton
import org.tasks.compose.ListSettings.ListSettingsSnackBar
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.databinding.ActivityCaldavCalendarSettingsBinding
import org.tasks.extensions.Context.hideKeyboard
import org.tasks.filters.CaldavFilter
import org.tasks.themes.TasksIcons
import org.tasks.ui.DisplayableException
import java.net.ConnectException
import javax.inject.Inject

abstract class BaseCaldavCalendarSettingsActivity : BaseListSettingsActivity() {
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskDeleter: TaskDeleter

    private lateinit var root: LinearLayout
    private lateinit var name: TextInputEditText
    protected lateinit var nameLayout: TextInputLayout
    protected lateinit var progressView: ProgressBar

    protected var caldavCalendar: CaldavCalendar? = null

    protected lateinit var caldavAccount: CaldavAccount
    override val defaultIcon = TasksIcons.LIST

    protected open val setContent
        get() = true
    protected val snackbar = SnackbarHostState() // to be used by descendants

    override fun bind() =
        if ( compose ) { TODO() }
        else {
            ActivityCaldavCalendarSettingsBinding.inflate(layoutInflater).let {
                root = it.rootLayout
                name = it.name.apply {
                    addTextChangedListener(
                        onTextChanged = { _, _, _, _ -> nameLayout.error = null }
                    )
                }
                nameLayout = it.nameLayout
                progressView = it.progressBar.progressBar
                it.root
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = intent
        caldavCalendar = intent.getParcelableExtra(EXTRA_CALDAV_CALENDAR)
        super.onCreate(savedInstanceState)
        caldavAccount = if (caldavCalendar == null) {
            intent.getParcelableExtra(EXTRA_CALDAV_ACCOUNT)!!
        } else {
            runBlocking { caldavDao.getAccountByUuid(caldavCalendar!!.account!!)!! }
        }
        if (savedInstanceState == null) {
            if (caldavCalendar != null) {
                if (compose) textState.value = caldavCalendar!!.name ?: ""
                else name.setText(caldavCalendar!!.name)
                selectedColor = caldavCalendar!!.color
                selectedIcon.update { caldavCalendar?.icon }
            }
        }
        if (!compose && caldavCalendar == null) {
            name.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT)
        }

        if (setContent) TODO()
/*
            setContent {
                MdcTheme {
                    ListSettings(
                        title = toolbarTitle,
                        requestKeyboard = isNew,
                        text = textState,
                        error = errorState,
                        color = colorState,
                        icon = iconState,
                        save = { lifecycleScope.launch { save() } },
                        selectColor = { showThemePicker() },
                        clearColor = { clearColor() },
                        selectIcon = { showIconPicker() },
                        optionButton = { if (!isNew) DeleteButton { lifecycleScope.launch { promptDelete() } } },
                        showProgress = showProgress
                    )

                    ListSettingsSnackBar(state = snackbar)
                }
            }
*/

        updateTheme()
    }

    override val isNew: Boolean
        get() = caldavCalendar == null

    override val toolbarTitle: String
        get() = if (isNew) getString(R.string.new_list) else caldavCalendar!!.name ?: ""

    override suspend fun save() {
        if (requestInProgress()) {
            return
        }
        val name = newName
        if (isNullOrEmpty(name)) {
            if (compose)
                errorState.value = getString(R.string.name_cannot_be_empty)
            else
                nameLayout.error = getString(R.string.name_cannot_be_empty)
            return
        }
        when {
            caldavCalendar == null -> {
                showProgressIndicator()
                createCalendar(caldavAccount, name, selectedColor)
            }
            nameChanged() || colorChanged() -> {
                showProgressIndicator()
                updateNameAndColor(caldavAccount, caldavCalendar!!, name, selectedColor)
            }
            iconChanged() -> updateCalendar()
            else -> finish()
        }
    }

    protected abstract suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int)

    protected abstract suspend fun updateNameAndColor(
        account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int)

    protected abstract suspend fun deleteCalendar(
        caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar
    )

    private fun showProgressIndicator() {
        if (compose)
            showProgress.value = true
        else
            progressView.visibility = View.VISIBLE
    }

    private fun hideProgressIndicator() {
        if (compose)
            showProgress.value = false
        else
            progressView.visibility = View.GONE
    }

    protected fun requestInProgress(): Boolean {
        return if (compose) showProgress.value else progressView.visibility == View.VISIBLE
    }

    protected fun requestFailed(t: Throwable) {
        hideProgressIndicator()
        when (t) {
            is HttpException -> showSnackbar(t.message)
            is retrofit2.HttpException -> showSnackbar(t.message() ?: "HTTP ${t.code()}")
            is DisplayableException -> showSnackbar(t.resId)
            is ConnectException -> showSnackbar(R.string.network_error)
            else -> showSnackbar(R.string.error_adding_account, t.message!!)
        }
        return
    }

    private fun showSnackbar(resId: Int, vararg formatArgs: Any) {
        if (compose)
            lifecycleScope.launch { snackbar.showSnackbar( getString(resId, *formatArgs) ) }
        else showSnackbar(getString(resId, *formatArgs))
    }

    private fun showSnackbar(message: String?) {
        val snackbar = Snackbar.make(root, message!!, 8000)
                .setTextColor(getColor(R.color.snackbar_text_color))
                .setActionTextColor(getColor(R.color.snackbar_action_color))
        snackbar
                .view
                .setBackgroundColor(getColor(R.color.snackbar_background))
        snackbar.show()
    }

    protected suspend fun createSuccessful(url: String?) {
        val caldavCalendar = CaldavCalendar(
            uuid = UUIDHelper.newUUID(),
            account = caldavAccount.uuid,
            url = url,
            name = newName,
            color = selectedColor,
            icon = selectedIcon.value,
        )
        caldavDao.insert(caldavCalendar)
        setResult(
                Activity.RESULT_OK,
                Intent().putExtra(MainActivity.OPEN_FILTER, CaldavFilter(caldavCalendar)))
        finish()
    }

    protected suspend fun updateCalendar() {
        val result = caldavCalendar!!.copy(
            name = newName,
            color = selectedColor,
            icon = selectedIcon.value,
        )
        caldavDao.update(result)
        setResult(
                Activity.RESULT_OK,
                Intent(TaskListFragment.ACTION_RELOAD)
                        .putExtra(MainActivity.OPEN_FILTER, CaldavFilter(result)))
        finish()
    }

    override fun hasChanges(): Boolean =
            if (caldavCalendar == null)
                !isNullOrEmpty(newName) || selectedColor != 0 || selectedIcon.value?.isBlank() == false
            else
                nameChanged() || iconChanged() || colorChanged()

    private fun nameChanged(): Boolean = caldavCalendar!!.name != newName

    private fun colorChanged(): Boolean = selectedColor != caldavCalendar!!.color

    private fun iconChanged(): Boolean = selectedIcon.value != caldavCalendar!!.icon

    private val newName: String
        get() =
            if (compose) textState.value.trim { it <= ' '}
            else name.text.toString().trim { it <= ' ' }

    override fun finish() {
        if (!compose) hideKeyboard(name)
        super.finish()
    }

    override fun discard() {
        if (!requestInProgress()) {
            super.discard()
        }
    }

    override fun promptDelete() {
        if (!requestInProgress()) {
            super.promptDelete()
        }
    }

    override suspend fun delete() {
        showProgressIndicator()
        deleteCalendar(caldavAccount, caldavCalendar!!)
    }

    protected suspend fun onDeleted(deleted: Boolean) {
        if (deleted) {
            taskDeleter.delete(caldavCalendar!!)
            setResult(Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED))
            finish()
        }
    }

    @Composable
    fun baseCaldavSettingsContent (
        optionButton: @Composable () -> Unit = { if (!isNew) DeleteButton { promptDelete() } },
        extensionContent: @Composable ColumnScope.() -> Unit = {}
    ) {
        baseSettingsContent (
            optionButton = optionButton,
            extensionContent = extensionContent
        )
        ListSettingsSnackBar(state = snackbar)
    }

    companion object {
        const val EXTRA_CALDAV_CALENDAR = "extra_caldav_calendar"
        const val EXTRA_CALDAV_ACCOUNT = "extra_caldav_account"
    }
}