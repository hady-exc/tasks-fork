package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Label
import androidx.compose.runtime.Composable
import org.tasks.data.entity.TagData

private val tagIcon = Icons.Outlined.Label

@Composable
fun TagsChip(
    current: List<TagData>,
    action: () -> Unit,
    delete: (() -> Unit)? = null,
) {
    fun String.trim(): String =
        if (this.length <= 12) this else this.substring(0,8) + "..."

    val title = current
        .mapNotNull { it.name }
        .foldIndexed("",
            { index, text, string ->
                text + (if (index>0) ", " else "") + string
            }
        )
        .trim()

    if (!current.isEmpty()) {
        Chip(
            title = title,
            leading = tagIcon,
            action = action,
            delete = delete
        )
    } else {
        IconChip(tagIcon, action = action)
    }
}