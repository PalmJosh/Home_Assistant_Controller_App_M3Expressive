package com.josh.hacontroller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.toPath

// This wrapper is required because RoundedPolygon is not a Compose Shape by default
class RoundedPolygonShape(
    private val polygon: RoundedPolygon
) : Shape {
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = polygon.toPath().asComposePath()

        // Scale the polygon to fit the container size
        matrix.reset()
        // Determine the bounds of the polygon (it's usually normalized -1..1 or 0.1 depending on creation)
        // The .star() creates a normalized polygon. We scale it to the View Size.
        // Usually graphics-shapes polygons are defined in a normalized space,
        // but toPath() creates a path based on those coordinates.
        // We need to scale the path to fill the Size.

        // A simpler approach for the standard normalized polygon:
        val bounds = path.getBounds()
        val scaleX = size.width / bounds.width
        val scaleY = size.height / bounds.height

        matrix.scale(scaleX, scaleY)
        matrix.translate(-bounds.left, -bounds.top)

        path.transform(matrix)
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var url by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    // FIX: Explicitly type <Shape> to solve "Cannot infer type parameter"
    val sunburstShape = remember<Shape> {
        val polygon = RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.7f,
            rounding = CornerRounding(0.1f),
            innerRounding = CornerRounding(0.1f)
        )
        // Use the helper class defined below/above
        RoundedPolygonShape(polygon)
    }

    Scaffold { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(120.dp)
                        .clip(sunburstShape) // Uses the sunburst
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        null,
                        Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.height(40.dp))
                Text(
                    stringResource(R.string.login_welcome_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.login_welcome_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(48.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.login_url_label)) },
                    placeholder = { Text(stringResource(R.string.login_url_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.startLoginFlow(url) },
                    enabled = url.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        stringResource(R.string.login_connect_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (authState is MainViewModel.AuthState.Error) {
                    Spacer(Modifier.height(24.dp))
                    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            (authState as MainViewModel.AuthState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}