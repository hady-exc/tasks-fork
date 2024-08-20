package org.tasks.caldav

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.compose.DeleteButton
import org.tasks.compose.ListSettings.ListSettings
import org.tasks.compose.ListSettings.ListSettingsSnackBar
import org.tasks.compose.ListSettingsComposables.PrincipalList
import org.tasks.compose.ShareInvite.ShareInviteDialog
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.SERVER_MAILBOX_ORG
import org.tasks.data.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.CaldavCalendar
import org.tasks.data.PrincipalDao
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.themes.TasksTheme
import javax.inject.Inject

@AndroidEntryPoint
class CaldavCalendarSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    @Inject lateinit var principalDao: PrincipalDao

    private val viewModel: CaldavCalendarViewModel by viewModels()

    override val setContent
        get() = false
    override val compose: Boolean
        get() = true

    private var principalsList: MutableState<List<PrincipalWithAccess>> = mutableStateOf( emptyList<PrincipalWithAccess>().toMutableList())
    private val removeDialog = mutableStateOf<PrincipalWithAccess?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.inFlight.observe(this) {
            if (compose) {
                showProgress.value = it
            } else
                progressView.isVisible = it
        }
        viewModel.error.observe(this) { throwable ->
            throwable?.let {
                requestFailed(it)
                viewModel.error.value = null
            }
        }
        viewModel.finish.observe(this) {
            setResult(RESULT_OK, it)
            finish()
        }

        if (!compose) {
            caldavCalendar?.takeIf { it.id > 0 }?.let {

                    findViewById<ComposeView>(R.id.people).setContent {
                TasksTheme {
                        val principals = principalDao.getPrincipals(it.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                        PrincipalList(
                            principals = principals,
                                onRemove = if (canRemovePrincipals) { { onRemove(it) } } else null,
                            )
                        }
                }
            }
        }
        if (!compose) {
            if (caldavAccount.canShare /*&& (isNew || caldavCalendar?.access == ACCESS_OWNER)*/) {  // TODO(rollback commented condition!!!)
                findViewById<ComposeView>(R.id.fab)
                    .apply { isVisible = true }
                    .setContent {
                        TasksTheme {
                            val openDialog = rememberSaveable { mutableStateOf(false) }
                            ShareInviteDialog(
                                openDialog,
                                email = caldavAccount.serverType != SERVER_OWNCLOUD
                            ) { input ->
                                lifecycleScope.launch {
                                    share(input)
                                    openDialog.value = false
                                }
                            }
                            FloatingActionButton(onClick = { openDialog.value = true },
                                containerColor = MaterialTheme.colorScheme.primary
                        ) {Icon(
                                    painter = painterResource(R.drawable.ic_outline_person_add_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
            }
        }

        if (compose) setContent {
            MdcTheme {
                Box (contentAlignment = Alignment.TopStart) {
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
                    ) {

                        caldavCalendar?.takeIf { it.id > 0 }?.let {
                            principalDao.getPrincipals(it.id).observeAsState().value?.let {
                                principalsList.value = it
                            }
                        }
                        if (principalsList.value.isNotEmpty())
                            PrincipalList(
                                principalsList.value,
                                onRemove = if (canRemovePrincipals) ::onRemove else null
                            )

                    }

                    ListSettingsSnackBar(state = snackbar)

                    removeDialog.value?.let { principal ->
                        AlertDialog(
                            onDismissRequest = { removeDialog.value = null },
                            confirmButton = {
                                Constants.TextButton(text = R.string.ok) {
                                    removePrincipal(principal)
                                    removeDialog.value = null
                                }
                            },
                            dismissButton = { Constants.TextButton(text = R.string.cancel) { removeDialog.value = null } },
                            title = { Text(stringResource(id = R.string.remove_user), style = MaterialTheme.typography.h6) },
                            text = {
                                Text(
                                    text = stringResource(R.string.remove_user_confirmation, principal.name, caldavCalendar?.name?:""),
                                    style = MaterialTheme.typography.body1
                                )
                            }
                        )
                    }

                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (caldavAccount.canShare /*&& (isNew || caldavCalendar?.access == ACCESS_OWNER)*/) {  // TODO(rollback commented condition!!!)
                        val openDialog = rememberSaveable { mutableStateOf(false) }
                        ShareInviteDialog(
                            openDialog,
                            email = caldavAccount.serverType != SERVER_OWNCLOUD
                        ) { input ->
                            lifecycleScope.launch {
                                share(input)
                                openDialog.value = false
                            }
                        }
                        FloatingActionButton(
                            onClick = { openDialog.value = true },
                            modifier = Modifier.padding(Constants.KEYLINE_FIRST)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_person_add_24),
                                contentDescription = null,
                                tint = MaterialTheme.colors.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }

    private val canRemovePrincipals: Boolean
        get() = true // TODO( revert back !) caldavCalendar?.access == ACCESS_OWNER && caldavAccount.canRemovePrincipal

    private fun onRemove(principal: PrincipalWithAccess) {
        if (requestInProgress()) {
            return
        }
        if (compose) removeDialog.value = principal
        else {
            dialogBuilder
                .newDialog(R.string.remove_user)
                .setMessage(R.string.remove_user_confirmation, principal.name, caldavCalendar?.name)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ -> removePrincipal(principal) }
                .show()
        }
    }

    private fun removePrincipal(principal: PrincipalWithAccess) = lifecycleScope.launch {
        try {
            viewModel.removeUser(caldavAccount, caldavCalendar!!, principal)
        } catch (e: Exception) {
            requestFailed(e)
        }
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) {
        caldavCalendar = viewModel.createCalendar(caldavAccount, name, color, selectedIcon.value)
    }

    override suspend fun updateNameAndColor(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        name: String,
        color: Int
    ) {
        viewModel.updateCalendar(account, calendar, name, color, selectedIcon.value)
    }

    override suspend fun deleteCalendar(
        caldavAccount: CaldavAccount,
        caldavCalendar: CaldavCalendar
    ) {
        viewModel.deleteCalendar(caldavAccount, caldavCalendar)
    }

    private suspend fun share(email: String) {
        if (isNew) {
            viewModel.ignoreFinish = true
            try {
                save()
            } finally {
                viewModel.ignoreFinish = false
            }
        }
        caldavCalendar?.let { viewModel.addUser(caldavAccount, it, email) }
    }

    companion object {
        val CaldavAccount.canRemovePrincipal: Boolean
            get() = when (serverType) {
                SERVER_TASKS, SERVER_OWNCLOUD, SERVER_SABREDAV, SERVER_NEXTCLOUD -> true
                else -> true // false TODO(rollback to false)
            }

        val CaldavAccount.canShare: Boolean
            get() = when (serverType) {
                SERVER_TASKS, SERVER_OWNCLOUD, SERVER_SABREDAV, SERVER_NEXTCLOUD, SERVER_MAILBOX_ORG -> true // TODO(revert MAILBOX_ORG out!!!)
                else -> true // false TODO(the same with the above!!!)
            }
    }
}