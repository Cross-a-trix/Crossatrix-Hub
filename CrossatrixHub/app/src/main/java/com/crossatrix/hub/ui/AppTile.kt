package com.crossatrix.hub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun RoundAppTile(app: HubApp, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
            shadowElevation = 4.dp,
            modifier = Modifier.size(96.dp),
        ) {
            Image(
                painter = painterResource(app.icon),
                contentDescription = app.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(3.dp)
                    .clip(CircleShape),
            )
        }
        Text(
            app.name,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
