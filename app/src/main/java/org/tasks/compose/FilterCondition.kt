package org.tasks.compose

/**
 *  Composables for FilterSettingActivity
 **/

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
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
            CriterionInstance.TYPE_ADD -> R.drawable.ic_call_split_24px
            CriterionInstance.TYPE_SUBTRACT -> R.drawable.ic_outline_not_interested_24px
            CriterionInstance.TYPE_INTERSECT -> R.drawable.ic_outline_add_24px
            else -> { 0 }  /* assert */
       }
    }
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        confirmDrag = { index -> index != 0 }
    ) { fromIndex, toIndex ->
        if (fromIndex != 0 && toIndex != 0) doSwap(fromIndex, toIndex)
    }

    Row {
        Text(
            text =  stringResource(id = R.string.custom_filter_criteria),
            color = MaterialTheme.colors.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
    Row {
        LazyColumn(
            modifier = Modifier.fillMaxSize().doDrag(dragDropState),
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
            modifier = Modifier.height(56.dp),
            contentAlignment = Alignment.Center
        )
        {
            if (criterion.type != CriterionInstance.TYPE_UNIVERSE) {
                Icon(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    painter = painterResource(id = getIcon(criterion)),
                    contentDescription = null
                )
            }
        }
        Text(
            text = criterion.titleFromCriterion,
            fontSize = 18.sp,
            modifier = Modifier
                .weight(0.8f)
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
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
    ) {

        @Composable
        fun deleteIcon() {
            Icon(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = Color.White
            )
        }

        Row(
            modifier = Modifier.fillMaxSize().height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            deleteIcon()
            deleteIcon()
        }
    }
} /* end SwipeOutDecoration */

@Composable
fun NewCriterionFAB(
    isExtended: MutableState<Boolean>,
    onClick: () -> Unit
) {

    Box( // lays out over main content as a space to layout FAB
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.padding(16.dp),
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
} /* end NewCriterionFAB */

@Composable
fun SelectCriterionType(
    title: String,
    selected: Int,
    types: List<String>,
    onCancel: () -> Unit,
    help: () -> Unit = {},
    onSelected: (Int) -> Unit
) {
    val selected = remember { mutableIntStateOf(selected) }
    
    Dialog(onDismissRequest = onCancel)
    {
        Card(
            backgroundColor = MaterialTheme.colors.background
        ) {
            Column (modifier = Modifier.padding(horizontal = 20.dp)){
                Text(
                    text = title,
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 16.dp)
                )
                ToggleGroup(items = types, selected = selected)
                Row (
                    modifier = Modifier.height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box( contentAlignment = Alignment.CenterStart ) {
                        Text(
                            text = stringResource(R.string.help).uppercase(),
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier.clickable { help() }
                        )
                    }
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row {
                            Text(
                                text = stringResource(R.string.cancel).uppercase(),
                                color = MaterialTheme.colors.secondary,
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .clickable { onCancel() }
                            )
                            Text(
                                text = stringResource(R.string.ok).uppercase(),
                                color = MaterialTheme.colors.secondary,
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .clickable { onSelected(selected.intValue) }
                            )
                        }
                    }
                }
            }
        }
    }
} /* end SelectCriterionType */

@Composable
fun ToggleGroup (
    items: List<String>,
    selected: MutableIntState = remember { mutableIntStateOf( 0 ) }
) {
    assert(selected.intValue in items.indices)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row {
            for (index in items.indices) {
                val highlight = (index == selected.intValue)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(88.dp)
                        .background(
                            color =
                            if (highlight) MaterialTheme.colors.secondary.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                        .clickable { selected.intValue = index }
                        .border(
                            width = (1.5).dp,
                            color = if (highlight) MaterialTheme.colors.secondary else Color.LightGray
                        )
                        .zIndex(zIndex = if (highlight) 1f else 0f)
                ) {
                    Text(
                        text = items[index],
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
} /* end ToggleGroup */


@Composable
fun SelectFromList(
    names: List<String>,
    title: String? = null,
    onCancel: () -> Unit,
    onSelected: (Int) -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Card (backgroundColor = MaterialTheme.colors.background) {
            Column (modifier = Modifier.padding(horizontal = 20.dp)) {
                title?.let { title ->
                    Text(
                        text = title,
                        color = MaterialTheme.colors.onSurface,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 16.dp)
                    )
                }
                names.forEachIndexed { index, name ->
                    Text(
                        text = name,
                        color = MaterialTheme.colors.onSurface,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 16.dp)
                            .clickable { onSelected(index) }
                    )
                }
            }
        }
    }
} /* end SelectFromList */


@Composable
fun InputTextOption(
    title: String,
    text: MutableState<String>,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Card (backgroundColor = MaterialTheme.colors.background) {
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.body1,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                )
                val focused = remember { mutableStateOf( false ) }

                BasicTextField(
                    value = text.value,
                    textStyle = TextStyle(
                        fontSize = LocalTextStyle.current.fontSize,
                        color = MaterialTheme.colors.onSurface
                    ),
                    onValueChange = {text.value = it },
                    cursorBrush = SolidColor(MaterialTheme.colors.secondary),
                    modifier = Modifier
                        .padding(bottom = 3.dp, start = 12.dp, end = 12.dp)
                        .onFocusChanged { focused.value = (it.isFocused) }
                )
                Divider(
                    color =
                        if (focused.value) MaterialTheme.colors.secondary
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row (modifier = Modifier.padding(end = 16.dp)){
                        Text(
                            text = stringResource(R.string.cancel).uppercase(),
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .clickable { onCancel() }
                        )
                        Text(
                            text = stringResource(R.string.ok).uppercase(),
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .clickable { onDone() }
                        )
                    }
                }
            }
        }
    }
} /* end InputTextOption */