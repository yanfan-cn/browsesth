package io.legado.app.ui.config.backupConfig

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.legado.app.R
import io.legado.app.ui.widget.components.modalBottomSheet.OptionCard
import io.legado.app.ui.widget.components.modalBottomSheet.OptionSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupOptionSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onBackupToLocal: () -> Unit,
    onBackupToNetwork: () -> Unit,
    onBackupToLocalAndNetwork: () -> Unit,
) {
    OptionSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.backup),
    ) {
        OptionCard(
            icon = Symbols.PhoneAndroid,
            text = stringResource(R.string.backup_to_local),
            onClick = onBackupToLocal,
        )
        OptionCard(
            icon = Symbols.Cloud,
            text = stringResource(R.string.backup_to_network),
            onClick = onBackupToNetwork,
        )
        OptionCard(
            icon = Symbols.Lan,
            text = stringResource(R.string.backup_to_local_and_network),
            onClick = onBackupToLocalAndNetwork,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreOptionSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onRestoreFromLocal: () -> Unit,
    onRestoreFromNetwork: () -> Unit,
) {
    OptionSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.restore),
    ) {
        OptionCard(
            icon = Symbols.PhoneAndroid,
            text = stringResource(R.string.restore_from_local),
            onClick = onRestoreFromLocal,
        )
        OptionCard(
            icon = Symbols.Cloud,
            text = stringResource(R.string.restore_from_network),
            onClick = onRestoreFromNetwork,
        )
    }
}
