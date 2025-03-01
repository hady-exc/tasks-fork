package org.tasks.compose.taskdrawer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor

val icon = Icons.Outlined.Flag

@Composable
fun PriorityChip(current: Int, action: () -> Unit)
{
    val color = Color(
        priorityColor(
            priority = current,
            isDarkMode = isSystemInDarkTheme(),
            desaturate = false, // TODO -- get from preferemnces
        )
    )

    IconChip(icon = icon, iconColor = color, action = action)

}