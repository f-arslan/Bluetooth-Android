package com.espressodev.bluetooth.common.component

import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun TicTacToeTextButton(onClick: () -> Unit, @StringRes text: Int) {
    TextButton(onClick = onClick) {
        Text(stringResource(text))
    }
}