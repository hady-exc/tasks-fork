package org.tasks.opentasks

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.compose.drawer.DrawerProgressBar
import org.tasks.compose.drawer.DrawerSnackBar
import org.tasks.compose.drawer.DrawerSurface
import org.tasks.compose.drawer.DrawerToolbar
import org.tasks.compose.drawer.TextInput
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar

@AndroidEntryPoint
class OpenTasksListSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    override val compose: Boolean
        get() = true
    override val setContent: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!compose) {
            toolbar.menu.findItem(R.id.delete).isVisible = false
            nameLayout.visibility = View.GONE
            colorRow.visibility = View.GONE
        }

        if (compose)
            setContent {
                DrawerSurface {
                    DrawerToolbar(
                        isNew = isNew,
                        title = toolbarTitle,
                        delete = { lifecycleScope.launch { promptDelete() } },
                        save = { lifecycleScope.launch { save() } },
                        suppressDeleteButton = false
                    )
                    DrawerProgressBar(showProgress)
                    TextInput(text = textState, error = errorState, requestKeyboard = isNew)
                }
                DrawerSnackBar(state = snackbar)
            } /* setContent */
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) {}

    override suspend fun updateNameAndColor(
            account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendar()

    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) {}
}