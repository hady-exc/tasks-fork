package org.tasks.caldav

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.compose.ListSettingsComposables.PrincipalList
import org.tasks.compose.ShareInvite.ShareInviteDialog
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.PrincipalDao
import org.tasks.data.PrincipalWithAccess
import javax.inject.Inject

@AndroidEntryPoint
class CaldavCalendarSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    @Inject lateinit var principalDao: PrincipalDao

    private val viewModel: CaldavCalendarViewModel by viewModels()

    private var principalsList: MutableState<List<PrincipalWithAccess>> = mutableStateOf( emptyList<PrincipalWithAccess>().toMutableList())
    private val removeDialog = mutableStateOf<PrincipalWithAccess?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.inFlight.observe(this) { showProgress.value = it }

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

        setContent {
            MdcTheme {
                Box(contentAlignment = Alignment.TopStart) {// Box to layout FAB over main content
                    baseCaldavSettingsContent {
                        caldavCalendar?.takeIf { it.id > 0 }?.let {
                            principalDao.getPrincipals(it.id).observeAsState().value?.let { list->
                                principalsList.value = list
                            }
                        }
                        if (principalsList.value.isNotEmpty())
                            PrincipalList(
                                principalsList.value,
                                onRemove = if (canRemovePrincipals) ::onRemove else null
                            )
                    }

                    removeDialog.value?.let { principal ->
                        AlertDialog(
                            onDismissRequest = { removeDialog.value = null },
                            confirmButton = {
                                Constants.TextButton(text = R.string.ok) {
                                    removePrincipal(principal)
                                    removeDialog.value = null
                                }
                            },
                            dismissButton = {
                                Constants.TextButton(text = R.string.cancel) {
                                    removeDialog.value = null
                                }
                            },
                            title = {
                                Text(
                                    stringResource(id = R.string.remove_user),
                                    style = MaterialTheme.typography.h6
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(
                                        R.string.remove_user_confirmation,
                                        principal.name,
                                        caldavCalendar?.name ?: ""
                                    ),
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
                    if (caldavAccount.canShare && (isNew || caldavCalendar?.access == ACCESS_OWNER)) {
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
        get() = caldavCalendar?.access == ACCESS_OWNER && caldavAccount.canRemovePrincipal

    private fun onRemove(principal: PrincipalWithAccess) {
        if (requestInProgress()) return
        removeDialog.value = principal
    }

    private fun removePrincipal(principal: PrincipalWithAccess) = lifecycleScope.launch {
        try {
            viewModel.removeUser(caldavAccount, caldavCalendar!!, principal)
        } catch (e: Exception) {
            requestFailed(e)
        }
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) {
        caldavCalendar = viewModel.createCalendar(caldavAccount, name, color, selectedIcon)
    }

    override suspend fun updateNameAndColor(
            account: CaldavAccount,
            calendar: CaldavCalendar,
            name: String,
            color: Int
    ) {
        viewModel.updateCalendar(account, calendar, name, color, selectedIcon)
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
                else -> false
            }

        val CaldavAccount.canShare: Boolean
            get() = when (serverType) {
                SERVER_TASKS, SERVER_OWNCLOUD, SERVER_SABREDAV, SERVER_NEXTCLOUD -> true
                else -> false
            }
    }
}