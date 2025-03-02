package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.edit.Priority
import org.tasks.data.entity.Task
import org.tasks.themes.TasksTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityPickerDialog (
    selected: Int,
    onClick: (Int) -> Unit = {},
    desaturate: Boolean = false,
    onDismissRequest: () -> Unit = {}
) {
    TasksTheme {
        BasicAlertDialog(
            onDismissRequest = onDismissRequest,
        ) {
            PriorityPicker(selected, onClick, desaturate)
        }
    }
}

@Composable
fun PriorityPicker(
    selected: Int,
    onClick: (Int) -> Unit = {},
    desaturate: Boolean = false
) {
    Card () {
        Column(
            modifier = Modifier
                .height(120.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center

        ) {
            Text(
                text = stringResource(id = R.string.TEA_importance_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.keyline_first))
            )
            Spacer(modifier = Modifier.weight(1f))
            Priority(selected = selected, onClick = onClick, desaturate = desaturate)
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
fun PriorityPickerDialogPreview()
{
    PriorityPickerDialog(selected = Task.Priority.MEDIUM, desaturate = false)
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
fun PriorityPickerDialogDesaturetePreview()
{
    PriorityPickerDialog(selected = Task.Priority.MEDIUM, desaturate = true)
}