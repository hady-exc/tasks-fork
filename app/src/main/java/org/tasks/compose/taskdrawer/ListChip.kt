package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.runtime.Composable
import org.tasks.filters.Filter

private val listIcon = Icons.AutoMirrored.Outlined.List

@Composable
fun ListChip(
    originalFilter: Filter,
    initialFilter: Filter,
    currentFiler: Filter,
    setFilter: (Filter) -> Unit,
    pickList: () -> Unit
) {
    if (initialFilter == originalFilter && currentFiler == initialFilter) {
        IconChip(icon = listIcon, action = pickList)
    } else {
        Chip(
            title = currentFiler.title!!,
            leading = listIcon,
            action = pickList,
            delete =
                if (initialFilter == originalFilter || currentFiler == initialFilter) { null }
                else { { setFilter(initialFilter) } }
        )
    }
}