package io.legado.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.browser.engine.BrowserClearCategory
import io.legado.app.browser.engine.BrowserClearDataHelper
import io.legado.app.browser.engine.BrowserClearDataPrefs
import io.legado.app.browser.engine.BrowserClearTimeRange
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.topbar.GlassSmallTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.launch

@Composable
fun BrowserClearDataScreen(onBack: () -> Unit) {
    BrowserTheme {
        BrowserClearDataBody(onBack = onBack)
    }
}

@Composable
private fun BrowserClearDataBody(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { BrowserClearDataPrefs(context) }
    val scope = rememberCoroutineScope()
    val scheme = LegadoTheme.colorScheme

    var clearOnExit by remember { mutableStateOf(prefs.clearOnExit) }
    var range by remember { mutableStateOf(prefs.timeRange) }
    var rangeMenu by remember { mutableStateOf(false) }
    var selected by remember {
        mutableStateOf(BrowserClearCategory.entries.associateWith { prefs.isSelected(it) })
    }
    var busy by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }

    fun summary(cat: BrowserClearCategory): String {
        tick
        return when (cat) {
            BrowserClearCategory.History -> BrowserClearDataHelper.summaryHistory(context, range)
            BrowserClearCategory.Cookies -> BrowserClearDataHelper.summaryCookies()
            BrowserClearCategory.Cache -> BrowserClearDataHelper.summaryCache(context)
            BrowserClearCategory.Tabs -> BrowserClearDataHelper.summaryTabs()
            BrowserClearCategory.Passwords -> BrowserClearDataHelper.summaryPasswords(context, range)
            BrowserClearCategory.Autofill -> BrowserClearDataHelper.summaryAutofill(context, range)
            BrowserClearCategory.SiteSettings ->
                BrowserClearDataHelper.summarySiteSettings(context, range)
        }
    }

    AppScaffold(
        topBar = {
            GlassSmallTopAppBar(
                title = "删除浏览数据",
                navigationIcon = { TopBarNavigationButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = BrowserUiTokens.PagePaddingH,
                            vertical = BrowserUiTokens.SpaceMd,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                        "在退出时清除浏览数据",
                            fontSize = 16.sp,
                            color = scheme.onSurface,
                        )
                        Text(
                            if (clearOnExit) "开" else "关",
                            fontSize = 13.sp,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = clearOnExit,
                        onCheckedChange = {
                            clearOnExit = it
                            prefs.clearOnExit = it
                        },
                    )
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { rangeMenu = true }
                        .padding(
                            horizontal = BrowserUiTokens.PagePaddingH,
                            vertical = BrowserUiTokens.SpaceLg,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "时间范围",
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        color = scheme.onSurface,
                    )
                    Text(range.label, fontSize = 15.sp, color = scheme.onSurfaceVariant)
                    SymbolIcon(
                        Symbols.KeyboardArrowDown,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                    )
                    DropdownMenu(expanded = rangeMenu, onDismissRequest = { rangeMenu = false }) {
                        BrowserClearTimeRange.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    range = option
                                    prefs.timeRange = option
                                    rangeMenu = false
                                    tick++
                                },
                            )
                        }
                    }
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                BrowserClearCategory.entries.forEach { cat ->
                    ClearCategoryRow(
                        icon = categoryIcon(cat),
                        title = categoryTitle(cat),
                        summary = summary(cat),
                        checked = selected[cat] == true,
                        onCheckedChange = {
                            selected = selected + (cat to it)
                            prefs.setSelected(cat, it)
                        },
                    )
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BrowserUiTokens.PagePaddingH,
                        vertical = BrowserUiTokens.SpaceMd,
                    ),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        val cats = selected.filterValues { it }.keys
                        if (cats.isEmpty()) {
                            context.toastOnUi("请至少选择一项")
                            return@Button
                        }
                        busy = true
                        scope.launch {
                            try {
                                BrowserClearDataHelper.clear(context, cats, range)
                                tick++
                                context.toastOnUi("已删除所选数据")
                            } catch (_: Throwable) {
                                context.toastOnUi("删除失败")
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("删除数据", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ClearCategoryRow(
    icon: MaterialSymbol,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(
                horizontal = BrowserUiTokens.PagePaddingH,
                vertical = BrowserUiTokens.SpaceMd,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = scheme.onSurface)
            Text(summary, fontSize = 13.sp, color = scheme.onSurfaceVariant)
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun categoryTitle(cat: BrowserClearCategory): String = when (cat) {
    BrowserClearCategory.History -> "浏览历史记录"
    BrowserClearCategory.Cookies -> "Cookie 和站点数据"
    BrowserClearCategory.Cache -> "缓存的图像和文件"
    BrowserClearCategory.Tabs -> "标签页"
    BrowserClearCategory.Passwords -> "保存的密码"
    BrowserClearCategory.Autofill -> "自动填充表单数据"
    BrowserClearCategory.SiteSettings -> "网站设置"
}

private fun categoryIcon(cat: BrowserClearCategory): MaterialSymbol = when (cat) {
    BrowserClearCategory.History -> Symbols.Schedule
    BrowserClearCategory.Cookies -> Symbols.Language
    BrowserClearCategory.Cache -> Symbols.Image
    BrowserClearCategory.Tabs -> Symbols.Web
    BrowserClearCategory.Passwords -> Symbols.SettingsBackupRestore
    BrowserClearCategory.Autofill -> Symbols.Edit
    BrowserClearCategory.SiteSettings -> Symbols.Settings
}
