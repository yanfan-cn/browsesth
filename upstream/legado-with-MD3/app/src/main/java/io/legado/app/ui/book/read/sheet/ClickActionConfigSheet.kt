package io.legado.app.ui.book.read.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.data.repository.ReadPreferences
import io.legado.app.data.repository.ReadSettingsRepository
import io.legado.app.feature.reader.legacy.ReaderClickActionResolver
import io.legado.app.model.ReadBook
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.alert.AppAlertDialog
import io.legado.app.ui.widget.components.card.GlassCard
import io.legado.app.ui.widget.components.text.AppText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ClickActionConfigSheet(
    onDismissRequest: () -> Unit,
) {
    BackHandler(onBack = onDismissRequest)

    val readSettingsRepository: ReadSettingsRepository = koinInject()
    val preferences by readSettingsRepository.preferences.collectAsStateWithLifecycle(
        initialValue = ReadPreferences()
    )
    val scope = rememberCoroutineScope()
    val scrollProfile = ReaderClickActionResolver.isScrollProfile(ReadBook.pageAnim())
    val grid = ReaderClickActionResolver.resolve(ReadBook.pageAnim(), preferences)

    val actions = linkedMapOf(
        -1 to stringResource(R.string.non_action),
        0 to stringResource(R.string.menu),
        1 to stringResource(R.string.next_page),
        2 to stringResource(R.string.prev_page),
        3 to stringResource(R.string.next_chapter),
        4 to stringResource(R.string.previous_chapter),
        5 to stringResource(R.string.read_aloud_prev_paragraph),
        6 to stringResource(R.string.read_aloud_next_paragraph),
        7 to stringResource(R.string.bookmark_add),
        8 to stringResource(R.string.edit_content),
        9 to stringResource(R.string.replace_state_change),
        10 to stringResource(R.string.chapter_list),
        11 to stringResource(R.string.search_content),
        12 to stringResource(R.string.sync_book_progress_t),
        13 to stringResource(R.string.read_aloud_pause_resume),
    )

    fun prefKey(base: String): String = if (scrollProfile) {
        when (base) {
            PreferKey.clickActionTL -> PreferKey.clickActionScrollTL
            PreferKey.clickActionTC -> PreferKey.clickActionScrollTC
            PreferKey.clickActionTR -> PreferKey.clickActionScrollTR
            PreferKey.clickActionML -> PreferKey.clickActionScrollML
            PreferKey.clickActionMC -> PreferKey.clickActionScrollMC
            PreferKey.clickActionMR -> PreferKey.clickActionScrollMR
            PreferKey.clickActionBL -> PreferKey.clickActionScrollBL
            PreferKey.clickActionBC -> PreferKey.clickActionScrollBC
            PreferKey.clickActionBR -> PreferKey.clickActionScrollBR
            else -> base
        }
    } else {
        base
    }

    var selectingPrefKey by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LegadoTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f))
            .clickable(onClick = onDismissRequest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            Spacer(modifier = Modifier.padding(top = 36.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                ClickAreaCell(
                    label = actions[grid.tl] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp),
                    onClick = { selectingPrefKey = prefKey(PreferKey.clickActionTL) },
                )
                ClickAreaCell(
                    label = actions[grid.tc] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp),
                    onClick = { selectingPrefKey = prefKey(PreferKey.clickActionTC) },
                )
                ClickAreaCell(
                    label = actions[grid.tr] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp),
                    onClick = { selectingPrefKey = prefKey(PreferKey.clickActionTR) },
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                ClickAreaCell(
                    label = actions[grid.ml] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp),
                    onClick = { selectingPrefKey = prefKey(PreferKey.clickActionML) },
                )
                ClickAreaCell(
                    label = actions[grid.mc] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp),
                    onClick = { selectingPrefKey = prefKey(PreferKey.clickActionMC) },
                )
                ClickAreaCell(
                    label = actions[grid.mr] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp),
                    onClick = { selectingPrefKey = prefKey(PreferKey.clickActionMR) },
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                ClickAreaCell(
                    label = actions[grid.bl] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp),
                    onClick = { selectingPrefKey = prefKey(PreferKey.clickActionBL) },
                )
                ClickAreaCell(
                    label = actions[grid.bc] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp),
                    onClick = { selectingPrefKey = prefKey(PreferKey.clickActionBC) },
                )
                ClickAreaCell(
                    label = actions[grid.br] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(3.dp),
                    onClick = { selectingPrefKey = prefKey(PreferKey.clickActionBR) },
                )
            }
        }
    }

    val actionKeys = actions.keys.toList()
    val actionValues = actions.values.toList()
    AppAlertDialog(
        show = selectingPrefKey != null,
        onDismissRequest = { selectingPrefKey = null },
        title = stringResource(R.string.select_action),
        content = {
            Column {
                actionValues.forEachIndexed { index, label ->
                    AppText(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val selectedAction = actionKeys[index]
                                selectingPrefKey?.let { key ->
                                    scope.launch {
                                        readSettingsRepository.setClickAction(key, selectedAction)
                                        selectingPrefKey = null
                                    }
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        style = LegadoTheme.typography.bodyLarge,
                    )
                }
            }
        },
    )
}

@Composable
private fun ClickAreaCell(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = LegadoTheme.colorScheme.surfaceContainer
            .copy(alpha = 0.9f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = label,
                style = LegadoTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
