package org.tasks.compose

import android.graphics.drawable.Drawable
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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

            Text(text = "Header will be here", modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp))

            LazyColumn(
                modifier = Modifier
                    .height(800.dp)
                    .doDrag(dragDropState),
                userScrollEnabled = false,
                state = listState
            ) {
/*
                item {
                    Text(text = "Header will be here", modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp))
                }

*/
                itemsIndexed(
                    items = items,
                    key = { _,item -> item.id }
                ) { index, criterion ->
                    DraggableItem(
                        dragDropState = dragDropState, index = index
                    ) {


                        SwipeOut(
                            decoration = { SwipeOutDecoration() },
                            onSwipe = { index -> onDelete(index) },
                            index = index
                        ) {
                            if (criterion.type == CriterionInstance.TYPE_ADD) {
                                Divider(color = Color.Black)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.requiredSize(48.dp),
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
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text("${criterion.max}", modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
} /* FilterCondition */

@Composable
private fun SwipeOutDecoration() {
    Box( modifier = Modifier
        .fillMaxSize()
        .background(Color.Red) ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                modifier = Modifier.padding(horizontal = 20.dp),
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = Color.White
            )
            androidx.compose.material3.Icon(
                modifier = Modifier.padding(horizontal = 20.dp),
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
} /* end SwipeOutDecoration */
