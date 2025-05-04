package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.runtime.Composable
import org.tasks.data.Location
import org.tasks.data.displayName

private val locationIcon = Icons.Outlined.LocationOn

@Composable
fun LocationChip (
    current: Location?,
    setLocation: (Location?) -> Unit,
    pickLocation: () -> Unit
) {
    fun trunk(s: String, len: Int = 10): String =
        s.takeIf{ it.length <= len } ?: (s.substring(0..len-3) + "...")

    if (current == null) {
        IconChip(icon = locationIcon, action = pickLocation)
    } else {
        Chip(
            title = trunk(current.displayName),
            leading = locationIcon,
            action = pickLocation,
            delete = { setLocation(null) }
        )
    }

}