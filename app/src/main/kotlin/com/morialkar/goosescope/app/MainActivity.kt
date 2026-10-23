package com.morialkar.goosescope.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morialkar.goosescope.R
import com.morialkar.goosescope.app.ui.theme.Tokens

// Écran provisoire de T01: prouve que l'app compile, s'installe et applique les jetons.
// Remplacé par le mode direct en T10.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize().background(Tokens.surface).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BasicText(
                    text = stringResource(R.string.app_name),
                    style = TextStyle(color = Tokens.textOnSurface, fontSize = 40.sp),
                )
                BasicText(
                    text = stringResource(R.string.placeholder_tagline),
                    style = TextStyle(color = Tokens.textOnSurface, fontSize = 20.sp),
                )
            }
        }
    }
}
