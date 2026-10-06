package io.legado.app.ui.book.manga

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.book.read.sheet.ReaderBookHeader
import io.legado.app.ui.book.read.sheet.ReaderBookHeaderState
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenu
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenuItem
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.reader.ReaderMenuActionSquare

private data class MangaMoreAction(
    val label: String,
    val icon: MaterialSymbol,
    val intent: MangaReaderIntent,
    val longClickIntent: MangaReaderIntent? = null,
)

@Composable
internal fun MangaReaderSourceActionsSheet(
    state: MangaReaderUiState,
    onIntent: (MangaReaderIntent) -> Unit,
) {
    AppModalBottomSheet(
        show = true,
        onDismissRequest = { onIntent(MangaReaderIntent.DismissSheet) },
        contentPaddingEnabled = false,
    ) {
        fun dispatch(intent: MangaReaderIntent) {
            onIntent(MangaReaderIntent.DismissSheet)
            onIntent(intent)
        }
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            ReaderBookHeader(
                state = ReaderBookHeaderState(
                    bookUrl = state.bookUrl,
                    sourceUrl = state.sourceUrl.orEmpty(),
                    sourceName = state.sourceName,
                    title = state.bookName,
                    author = state.bookAuthor,
                    coverUrl = state.coverUrl,
                    customCoverUrl = state.customCoverUrl,
                    chapterTitle = state.chapterName,
                    chapterIndex = state.chapterIndex,
                    chapterCount = state.chapterCount,
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        val actions = buildList {
            if (state.sourceCustomButtonAvailable) add(
                MangaMoreAction(
                    stringResource(R.string.custom_button),
                    Symbols.Extension,
                    MangaReaderIntent.SourceCustomButton(false),
                    MangaReaderIntent.SourceCustomButton(true),
                )
            )
            addAll(
                listOf(
                    MangaMoreAction(
                        stringResource(R.string.change_origin),
                        Symbols.SwapHoriz,
                        MangaReaderIntent.ChangeSource
                    ),
                    MangaMoreAction(
                        stringResource(R.string.refresh),
                        Symbols.Refresh,
                        MangaReaderIntent.RefreshChapter
                    ),
                    MangaMoreAction(
                        stringResource(R.string.login),
                        Symbols.Login,
                        MangaReaderIntent.OpenSourceLogin
                    ),
                    MangaMoreAction(
                        stringResource(R.string.manga_reader_buy_chapter),
                        Symbols.ShoppingCart,
                        MangaReaderIntent.RequestPayCurrentChapter
                    ),
                    MangaMoreAction(
                        stringResource(R.string.edit_source),
                        Symbols.Edit,
                        MangaReaderIntent.OpenSourceEdit
                    ),
                    MangaMoreAction(
                        stringResource(R.string.disable_source),
                        Symbols.MoreVert,
                        MangaReaderIntent.DisableCurrentSource
                    ),
                )
            )
        }
        Column(Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            actions.chunked(4).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowActions.forEach { action ->
                        MangaSourceActionSquare(
                            action = action,
                            dispatch = ::dispatch,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - rowActions.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MangaSourceActionSquare(
    action: MangaMoreAction,
    dispatch: (MangaReaderIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(action.intent) { mutableStateOf(false) }
    Box(modifier) {
        ReaderMenuActionSquare(
            icon = action.icon,
            text = action.label,
            hasMore = action.longClickIntent != null,
            onClick = { dispatch(action.intent) },
            onMoreClick = { expanded = true },
        )
        RoundDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) { dismiss ->
            action.longClickIntent?.let { intent ->
                RoundDropdownMenuItem(
                    text = stringResource(R.string.source_custom_button_long_action),
                    onClick = { dismiss(); dispatch(intent) },
                )
            }
        }
    }
}

@Composable
internal fun MangaReaderPageActionsSheet(
    hasSpread: Boolean,
    onIntent: (MangaReaderIntent) -> Unit,
) {
    AppModalBottomSheet(
        show = true,
        onDismissRequest = { onIntent(MangaReaderIntent.DismissSheet) },
        contentPaddingEnabled = false,
    ) {
        val actions = buildList {
            add(
                MangaMoreAction(
                    stringResource(R.string.save_image), Symbols.Save,
                    MangaReaderIntent.ExecutePageAction(MangaPageAction.SAVE)
                )
            )
            add(
                MangaMoreAction(
                    stringResource(R.string.share), Symbols.Share,
                    MangaReaderIntent.ExecutePageAction(MangaPageAction.SHARE)
                )
            )
            add(
                MangaMoreAction(
                    stringResource(R.string.copy_text), Symbols.ContentCopy,
                    MangaReaderIntent.ExecutePageAction(MangaPageAction.COPY)
                )
            )
            add(
                MangaMoreAction(
                    stringResource(R.string.manga_reader_set_cover), Symbols.Image,
                    MangaReaderIntent.ExecutePageAction(MangaPageAction.SET_COVER)
                )
            )
            if (hasSpread) {
                add(
                    MangaMoreAction(
                        stringResource(R.string.manga_reader_save_spread), Symbols.Save,
                        MangaReaderIntent.ExecutePageAction(MangaPageAction.SAVE_SPREAD)
                    )
                )
                add(
                    MangaMoreAction(
                        stringResource(R.string.manga_reader_share_spread), Symbols.Share,
                        MangaReaderIntent.ExecutePageAction(MangaPageAction.SHARE_SPREAD)
                    )
                )
                add(
                    MangaMoreAction(
                        stringResource(R.string.manga_reader_copy_spread),
                        Symbols.ContentCopy,
                        MangaReaderIntent.ExecutePageAction(MangaPageAction.COPY_SPREAD)
                    )
                )
            }
        }
        Column(Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            actions.chunked(4).forEach { rowActions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowActions.forEach { action ->
                        ReaderMenuActionSquare(
                            action.icon,
                            action.label,
                            Modifier.weight(1f),
                            onClick = { onIntent(action.intent) },
                        )
                    }
                    repeat(4 - rowActions.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
internal fun MangaReaderCacheActionsSheet(
    state: MangaReaderUiState,
    onIntent: (MangaReaderIntent) -> Unit,
) {
    AppModalBottomSheet(
        show = true,
        onDismissRequest = { onIntent(MangaReaderIntent.DismissSheet) },
        contentPaddingEnabled = false,
    ) {
        val actions = listOf(
            MangaMoreAction(
                stringResource(R.string.manga_reader_cache_current),
                Symbols.Download,
                MangaReaderIntent.CacheChapters(MangaCacheSelection.CURRENT),
            ),
            MangaMoreAction(
                stringResource(R.string.manga_reader_cache_following),
                Symbols.CloudDownload,
                MangaReaderIntent.CacheChapters(MangaCacheSelection.FOLLOWING),
            ),
            MangaMoreAction(
                stringResource(R.string.manga_reader_cache_all),
                Symbols.DownloadForOffline,
                MangaReaderIntent.CacheChapters(MangaCacheSelection.ALL),
            ),
        )
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            ReaderBookHeader(
                state = ReaderBookHeaderState(
                    bookUrl = state.bookUrl,
                    sourceUrl = state.sourceUrl.orEmpty(),
                    sourceName = state.sourceName,
                    title = state.bookName,
                    author = state.bookAuthor,
                    coverUrl = state.coverUrl,
                    customCoverUrl = state.customCoverUrl,
                    chapterTitle = state.chapterName,
                    chapterIndex = state.chapterIndex,
                    chapterCount = state.chapterCount,
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.forEach { action ->
                ReaderMenuActionSquare(
                    icon = action.icon,
                    text = action.label,
                    modifier = Modifier.weight(1f),
                    onClick = { onIntent(action.intent) },
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}
