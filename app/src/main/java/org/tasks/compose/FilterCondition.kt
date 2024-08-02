package org.tasks.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.core.CriterionInstance
import org.tasks.R
import org.tasks.compose.SwipeOut.SwipeOut


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterCondition (
    items: SnapshotStateList<CriterionInstance>,  //MutableState<MutableList<CriterionInstance>>, //State<List<CriterionInstance>>,
    onDelete: (Int) -> Unit,
    doSwap: (Int, Int) -> Unit
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

    MdcTheme {
        Box(modifier = Modifier.fillMaxWidth()) {

            val listState = rememberLazyListState()
            val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
                doSwap(fromIndex, toIndex)
            }

            Column {

                Text(
                    text = LocalContext.current.getString(R.string.custom_filter_criteria), //"Header will be here",  //R.string.custom_filter_criteria
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .height(800.dp)
                        .doDrag(dragDropState),
                    userScrollEnabled = false,
                    state = listState
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.id }
                    ) { index, criterion ->
                        DraggableItem(
                            dragDropState = dragDropState, index = index
                        ) { dragging ->
                            SwipeOut(
                                decoration = { SwipeOutDecoration{ onDelete(index) } },
                                onSwipe = { index -> onDelete(index) },
                                index = index
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
                                    modifier = modifier,
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
                                        fontSize = 18.sp,
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .padding(start = 20.dp, top = 16.dp, bottom = 16.dp)
                                    )
                                    Text(
                                        text = "${criterion.max}",
                                        modifier = Modifier.padding(16.dp),
                                        color = Color.Gray,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} /* FilterCondition */

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
