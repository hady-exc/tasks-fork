package org.tasks.compose.taskdrawer

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow

private val clearIcon = Icons.Outlined.Close

@Composable
fun IconChip(icon: ImageVector, iconColor: Color = Color.Unspecified, action: (() -> Unit)) = Chip(null, null, null, icon, iconColor, action)

@Composable
fun Chip (
    title: String?,
    leading: ImageVector?,
    delete: (() -> Unit)? = null,
    titleIcon: ImageVector? = null,
    iconColor: Color = Color.Unspecified,
    action: (() -> Unit),
) = InputChip (
    selected = false,
    onClick = action,
    label = {
        title?.let {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        titleIcon?.let { Icon(titleIcon, null, tint = if (iconColor == Color.Unspecified) LocalContentColor.current else iconColor) }
    },
    leadingIcon = {
        leading?.let { Icon(leading, null, tint = if (iconColor == Color.Unspecified) LocalContentColor.current else iconColor) }
    },
    trailingIcon = {
        delete?.let { Icon(clearIcon, null, Modifier.clickable(onClick = delete)) }
    }
)

