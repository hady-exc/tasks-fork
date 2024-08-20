package org.tasks.activities

import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import com.google.android.material.composethemeadapter.MdcTheme
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.compose.ListSettings.ListSettingsProgressBar
import org.tasks.compose.ListSettings.ListSettingsSurface
import org.tasks.compose.ListSettings.ListSettingsTitleInput
import org.tasks.compose.ListSettings.ListSettingsToolbar
import org.tasks.compose.ListSettings.PromptAction
import org.tasks.compose.ListSettings.SelectColorRow
import org.tasks.compose.ListSettings.SelectIconRow
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.extensions.addBackPressedCallback
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.themes.ColorProvider
import org.tasks.themes.DrawableUtil
import org.tasks.themes.TasksTheme
import org.tasks.themes.ThemeColor
import javax.inject.Inject

abstract class BaseListSettingsActivity : ThemedInjectingAppCompatActivity(), Toolbar.OnMenuItemClickListener, ColorPalettePicker.ColorPickedCallback, ColorWheelPicker.ColorPickedCallback {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var colorProvider: ColorProvider
    protected abstract val defaultIcon: String
    protected var selectedColor = 0
    protected var selectedIcon = MutableStateFlow<String?>(null)

    private lateinit var clear: View
    private lateinit var color: TextView
    protected lateinit var toolbar: Toolbar
    protected lateinit var colorRow: ViewGroup

    /* descendants which are @Compose'ed shall override it and return true */
    protected open val compose: Boolean
        get() = false
    protected val textState = mutableStateOf("")
    protected val errorState = mutableStateOf("")
    protected val colorState = mutableStateOf(Color.Unspecified)
    protected val iconState = mutableIntStateOf(R.drawable.ic_outline_not_interested_24px)
    protected val showProgress = mutableStateOf(false)
    protected val promptDelete = mutableStateOf(false)
    protected val promptDiscard = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* defaultIcon is initialized in the descendant's constructor so it can not be used
           in constructor of the base class. So valid initial value for iconState is set here  */
        iconState.intValue = getIconResId(defaultIcon)!!

        if (!compose) {
            val view = bind()
            setContentView(view)
            clear = findViewById<View>(R.id.clear).apply {
                setOnClickListener { clearColor() }
            }
            color = findViewById(R.id.color)
            colorRow = findViewById<ViewGroup>(R.id.color_row).apply {
                setOnClickListener { showThemePicker() }
            }
        }
        findViewById<View>(R.id.icon_row).setOnClickListener { showIconPicker() }
        toolbar = view.findViewById(R.id.toolbar)
        if (savedInstanceState != null) {
            selectedColor = savedInstanceState.getInt(EXTRA_SELECTED_THEME)
            selectedIcon.update { savedInstanceState.getString(EXTRA_SELECTED_ICON) }
        }
        if (!compose) {
            toolbar.title = toolbarTitle
            toolbar.navigationIcon = getDrawable(R.drawable.ic_outline_save_24px)
            toolbar.setNavigationOnClickListener { lifecycleScope.launch { save() } }
            if (!isNew) {
                toolbar.inflateMenu(R.menu.menu_tag_settings)
            }
            toolbar.setOnMenuItemClickListener(this)
        }

        addBackPressedCallback {
            discard()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_SELECTED_THEME, selectedColor)
        outState.putString(EXTRA_SELECTED_ICON, selectedIcon.value)
    }

    protected abstract fun hasChanges(): Boolean
    protected abstract suspend fun save()
    protected abstract val isNew: Boolean
    protected abstract val toolbarTitle: String?
    protected abstract suspend fun delete()
    protected abstract fun bind(): View
    protected open fun discard() {
        if (hasChanges())  promptDiscard.value = true
        else finish()
    }

    protected fun clearColor() {
        onColorPicked(0)
    }

    protected fun showThemePicker() {
        newColorPalette(null, 0, selectedColor, Palette.COLORS)
                .show(supportFragmentManager, FRAG_TAG_COLOR_PICKER)
    }

    val launcher = registerForIconPickerResult { selected ->
        selectedIcon.update { selected }
    }

    private fun showIconPicker() {
        launcher.launchIconPicker(this, selectedIcon.value)
    }

    override fun onColorPicked(color: Int) {
        selectedColor = color
        updateTheme()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.delete) {
            promptDelete()
            return true
        }
        return onOptionsItemSelected(item)
    }

    protected open fun promptDelete() { promptDelete.value = true }

    protected fun updateTheme() {
        val themeColor: ThemeColor
        if (compose) {
            themeColor = if (selectedColor == 0) this.themeColor
                else colorProvider.getThemeColor(selectedColor, true)
            colorState.value =
                if (selectedColor == 0) Color.Unspecified
                else Color((colorProvider.getThemeColor(selectedColor, true)).primaryColor)
            iconState.intValue = (getIconResId(selectedIcon) ?: getIconResId(defaultIcon))!!
            themeColor.applyToNavigationBar(this)
        } else {
            if (selectedColor == 0) {
                themeColor = this.themeColor
                DrawableUtil.setLeftDrawable(this, color, R.drawable.ic_outline_not_interested_24px)
                DrawableUtil.getLeftDrawable(color).setTint(getColor(R.color.icon_tint_with_alpha))
                clear.visibility = View.GONE
            } else {
                themeColor = colorProvider.getThemeColor(selectedColor, true)
                DrawableUtil.setLeftDrawable(this, color, R.drawable.color_picker)
                val leftDrawable = DrawableUtil.getLeftDrawable(color)
                (if (leftDrawable is LayerDrawable) leftDrawable.getDrawable(0) else leftDrawable)
                    .setTint(themeColor.primaryColor)
                clear.visibility = View.VISIBLE
            }
            themeColor.applyToNavigationBar(this)
            val icon = getIconResId(selectedIcon) ?: getIconResId(defaultIcon)
            DrawableUtil.setLeftDrawable(this, this.icon, icon!!)
            DrawableUtil.getLeftDrawable(this.icon).setTint(getColor(R.color.icon_tint_with_alpha))
        }

    }

    @Composable
    protected fun DefaultContent(
        title: String,
        requestKeyboard: Boolean,
        optionButton: @Composable () -> Unit,
        extensionContent: @Composable ColumnScope.() -> Unit = {}
    ) {
        MdcTheme {
            ListSettingsSurface {
                ListSettingsToolbar(
                    title = title,
                    save = { lifecycleScope.launch { save() } },
                    optionButton = optionButton
                )
                ListSettingsProgressBar(showProgress)
                ListSettingsTitleInput(
                    text = textState, error = errorState, requestKeyboard = requestKeyboard,
                    modifier = Modifier.padding(horizontal = Constants.KEYLINE_FIRST)
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    SelectColorRow(
                        color = colorState,
                        selectColor = { showThemePicker() },
                        clearColor = { clearColor() })
                    SelectIconRow(
                        icon = iconState,
                        selectIcon = { showIconPicker() })
                    extensionContent()

                    PromptAction(
                        showDialog = promptDelete,
                        title = stringResource(id = R.string.delete_tag_confirmation, title),
                        onAction = { lifecycleScope.launch { delete() } }
                    )
                    PromptAction(
                        showDialog = promptDiscard,
                        title = stringResource(id = R.string.discard_changes),
                        onAction = { lifecycleScope.launch { finish() } }
                    )
                }
            }
        }
            clear.visibility = View.VISIBLE
        }
        themeColor.applyToNavigationBar(this)
    }

    companion object {
        private const val EXTRA_SELECTED_THEME = "extra_selected_theme"
        private const val EXTRA_SELECTED_ICON = "extra_selected_icon"
        private const val FRAG_TAG_ICON_PICKER = "frag_tag_icon_picker"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
    }
}
