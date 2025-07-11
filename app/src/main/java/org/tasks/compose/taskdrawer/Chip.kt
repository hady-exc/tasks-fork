package org.tasks.compose.taskdrawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowDpSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val clearIcon = Icons.Outlined.Close
private val gap = 4.dp

@Composable
fun IconChip(icon: ImageVector, contentColor: Color = Color.Unspecified, action: (() -> Unit)) =
    Chip(null, null, action, null, icon, contentColor)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun Chip (
    title: String?,
    leading: ImageVector?,
    action: () -> Unit,
    delete: (() -> Unit)? = null,
    titleIcon: ImageVector? = null,
    contentColor: Color = Color.Unspecified,
) {
    val screenWidth = currentWindowDpSize().width
    val widthShort = if (screenWidth <= 400.dp) {
        (screenWidth - 32.dp - gap * 5) / 5
    } else {
        (screenWidth - 32.dp - gap * 6) / 6
    }
    val widthLong = widthShort * 2 + 4.dp
    val width = if (title == null) widthShort else widthLong
    InputChip(
        selected = false,
        onClick = action,
        label = {
            title?.let {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = contentColor,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            titleIcon?.let {
                Icon(
                    imageVector = titleIcon,
                    contentDescription = null,
                    tint = if (contentColor == Color.Unspecified) LocalContentColor.current else contentColor
                )
            }
        },
        leadingIcon = {
            leading?.let {
                Icon(
                    imageVector = leading,
                    contentDescription = null,
                    tint = if (contentColor == Color.Unspecified) LocalContentColor.current else contentColor
                )
            }
        },
        trailingIcon = {
            delete?.let {
                Icon(
                    imageVector = clearIcon,
                    contentDescription = null,
                    modifier = Modifier.clickable(onClick = delete),
                    tint = if (contentColor == Color.Unspecified) LocalContentColor.current else contentColor
                )
            }
        },
        modifier = Modifier.requiredWidth(width)
    )
}

