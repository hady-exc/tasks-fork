package org.tasks.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.os.ConfigurationCompat
import com.todoroo.astrid.core.CriterionInstance
import org.tasks.R
import org.tasks.compose.SwipeOut.SwipeOut
import org.tasks.extensions.formatNumber
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterCondition (
    items: SnapshotStateList<CriterionInstance>,
    onDelete: (Int) -> Unit,
    doSwap: (Int, Int) -> Unit,
    onClick: (String) -> Unit
) {

    val getIcon: (CriterionInstance) -> Int = { criterion ->
        when (criterion.type) {
            CriterionInstance.TYPE_ADD -> {

                R.drawable.ic_call_split_24px
                //divider.visibility = View.VISIBLE
            }
            CriterionInstance.TYPE_SUBTRACT -> {
                R.drawable.ic_outline_not_interested_24px
                //divider.visibility = View.GONE
            }
            CriterionInstance.TYPE_INTERSECT -> {
                R.drawable.ic_outline_add_24px
                //divider.visibility = View.GONE
            }
            else -> { 0 }  /* assert */
        }

    }

/*
        Box(
            //modifier = Modifier
            //    .fillMaxWidth()
                //.verticalScroll(state = rememberScrollState())
        ) {
*/

        val listState = rememberLazyListState()
        val dragDropState = rememberDragDropState(
            lazyListState = listState,
            confirmDrag = { index -> index != 0 }
        ) { fromIndex, toIndex ->
            if (fromIndex != 0 && toIndex != 0) doSwap(fromIndex, toIndex)
        }

        //Column (/*modifier = Modifier.verticalScroll(rememberScrollState())*/) {

        Row {
            Text(
                text = LocalContext.current.getString(R.string.custom_filter_criteria), //"Header will be here",  //R.string.custom_filter_criteria
                color = MaterialTheme.colors.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        Row {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .doDrag(dragDropState),
                userScrollEnabled = true,
                state = listState
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id }
                ) { index, criterion ->
                    if (index == 0) {
                        FilterConditionRow(criterion, false, getIcon, onClick)
                    } else {
                        DraggableItem(
                            dragDropState = dragDropState, index = index
                        ) { dragging ->
                            SwipeOut(
                                decoration = { SwipeOutDecoration { onDelete(index) } },
                                onSwipe = { index -> onDelete(index) },
                                index = index
                            ) {
                                FilterConditionRow(criterion, dragging, getIcon, onClick )
                            }
                        }
                    }
                }
            }
        }
    //}
} /* FilterCondition */

@Composable
private fun FilterConditionRow(
    criterion: CriterionInstance,
    dragging: Boolean,
    getIcon: (CriterionInstance) -> Int,
    onClick: (String) -> Unit
) {
    Divider(
        color = when (criterion.type) {
            CriterionInstance.TYPE_ADD -> Color.LightGray
            else -> Color.Transparent
        }
    )

    val modifier =
        if (dragging) Modifier.background(Color.LightGray)
        else Modifier
    Row(
        modifier = modifier.clickable { onClick(criterion.id) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.requiredSize(56.dp),
            contentAlignment = Alignment.Center
        )
        {
            if (criterion.type != CriterionInstance.TYPE_UNIVERSE) {
                Icon(
                    painter = painterResource(id = getIcon(criterion)),
                    contentDescription = null
                )
            }
        }
        Text(
            text = criterion.titleFromCriterion,
            fontSize = 17.sp,
            modifier = Modifier
                .weight(0.8f)
                .padding(start = 20.dp, top = 16.dp, bottom = 16.dp)
        )
        val context = LocalContext.current
        val locale = remember {
            ConfigurationCompat
                .getLocales(context.resources.configuration)
                .get(0)
                ?: Locale.getDefault()
        }
        Text(
            text = locale.formatNumber(criterion.max),
            modifier = Modifier.padding(16.dp),
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SwipeOutDecoration(onClick: () -> Unit = {}) {
    Box( modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.secondary)
        //.background(Color.Red)
    ) {

        @Composable
        fun delIcon() {
            IconButton(onClick = onClick) {
                Icon(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            delIcon()
            delIcon()
        }
    }
} /* end SwipeOutDecoration */

@Composable
fun AddCriteriaButton(
    isExtended: MutableState<Boolean>,
    onClick: () -> Unit
) {

    Box( // lays out over main content as space to layout FAB
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.padding(8.dp),
            shape = RoundedCornerShape(50),
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color.White,
        ) {
            val extended = isExtended.value

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Criteria",
                    modifier = Modifier.padding(
                        start = if (extended) 16.dp else 0.dp
                    )
                )
                if (extended)
                    Text(
                        text = LocalContext.current.getString(R.string.CFA_button_add),
                        modifier = Modifier.padding(end = 16.dp)
                    )
            }
        } /* end FloatingActionButton */
    }
}


@Composable
fun ToggleGroup (
    items: List<String>,
    selected: MutableIntState = remember { mutableIntStateOf( 0 ) }
) {
    assert(selected.value in items.indices)
    Row {
        for (index in items.indices) {
            val highlight = (index == selected.value)
            Text(
                text = items[index],
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clickable { selected.value = index }
                    .background(
                        color =
                        if (highlight) Color.Red.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
            )
        }
    }
}

@Composable
fun SelectCriterionType(
    title: String,
    selected: Int,
    types: List<String>,
    onCancel: () -> Unit,
    onSelected: (Int) -> Unit
) {
    val selected = remember { mutableIntStateOf(selected) }
    Dialog(onDismissRequest = onCancel)
    {
        Column {
            Text(
                text = title,
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.body1,
            )
            ToggleGroup(items = types, selected = selected)
            Row {
                Text(
                    text = "Cancel",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { onCancel() }
                )
                Text(
                    text = "OK",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { onSelected(selected.value) }
                )
            }
        }
    }
}