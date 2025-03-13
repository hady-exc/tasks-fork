package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.runtime.Composable

private val descriptionIcon = Icons.Outlined.EditNote

@Composable
fun DescriptionChip(
    show: Boolean,
    action: () -> Unit
) {
    if (show)
        IconChip(
            icon = descriptionIcon,
            action = action
        )
}