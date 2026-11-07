package io.legado.app.ui.widget.components.dialog

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppTextField
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.text.AppText
import io.legado.app.utils.isHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    show: Boolean,
    initialColor: Int,
    onDismissRequest: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    var currentColor by remember { mutableStateOf(Color(initialColor)) }
    var hexInput by remember { mutableStateOf(initialColor.asHexColorString()) }
    var isHexInputError by remember { mutableStateOf(false) }

    LaunchedEffect(show, initialColor) {
        if (show) {
            currentColor = Color(initialColor)
            hexInput = initialColor.asHexColorString()
            isHexInputError = false
        }
    }

    val parsedHexColor = parseHexColor(hexInput)

    AppModalBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.select_color),
        startAction = {
            MediumTonalButton(
                onClick = {
                    currentColor = Color.Transparent
                    hexInput = "#00000000"
                    isHexInputError = false
                },
                icon = Symbols.Restore,
                contentDescription = stringResource(R.string.reset),
            )
        },
        endAction = {
            MediumTonalButton(
                onClick = {
                    onColorSelected(currentColor.toArgb())
                    onDismissRequest()
                },
                enabled = parsedHexColor != null && !isHexInputError,
                icon = Symbols.Save,
                contentDescription = stringResource(R.string.action_save),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ColorPalette(
                color = currentColor,
                onColorChanged = { color ->
                    currentColor = color
                    hexInput = color.toArgb().asHexColorString()
                    isHexInputError = false
                },
                rows = 8,
                hueColumns = 12,
                modifier = Modifier.fillMaxWidth(),
                showPreview = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(
                            1.dp,
                            LegadoTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(8.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                AppTextField(
                    value = hexInput,
                    onValueChange = { value ->
                        hexInput = normalizeHexInput(value)
                        val parsedColor = parseHexColor(hexInput)
                        if (parsedColor != null) {
                            currentColor = Color(parsedColor)
                            isHexInputError = false
                        } else {
                            isHexInputError = hexInput.isNotBlank()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.color_value),
                    singleLine = true,
                    isError = isHexInputError,
                    backgroundColor = LegadoTheme.colorScheme.surfaceContainerLow,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    )
                )
            }

        }
    }
}

private fun normalizeHexInput(input: String): String {
    val trimmed = input.trim().uppercase()
    return if (trimmed.startsWith("#")) {
        "#${trimmed.removePrefix("#")}"
    } else {
        trimmed
    }
}

private fun parseHexColor(input: String): Int? {
    val hex = input.trim().removePrefix("#")
    if (hex.length !in setOf(6, 8) || !hex.isHex()) return null
    val argb = if (hex.length == 6) "FF$hex" else hex
    return argb.toLong(16).toInt()
}

private fun Int.asHexColorString(): String =
    "#${Integer.toHexString(this).uppercase().padStart(8, '0')}"

@Composable
private fun ColorPalette(
    color: Color,
    onColorChanged: (Color) -> Unit,
    rows: Int,
    hueColumns: Int,
    modifier: Modifier = Modifier,
    showPreview: Boolean = false,
) {
    Column(modifier = modifier) {
        if (showPreview) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(color, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(hueColumns) { col ->
                    val hue = col / hueColumns.toFloat() * 360f
                    val value = 1f - row / (rows - 1).coerceAtLeast(1).toFloat() * 0.85f
                    val cell = Color.hsv(hue, 1f, value.coerceIn(0.15f, 1f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .background(cell)
                            .clickable { onColorChanged(cell) }
                    )
                }
            }
        }
    }
}
