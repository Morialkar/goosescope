// REQUIRES_REVIEW: écran de permission caméra (SPEC F7, ARCHITECTURE §6).
package com.morialkar.goosescope.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.morialkar.goosescope.R
import com.morialkar.goosescope.app.ui.theme.Tokens

@Composable
fun MessageScreen(
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>?,
    secondary: Pair<String, () -> Unit>? = null,
    tag: String,
) {
    Column(
        Modifier.fillMaxSize().background(Tokens.surface).safeDrawingPadding().padding(24.dp).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Label(title, Tokens.shadeNameSize, bold = true)
            Label(body)
            primary?.let { (label, action) -> FlatButton(label, action, Modifier.testTag("$tag.primary")) }
            secondary?.let { (label, action) -> FlatButton(label, action, Modifier.testTag("$tag.secondary"), background = Tokens.surface) }
        }
    }
}

@Composable
fun PermissionScreen(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    MessageScreen(
        title = stringResource(R.string.permission_title),
        body = stringResource(R.string.permission_body),
        primary = stringResource(R.string.permission_allow) to onAllow,
        secondary = stringResource(R.string.permission_settings) to onOpenSettings,
        tag = "permission",
    )
}
