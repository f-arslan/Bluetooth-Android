package com.espressodev.bluetooth.common.component

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.espressodev.bluetooth.common.TicTacToeIcons
import com.espressodev.bluetooth.R.string as AppText


@Composable
fun TicTacToeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes title: Int,
    opponentEndpointId: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TicTacToeTextButton(onClick = onConfirm, text = AppText.confirm) },
        dismissButton = { TicTacToeTextButton(onClick = onDismiss, text = AppText.dismiss) },
        icon = { Icon(imageVector = TicTacToeIcons.InfoOutlined, contentDescription = null) },
        title = { Text(text = stringResource(id = title)) },
        text = { Text(text = stringResource(id = AppText.auth_dialog_text, opponentEndpointId)) }
    )
}


@Composable
@Preview(showBackground = true)
fun DialogPreview() {
    TicTacToeDialog(onDismiss = {}, onConfirm = {}, AppText.auth_dialog_title, "Fatih")
}