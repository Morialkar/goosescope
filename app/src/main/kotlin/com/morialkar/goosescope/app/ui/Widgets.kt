package com.morialkar.goosescope.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morialkar.goosescope.app.ui.theme.Tokens

@Composable
fun FlatButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, background: Color = Tokens.surfaceRaised) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = Tokens.minTouch, minHeight = Tokens.minTouch)
            .background(background, RoundedCornerShape(Tokens.corner))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Une seule ligne: le libellé rétrécit un peu plutôt que de passer à la ligne (trois boutons sur un téléphone).
        BasicText(
            label,
            style = TextStyle(color = Tokens.textOnSurface, fontWeight = FontWeight.Bold),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = Tokens.labelSize),
        )
    }
}

@Composable
fun Label(text: String, size: TextUnit = Tokens.bodySize, color: Color = Tokens.textOnSurface, bold: Boolean = false, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, lineHeight = size * 1.3f),
    )
}
