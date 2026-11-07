package io.legado.app.ui.book.info

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.flowOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.domain.usecase.ChangeSourceMigrationOptions
import io.legado.app.ui.book.changecover.ChangeCoverViewModel
import io.legado.app.ui.book.group.GroupEditSheet
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.image.cover.CoverPaletteStore
import io.legado.app.ui.widget.components.image.cover.GeneratedCover
import io.legado.app.ui.widget.components.image.cover.GeneratedCoverFace
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.button.series.SmallPlainButton
import io.legado.app.ui.widget.components.card.GlassCard
import io.legado.app.ui.widget.components.card.SelectionItemCard
import io.legado.app.ui.widget.components.changeSource.ChangeSourceSheet
import io.legado.app.ui.widget.components.checkBox.AppCheckbox
import io.legado.app.ui.widget.components.image.cover.CoilBookCover
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.progressIndicator.AppLinearProgressIndicator
import io.legado.app.ui.widget.components.text.AppText
import org.koin.androidx.compose.koinViewModel

@Composable
fun WebFileSheet(
    show: Boolean,
    files: List<BookInfoWebFile>,
    title: String,
    onDismissRequest: () -> Unit,
    onSelect: (BookInfoWebFile) -> Unit,
) {
    AppModalBottomSheet(show = show, onDismissRequest = onDismissRequest, title = title) {
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp), contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.empty))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files, key = { it.name }) { file ->
                    GlassCard(onClick = { onSelect(file) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SymbolIcon(if (file.name.endsWith("zip") || file.name.endsWith("rar") || file.name.endsWith("7z")) Symbols.FolderZip else Symbols.Image, null)
                            Text(text = file.name, modifier = Modifier.weight(1f), style = LegadoTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GroupSelectSheet(
    show: Boolean,
    groups: List<BookGroup>,
    currentGroupId: Long,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var selectedGroupId by remember(currentGroupId, show) { mutableLongStateOf(currentGroupId) }
    var editingGroup by remember(show) { mutableStateOf<BookGroup?>(null) }
    var showAddGroup by remember(show) { mutableStateOf(false) }

    AppModalBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.group_select),
        startAction = {
            MediumTonalButton(
                onClick = { showAddGroup = true },
                icon = Symbols.Add,
                contentDescription = stringResource(R.string.group_add)
            )
        },
        endAction = {
            MediumTonalButton(
                onClick = { onConfirm(selectedGroupId) },
                icon = Symbols.Check,
                contentDescription = stringResource(R.string.confirm)
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups, key = { it.groupId }) { group ->
                    val isSelected = (selectedGroupId and group.groupId) != 0L
                    SelectionItemCard(
                        title = group.groupName,
                        isSelected = isSelected,
                        onToggleSelection = {
                            selectedGroupId = if (isSelected) {
                                selectedGroupId - group.groupId
                            } else {
                                selectedGroupId + group.groupId
                            }
                        },
                        leadingContent = {
                            AppCheckbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selectedGroupId = if (it) {
                                        selectedGroupId + group.groupId
                                    } else {
                                        selectedGroupId - group.groupId
                                    }
                                }
                            )
                        },
                        trailingAction = {
                            SmallPlainButton(
                                onClick = { editingGroup = group },
                                icon = Symbols.Edit,
                                contentDescription = stringResource(R.string.edit)
                            )
                        },
                        containerColor = LegadoTheme.colorScheme.surfaceContainerLow
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
    GroupEditSheet(
        show = showAddGroup || editingGroup != null,
        group = editingGroup,
        onDismissRequest = {
            showAddGroup = false
            editingGroup = null
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChangeCoverSheet(
    show: Boolean,
    name: String,
    author: String,
    onDismissRequest: () -> Unit,
    onSelect: (String) -> Unit,
    /** SPEC-160: http(s) webpage cover when available (净读网页书). */
    webCoverUrl: String? = null,
    viewModel: ChangeCoverViewModel = koinViewModel(key = "cover-$name-$author"),
) {
    val items by remember(show) {
        // dataFlow 若无条件 collectAsStateWithLifecycle 收集会带来副作用：
        // ChangeCoverSheet 在编辑页（BookInfoEditContent）里是 show=false 也照样组合的，
        // callbackFlow 收集体立即执行：查库后若结果 ≤ 1 条就 startSearch() → 用全部启用
        // 书源并发搜书（每源执行搜索规则/登录检测脚本），这就是“每次点开编辑页都弹某书源
        // 未登录提示”的根源。
        // 这里只有用户真正打开换封面面板（show=true）才开始收集/自动搜索；
        // 隐藏时收集一个空的已完成 Flow，不触发任何网络与脚本。
        if (show) viewModel.dataFlow else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList<SearchBook>())
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val pageCover = webCoverUrl?.takeIf {
        it.startsWith("http://") || it.startsWith("https://")
    }
    val context = LocalContext.current
    val galleryCovers = remember(show) {
        if (show) CoverPaletteStore.galleryUris(context) else emptyList()
    }

    LaunchedEffect(name, author) {
        viewModel.initData(name, author)
    }
    DisposableEffect(show) {
        onDispose {
            viewModel.stopSearch()
        }
    }

    AppModalBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.change_cover_source),
        endAction = {
            MediumTonalButton(
                onClick = { viewModel.startOrStopSearch() },
                icon = if (isSearching) Symbols.MoreVert else Symbols.Refresh,
                contentDescription = stringResource(
                    if (isSearching) R.string.more_menu else R.string.refresh
                )
            )
        }
    ) {
        if (isSearching) {
            AppLinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // SPEC-160: shared color / gradient templates (NTP + book info).
            item(span = { GridItemSpan(3) }) {
                AppText(
                    text = "配色封面",
                    style = LegadoTheme.typography.titleSmallEmphasized,
                )
            }
            items(GeneratedCover.presets, key = { it.uri }) { preset ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSelect(preset.uri) }
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(5f / 7f)
                            .clip(RoundedCornerShape(4.dp)),
                    ) {
                        GeneratedCoverFace(
                            path = preset.uri,
                            title = name.takeIf { it.isNotBlank() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    AppText(
                        text = preset.label,
                        style = LegadoTheme.typography.labelSmallEmphasized,
                        maxLines = 1,
                    )
                }
            }
            if (galleryCovers.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    AppText(
                        text = "相册封面",
                        style = LegadoTheme.typography.titleSmallEmphasized,
                    )
                }
                items(galleryCovers, key = { it }) { uri ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onSelect(uri) }
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "相册封面",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(5f / 7f)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                        AppText(
                            text = "相册",
                            style = LegadoTheme.typography.labelSmallEmphasized,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (pageCover != null) {
                item(span = { GridItemSpan(3) }) {
                    AppText(
                        text = "网页封面",
                        style = LegadoTheme.typography.titleSmallEmphasized,
                    )
                }
                item(key = "web-cover") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onSelect(pageCover) }
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CoilBookCover(
                            name = name,
                            author = author,
                            path = pageCover,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(5f / 7f),
                        )
                        AppText(
                            text = "使用网页图片",
                            style = LegadoTheme.typography.labelSmallEmphasized,
                            maxLines = 2,
                        )
                    }
                }
            }
            item(span = { GridItemSpan(3) }) {
                AppText(
                    text = "搜索封面",
                    style = LegadoTheme.typography.titleSmallEmphasized,
                )
            }
            items(items, key = { it.bookUrl + it.originName }) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            onSelect(item.coverUrl.orEmpty())
                        }
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CoilBookCover(
                        name = item.name,
                        author = item.author,
                        path = item.coverUrl,
                        sourceOrigin = item.origin,
                        modifier = Modifier
                            .width(112.dp)
                            .aspectRatio(5f / 7f),
                    )
                    AppText(
                        text = item.originName,
                        style = LegadoTheme.typography.labelSmallEmphasized,
                        maxLines = 2
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ChangeSourceSheet(
    data: Book?,
    onDismissRequest: () -> Unit,
    onReplace: (BookSource, Book, List<BookChapter>, ChangeSourceMigrationOptions) -> Unit,
    onAddAsNew: (Book, List<BookChapter>) -> Unit,
) {
    var cachedData by remember { mutableStateOf(data) }

    if (data != null) {
        cachedData = data
    }

    val currentData = cachedData
    if (currentData != null) {
        ChangeSourceSheet(
            show = data != null,
            oldBook = currentData,
            onDismissRequest = onDismissRequest,
            onReplace = onReplace,
            onAddAsNew = onAddAsNew,
        )
    }
}
