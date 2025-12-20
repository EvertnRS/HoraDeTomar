package br.upe.horaDeTomar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.upe.horaDeTomar.R
import br.upe.horaDeTomar.ui.themes.*

/* ====== Campo de texto estilo “cartão” ====== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None, // <- campo para adicionar máscaras
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = md_theme_light_onSurface
        )
        Spacer(Modifier.height(6.dp))

        val borderColor = when {
            isError -> MaterialTheme.colorScheme.error
            focused -> button_green_primary
            else    -> md_theme_light_outline
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = white,
            tonalElevation = if (focused) 1.dp else 0.dp,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(button_green_primary),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = text_balck),
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = capitalization,
                    keyboardType = keyboardType
                ),
                visualTransformation = visualTransformation,
                interactionSource = interaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    TextFieldDefaults.DecorationBox(
                        value = value,
                        innerTextField = inner,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interaction,
                        placeholder = { Text(placeholder, color = gray_dark) },
                        container = {},
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            )
        }

        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/* ====== Select estilo “cartão” com bottom sheet ====== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSelectField(
    label: String,
    value: String?,
    onValueChange: (String) -> Unit,
    placeholder: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = md_theme_light_onSurface
        )
        Spacer(Modifier.height(6.dp))

        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        val borderColor = when {
            isError -> MaterialTheme.colorScheme.error
            focused || showSheet -> button_green_primary
            else -> md_theme_light_outline
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = white,
            tonalElevation = if (focused || showSheet) 1.dp else 0.dp,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showSheet = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value.isNullOrBlank()) gray_dark else text_balck
                )
                Icon(
                    painter = painterResource(
                        id = if (showSheet) R.drawable.chevron_up else R.drawable.chevron_down
                    ),
                    contentDescription = null,
                    tint = button_green_primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            options.forEach { opt ->
                ListItem(
                    headlineContent = { Text(opt) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onValueChange(opt)
                            showSheet = false
                        }
                        .padding(horizontal = 16.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun CardActionField(
    label: String,
    valueText: String?,
    onClick: () -> Unit,
    placeholder: String = "Selecionar",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = md_theme_light_onSurface
        )
        Spacer(Modifier.height(6.dp))

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            color = white,
            border = BorderStroke(1.dp, md_theme_light_outline),
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = valueText?.takeIf { it.isNotBlank() } ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (valueText.isNullOrBlank()) gray_dark else text_balck
                )
                Icon(
                    painter = painterResource(id = R.drawable.chevron_right),
                    contentDescription = null,
                    tint = button_green_primary,
                    modifier = Modifier.size(22.dp)
                )
            }

        }
    }
}

