package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.RelativeLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.sql.Field
import com.todoroo.andlib.sql.Query
import com.todoroo.andlib.sql.UnaryCriterion
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.api.BooleanCriterion
import com.todoroo.astrid.api.CustomFilter
import com.todoroo.astrid.api.CustomFilterCriterion
import com.todoroo.astrid.api.Filter.Companion.NO_ORDER
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.api.TextInputCriterion
import com.todoroo.astrid.core.CriterionInstance
import com.todoroo.astrid.dao.Database
import com.todoroo.astrid.data.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings
import org.tasks.compose.FilterCondition
import org.tasks.compose.InputTextOption
import org.tasks.compose.NewCriterionFAB
import org.tasks.compose.SelectCriterionType
import org.tasks.compose.SelectFromList
import org.tasks.compose.drawer.ListSettingsDrawer
import org.tasks.data.Filter
import org.tasks.data.FilterDao
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.db.QueryUtils
import org.tasks.extensions.Context.openUri
import org.tasks.filters.FilterCriteriaProvider
import org.tasks.themes.CustomIcons
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class FilterSettingsActivity : BaseListSettingsActivity() {
    @Inject lateinit var filterDao: FilterDao
    @Inject lateinit var locale: Locale
    @Inject lateinit var database: Database
    @Inject lateinit var filterCriteriaProvider: FilterCriteriaProvider


    private var filter: CustomFilter? = null
    override val defaultIcon: Int = CustomIcons.FILTER

    override val compose: Boolean
        get() = true

    private var criteria: SnapshotStateList<CriterionInstance> = emptyList<CriterionInstance>().toMutableStateList()
    private val fabExtended = mutableStateOf(false)
    private val editCriterionType: MutableState<String?> = mutableStateOf(null)
    private val newCriterionTypes: MutableState<List<CustomFilterCriterion>?> = mutableStateOf(null)
    private val newCriterionOptions: MutableState<CriterionInstance?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        filter = intent.getParcelableExtra(TOKEN_FILTER)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && filter != null) {
            selectedColor = filter!!.tint
            selectedIcon = filter!!.icon
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
                textState.value = intent.getStringExtra(EXTRA_TITLE) ?: ""
                setCriteria(
                    filterCriteriaProvider.fromString(intent.getStringExtra(EXTRA_CRITERIA)!!)
                )
            }
            else -> setCriteria(universe())
        }
        if (isNew) {
            // toolbar.inflateMenu(R.menu.menu_help) TODO(introduce @composable for help in BaseListSettingActivity)
        }

        // setContent { activityContent() }

        updateTheme()

    } /* end onCreate */

    private fun universe() = listOf(CriterionInstance().apply {
        criterion = filterCriteriaProvider.startingUniverse
        type = CriterionInstance.TYPE_UNIVERSE
    })

    private fun setCriteria(criteriaList: List<CriterionInstance>) {
        criteria = criteriaList
                .ifEmpty { universe() }
                .toMutableStateList()
        fabExtended.value = isNew || criteria.size <= 1
        updateList()

        this.setContent { activityContent() }
    }

    private fun onDelete(index: Int) {
        criteria.removeAt(index)
        updateList()
    }

    private fun onMove(from: Int, to: Int) {
        val criterion = criteria.removeAt(from)
        criteria.add(to, criterion)
    }

    private fun newCriterion() {
        fabExtended.value = false // a.k.a. fab.shrink()
        lifecycleScope.launch {
            newCriterionTypes.value = filterCriteriaProvider.all()
        }
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
           errorState.value = getString(R.string.name_cannot_be_empty)
        }

        if (hasChanges()) {
            var f = Filter(
                id = filter?.id ?: 0L,
                title = newName,
                color = selectedColor,
                icon = selectedIcon,
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
                    || selectedColor != 0 || selectedIcon != -1 || criteria.size > 1)
        } else newName != filter!!.title
                || selectedColor != filter!!.tint
                || selectedIcon != filter!!.icon
                || CriterionInstance.serialize(criteria) != filter!!.criterion!!.trim()
                || criteria.values != filter!!.valuesForNewTasks
                || criteria.sql != filter!!.sql
    }

    override fun finish() {
        super.finish()
    }

    override fun bind(): RelativeLayout { TODO("must be deleted") }

    override suspend fun delete() {
        filterDao.delete(filter!!.id)
        setResult(
                Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED).putExtra(TOKEN_FILTER, filter))
        finish()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
        /* TODO(delete after help button introduced) */
        if (item.itemId == R.id.menu_help) {
            help()
            true
        } else {
            super.onMenuItemClick(item)
        }

    private fun help() = openUri(R.string.url_filters)

    private fun updateList() {
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
                var subSql: String? = instance.criterion.sql.replace(
                    "?",
                    UnaryCriterion.sanitize(instance.valueFromCriterion!!)
                )
                subSql = PermaSql.replacePlaceholdersForQuery(subSql)
                sql.append(Task.ID).append(" IN (").append(subSql).append(")")
            }
            val sqlString = QueryUtils.showHiddenAndCompleted(sql.toString())
            database.query(sqlString, null).use { cursor ->
                cursor.moveToNext()
                instance.start = if (last == -1) cursor.getInt(0) else last
                instance.end = cursor.getInt(0)
                last = instance.end
                max = max(max, last)
            }
        }
        for (instance in criteria) {
            instance.max = max
        }
    }

    @Composable
    private fun activityContent ()
    {
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
                        items = criteria,
                        onDelete = { index -> onDelete(index) },
                        doSwap = { from, to -> onMove(from, to) },
                        onClick = { id -> editCriterionType.value = id }
                    )
                }

                NewCriterionFAB(fabExtended) { newCriterion() }

                /** edit given criterion type (AND|OR|NOT) **/
                editCriterionType.value?.let { itemId ->
                    val index = criteria.indexOfFirst { it.id == itemId }
                    assert(index >= 0)
                    val criterionInstance = criteria[index]
                    if (criterionInstance.type != CriterionInstance.TYPE_UNIVERSE) {
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
                            onCancel = { editCriterionType.value = null }
                        ) { selected ->
                            val type = when (selected) {
                                0 -> CriterionInstance.TYPE_INTERSECT
                                1 -> CriterionInstance.TYPE_ADD
                                else -> CriterionInstance.TYPE_SUBTRACT
                            }
                            if (criterionInstance.type != type) {
                                criterionInstance.type = type
                                criteria.removeAt(index)  // remove - add pair triggers the item recomposition
                                criteria.add(index, criterionInstance)
                                updateList()
                            }
                            editCriterionType.value = null
                        }
                    }
                } /* end (AND|OR|NOT) dialog */

                /** dialog to select new criterion category **/
                newCriterionTypes.value?.let  { list ->
                    SelectFromList(
                        names = list.map(CustomFilterCriterion::getName),
                        onCancel = { newCriterionTypes.value = null },
                        onSelected = { which ->
                            val instance = CriterionInstance()
                            instance.criterion = list[which]
                            newCriterionTypes.value = null
                            if (instance.criterion is BooleanCriterion) {
                                criteria.add(instance)
                                updateList()
                            } else
                                newCriterionOptions.value = instance
                        }
                    )
                } /* end dialog  */

                /** Show options menu for the given CriterionInstance  */
                newCriterionOptions.value?.let { instance ->

                    when (instance.criterion) {
                        is MultipleSelectCriterion -> {
                            val multiSelectCriterion = instance.criterion as MultipleSelectCriterion
                            val list = multiSelectCriterion.entryTitles.toList()
                            SelectFromList(
                                names = list,
                                title = instance.criterion.name,
                                onCancel = { newCriterionOptions.value = null },
                                onSelected = { which ->
                                    instance.selectedIndex = which
                                    criteria.add(instance)
                                    updateList()
                                    newCriterionOptions.value = null
                                }
                            )
                        }

                        is TextInputCriterion -> {
                            val textInCriterion = instance.criterion as TextInputCriterion
                            val editText = remember { mutableStateOf(instance.selectedText?: "") }
                            InputTextOption (
                                title = textInCriterion.name,
                                text = editText,
                                onCancel = { newCriterionOptions.value = null },
                                onDone = {
                                    instance.selectedText = editText.value
                                    criteria.add(instance)
                                    updateList()
                                    newCriterionOptions.value = null
                                }
                            )
                        }

                        else -> assert(false) { "Unexpected Criterion type" }
                    }
                } /* end given criteria options dialog */
            }
        }
    } /* activityContent */

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
                return AndroidUtilities.mapToSerializedString(values)
            }
    }
}