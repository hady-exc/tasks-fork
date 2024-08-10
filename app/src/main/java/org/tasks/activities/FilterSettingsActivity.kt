package org.tasks.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.tasks.data.sql.Field
import org.tasks.data.sql.Query
import org.tasks.data.sql.UnaryCriterion
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.api.BooleanCriterion
import org.tasks.filters.CustomFilter
import com.todoroo.astrid.api.CustomFilterCriterion
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.api.TextInputCriterion
import com.todoroo.astrid.core.CriterionInstance
import com.todoroo.astrid.core.CustomFilterAdapter
import org.tasks.data.db.Database
import org.tasks.data.entity.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings
import org.tasks.compose.AddCriteriaButton
import org.tasks.compose.FilterCondition
import org.tasks.compose.SelectCriterionType
import org.tasks.compose.drawer.ListSettingsDrawer
import org.tasks.data.entity.Filter
import org.tasks.data.dao.FilterDao
import org.tasks.data.NO_ORDER
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.rawQuery
import org.tasks.databinding.FilterSettingsActivityBinding
import org.tasks.db.QueryUtils
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.hideKeyboard
import org.tasks.filters.FilterCriteriaProvider
import org.tasks.filters.mapToSerializedString
import org.tasks.themes.TasksIcons
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class FilterSettingsActivity : BaseListSettingsActivity() {
    @Inject lateinit var filterDao: FilterDao
    @Inject lateinit var locale: Locale
    @Inject lateinit var database: Database
    @Inject lateinit var filterCriteriaProvider: FilterCriteriaProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var name: TextInputEditText  /* TODO(eliminate) */
    private lateinit var nameLayout: TextInputLayout /* TODO(eliminate) */
    private lateinit var recyclerView: RecyclerView /* TODO(eliminate) */
    private lateinit var composeView: ComposeView /* TODO(eliminate) */
    private lateinit var fab: ExtendedFloatingActionButton /* TODO(probably eliminate) */
    
    private var filter: CustomFilter? = null
    private lateinit var adapter: CustomFilterAdapter /* TODO(eliminate) */
    private var criteria: MutableList<CriterionInstance> = ArrayList()
    override val defaultIcon: Int = CustomIcons.FILTER

    override val compose: Boolean
        get() = true

    private lateinit var _criteria: SnapshotStateList<CriterionInstance>
    private val fabExtended = mutableStateOf(false)
    private val editTypeId: MutableState<String?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        filter = intent.getParcelableExtra(TOKEN_FILTER)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && filter != null) {
            selectedColor = filter!!.tint
            selectedIcon.update { filter!!.icon }
            //name.setText(filter!!.title)
            textState.value = filter!!.title ?: ""
        }
        when {
            savedInstanceState != null -> lifecycleScope.launch {
                setCriteria(
                    filterCriteriaProvider.fromString(
                        savedInstanceState.getString(EXTRA_CRITERIA)!!
                    )
                )
            }
            filter != null -> lifecycleScope.launch {
                setCriteria(filterCriteriaProvider.fromString(filter!!.criterion))
            }
            intent.hasExtra(EXTRA_CRITERIA) -> lifecycleScope.launch {
                //name.setText(intent.getStringExtra(EXTRA_TITLE))
                textState.value = intent.getStringExtra(EXTRA_TITLE) ?: ""
                setCriteria(
                    filterCriteriaProvider.fromString(intent.getStringExtra(EXTRA_CRITERIA)!!)
                )
            }
            else -> setCriteria(universe())
        }
/*
        composeView.setContent {
            FilterCondition(
                _criteria,
                { index -> onDelete(index) },
                { from, to -> onMove(from, to) }
            )
        }
*/

        //recyclerView.layoutManager = LinearLayoutManager(this)
/*
        ItemTouchHelper(
                CustomFilterItemTouchHelper(this, this::onMove, this::onDelete, this::updateList))
                .attachToRecyclerView(recyclerView)
*/
        if (isNew) {
            // toolbar.inflateMenu(R.menu.menu_help) TODO(introduce @composable in BaseListSettingActivity)
        }

        setContent {
            MdcTheme {
                Box(  // to layout FAB over the main content
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    ListSettingsDrawer(
                        title = toolbarTitle,
                        isNew = isNew,
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
                    ) {
                        FilterCondition(
                            _criteria,
                            { index -> onDelete(index) },
                            { from, to -> onMove(from, to) },
                            { id -> editTypeId.value = id }
                        )
                    }
                    AddCriteriaButton(fabExtended, { addCriteria() })

                    if (editTypeId.value != null) {
                        val index = _criteria.indexOfFirst { it.id == editTypeId.value }
                        assert(index >= 0)
                        val criterionInstance = criteria[index]

                        SelectCriterionType(
                            title = criterionInstance.titleFromCriterion,
                            selected = when (criterionInstance.type) {
                                CriterionInstance.TYPE_INTERSECT -> 0
                                CriterionInstance.TYPE_ADD -> 1
                                else -> 2
                            },
                            types = listOf(
                                stringResource(R.string.custom_filter_and),
                                stringResource(R.string.custom_filter_or),
                                stringResource(R.string.custom_filter_not)
                            ),
                            help = { help() },
                            onCancel = { editTypeId.value = null }
                        ) {
                            val type = when (it) {
                                0 -> CriterionInstance.TYPE_INTERSECT
                                1 -> CriterionInstance.TYPE_ADD
                                else -> CriterionInstance.TYPE_SUBTRACT
                            }
                            if (criterionInstance.type != type) {
                                criterionInstance.type = type
                                _criteria.removeAt(index)  // remove - add pair triggers the item recomposition
                                _criteria.add(index,criterionInstance)
                                updateList()
                            }
                            editTypeId.value = null
                        }
                    }
                }
            }
        }

        updateTheme()

    } /* end onCreate */


    private fun universe() = listOf(CriterionInstance().apply {
        criterion = filterCriteriaProvider.startingUniverse
        type = CriterionInstance.TYPE_UNIVERSE
    })

    private fun setCriteria(criteria: List<CriterionInstance>) {
        this._criteria = criteria
                .ifEmpty { universe() }
                .toMutableStateList()
/*
        adapter = CustomFilterAdapter(criteria, locale) { replaceId: String -> onClick(replaceId) }
        recyclerView.adapter = adapter
        fab.isExtended = isNew || adapter.itemCount <= 1
*/
        fabExtended.value = isNew || _criteria.size <= 1
        updateList()
    }

    private fun onDelete(index: Int) {
        _criteria.removeAt(index)
        updateList()
    }

    private fun onMove(from: Int, to: Int) {
        val criterion = _criteria.removeAt(from)
        _criteria.add(to, criterion)
        //adapter.notifyItemMoved(from, to)
    }

    private fun onClick(replaceId: String) {
    // TODO(replace by Composable)
        val index = criteria.indexOfFirst { it.id == replaceId }
        assert(index >= 0)
        //val criterionInstance = criteria.find { it.id == replaceId }!!
        val criterionInstance = criteria[index]
        val view = layoutInflater.inflate(R.layout.dialog_custom_filter_row_edit, window.decorView.rootView as ViewGroup, false)
        val group: MaterialButtonToggleGroup = view.findViewById(R.id.button_toggle)
        val selected = getSelected(criterionInstance)
        group.check(selected)
        dialogBuilder
                .newDialog(criterionInstance.titleFromCriterion)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    criterionInstance.type = getType(group.checkedButtonId)
                    _criteria.removeAt(index)  // remove - add pair triggers the item recomposition
                    _criteria.add(index,criterionInstance)
                    updateList()
                }
                .setNeutralButton(R.string.help) { _, _ -> help() }
                .show()
    }

    private fun getSelected(instance: CriterionInstance): Int =
        when (instance.type) {
            CriterionInstance.TYPE_ADD -> R.id.button_or
            CriterionInstance.TYPE_SUBTRACT -> R.id.button_not
            else -> R.id.button_and
        }

    private fun getType(selected: Int): Int =
        when (selected) {
            R.id.button_or -> CriterionInstance.TYPE_ADD
            R.id.button_not -> CriterionInstance.TYPE_SUBTRACT
            else -> CriterionInstance.TYPE_INTERSECT
        }

    private fun addCriteria() {
        hideKeyboard()
        fabExtended.value = false //  fab.shrink()
        lifecycleScope.launch {
            val all = filterCriteriaProvider.all()
            val names = all.map(CustomFilterCriterion::getName)
            dialogBuilder.newDialog()
                    .setItems(names) { dialog: DialogInterface, which: Int ->
                        val instance = CriterionInstance()
                        instance.criterion = all[which]
                        showOptionsFor(instance) {
                            _criteria.add(instance)
                            updateList()
                        }
                        dialog.dismiss()
                    }
                    .show()
        }
    }

    /** Show options menu for the given CriterionInstance  */
    private fun showOptionsFor(item: CriterionInstance, onComplete: Runnable?) {
        if (item.criterion is BooleanCriterion) {
            onComplete?.run()
            return
        }
        val dialog = dialogBuilder.newDialog(item.criterion.name)
        if (item.criterion is MultipleSelectCriterion) {
            val multiSelectCriterion = item.criterion as MultipleSelectCriterion
            val titles = multiSelectCriterion.entryTitles
            val listener = DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
                item.selectedIndex = which
                onComplete?.run()
            }
            dialog.setItems(titles, listener)
        } else if (item.criterion is TextInputCriterion) {
            val textInCriterion = item.criterion as TextInputCriterion
            val frameLayout = FrameLayout(this)
            frameLayout.setPadding(10, 0, 10, 0)
            val editText = EditText(this)
            editText.setText(item.selectedText)
            editText.hint = textInCriterion.hint
            frameLayout.addView(
                    editText,
                    FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
            dialog
                    .setView(frameLayout)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        item.selectedText = editText.text.toString()
                        onComplete?.run()
                    }
        }
        dialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_CRITERIA, CriterionInstance.serialize(criteria))
    }

    override val isNew: Boolean
        get() = filter == null

    override val toolbarTitle: String
        get() = if (isNew) getString(R.string.FLA_new_filter) else filter?.title ?: ""

    override suspend fun save() {
        val newName = newName
        if (Strings.isNullOrEmpty(newName)) {
            nameLayout.error = getString(R.string.name_cannot_be_empty)
            return
        }
        if (hasChanges()) {
            var f = Filter(
                id = filter?.id ?: 0L,
                title = newName,
                color = selectedColor,
                icon = selectedIcon.value,
                values = criteria.values,
                criterion = CriterionInstance.serialize(criteria),
                sql = criteria.sql,
                order = filter?.order ?: NO_ORDER,
            )
            if (f.criterion.isNullOrBlank()) {
                throw RuntimeException("Criterion cannot be empty")
            }
            if (isNew) {
                f = f.copy(
                    id = filterDao.insert(f)
                )
            } else {
                filterDao.update(f)
            }
            localBroadcastManager.broadcastRefresh()
            setResult(
                    Activity.RESULT_OK,
                    Intent(TaskListFragment.ACTION_RELOAD)
                            .putExtra(MainActivity.OPEN_FILTER, CustomFilter(f)))
        }
        finish()
    }

    private val newName: String
        get() = textState.value.trim { it <= ' ' }

    override fun hasChanges(): Boolean {
        return if (isNew) {
            (!Strings.isNullOrEmpty(newName)
                    || selectedColor != 0 || selectedIcon.value?.isBlank() == false || criteria.size > 1)
        } else newName != filter!!.title
                || selectedColor != filter!!.tint
                || selectedIcon.value != filter!!.icon
                || CriterionInstance.serialize(criteria) != filter!!.criterion!!.trim()
                || criteria.values != filter!!.valuesForNewTasks
                || criteria.sql != filter!!.sql
    }

    override fun finish() {
        //hideKeyboard(name)
        super.finish()
    }

    override fun bind() = FilterSettingsActivityBinding.inflate(layoutInflater).let {
        name = it.name.apply {
            addTextChangedListener(
                onTextChanged = { _, _, _, _ -> nameLayout.error = null }
            )
        }
        nameLayout = it.nameLayout
        recyclerView = it.recyclerView
        composeView = it.filterCriteriaList

        fab = it.fab.apply {
            setOnClickListener { addCriteria() }
        }
        it.root
    }

    override suspend fun delete() {
        filterDao.delete(filter!!.id)
        setResult(
                Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED).putExtra(TOKEN_FILTER, filter))
        finish()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
        if (item.itemId == R.id.menu_help) {
            help()
            true
        } else {
            super.onMenuItemClick(item)
        }

    private fun help() = openUri(R.string.url_filters)

    private fun updateList() = lifecycleScope.launch {
        var max = 0
        var last = -1
        val sql = StringBuilder(Query.select(Field.COUNT).from(Task.TABLE).toString())
                .append(" WHERE ")
        for (instance in criteria) {
            when (instance.type) {
                CriterionInstance.TYPE_ADD -> sql.append("OR ")
                CriterionInstance.TYPE_SUBTRACT -> sql.append("AND NOT ")
                CriterionInstance.TYPE_INTERSECT -> sql.append("AND ")
            }

            // special code for all tasks universe
            if (instance.type == CriterionInstance.TYPE_UNIVERSE || instance.criterion.sql == null) {
                sql.append(activeAndVisible()).append(' ')
            } else {
                var subSql: String = instance.criterion.sql.replace(
                    "?",
                    UnaryCriterion.sanitize(instance.valueFromCriterion!!)
                )
                subSql = PermaSql.replacePlaceholdersForQuery(subSql)
                sql.append(Task.ID).append(" IN (").append(subSql).append(")")
            }
            val sqlString = QueryUtils.showHiddenAndCompleted(sql.toString())
            database.rawQuery(sqlString) { cursor ->
                cursor.step()
                instance.start = if (last == -1) cursor.getInt(0) else last
                instance.end = cursor.getInt(0)
                last = instance.end
                max = max(max, last)
            }
        }
        for (instance in criteria) {
            instance.max = max
        }
        //adapter.submitList(criteria)

        //_criteria.value = criteria

    }

    companion object {
        const val TOKEN_FILTER = "token_filter"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CRITERIA = "extra_criteria"

        val List<CriterionInstance>.sql: String
            get() {
                val sql = StringBuilder(" WHERE ")
                for (instance in this) {
                    val value = instance.valueFromCriterion
                    when (instance.type) {
                        CriterionInstance.TYPE_ADD -> sql.append(" OR ")
                        CriterionInstance.TYPE_SUBTRACT -> sql.append(" AND NOT ")
                        CriterionInstance.TYPE_INTERSECT -> sql.append(" AND ")
                    }

                    // special code for all tasks universe
                    if (instance.type == CriterionInstance.TYPE_UNIVERSE || instance.criterion.sql == null) {
                        sql.append(activeAndVisible())
                    } else {
                        val subSql = instance.criterion.sql
                            .replace("?", UnaryCriterion.sanitize(value!!))
                            .trim()
                        sql.append(Task.ID).append(" IN (").append(subSql).append(")")
                    }
                }
                return sql.toString()
            }

        private val List<CriterionInstance>.values: String
            get() {
                val values: MutableMap<String, Any> = HashMap()
                for (instance in this) {
                    val value = instance.valueFromCriterion
                    if (instance.criterion.valuesForNewTasks != null
                        && instance.type == CriterionInstance.TYPE_INTERSECT) {
                        for ((key, value1) in instance.criterion.valuesForNewTasks) {
                            values[key.replace("?", value!!)] = value1.toString().replace("?", value)
                        }
                    }
                }
                return mapToSerializedString(values)
            }
    }
}