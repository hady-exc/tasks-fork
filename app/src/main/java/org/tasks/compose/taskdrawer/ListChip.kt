package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.runtime.Composable
import org.tasks.filters.Filter

private val listIcon = Icons.AutoMirrored.Outlined.List

@Composable
fun ListChip(
    initialFilter: Filter,
    currentFiler: Filter,
    setFilter: (Filter) -> Unit,
    pickList: () -> Unit
) {
    Chip(
        title = currentFiler.title!!,
        leading = listIcon,
        action = pickList,
        delete =
            if (currentFiler == initialFilter) { null }
            else { { setFilter(initialFilter) } }
    )
}