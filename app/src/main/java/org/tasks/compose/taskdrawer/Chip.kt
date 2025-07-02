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
fun IconChip(icon: ImageVector, contentColor: Color = Color.Unspecified, action: (() -> Unit)) = Chip(null, null, action, null, icon, contentColor)

@Composable
fun Chip (
    title: String?,
    leading: ImageVector?,
    action: () -> Unit,
    delete: (() -> Unit)? = null,
    titleIcon: ImageVector? = null,
    contentColor: Color = Color.Unspecified,
) = InputChip (
    selected = false,
    onClick = action,
    label = {
        title?.let {
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        titleIcon?.let { Icon(titleIcon, null, tint = if (contentColor == Color.Unspecified) LocalContentColor.current else contentColor) }
    },
    leadingIcon = {
        leading?.let { Icon(leading, null, tint = if (contentColor == Color.Unspecified) LocalContentColor.current else contentColor) }
    },
    trailingIcon = {
        delete?.let { Icon(clearIcon, null, Modifier.clickable(onClick = delete), tint = if (contentColor == Color.Unspecified) LocalContentColor.current else contentColor) }
    }
)

