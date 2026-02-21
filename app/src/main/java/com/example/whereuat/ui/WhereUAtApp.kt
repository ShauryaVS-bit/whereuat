package com.example.whereuat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whereuat.model.FriendNode
import com.example.whereuat.viewmodel.AppAction
import com.example.whereuat.viewmodel.AppUiState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WhereUAtApp(state: AppUiState, onAction: (AppAction) -> Unit) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F7))
                .padding(16.dp)
        ) {
            when {
                state.loading -> Text("Starting up…", modifier = Modifier.align(Alignment.Center))
                state.selectedTarget != null -> GuidanceScreen(state = state, onBack = { onAction(AppAction.BackToRadar) })
                else -> RadarScreen(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun RadarScreen(state: AppUiState, onAction: (AppAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SessionControls(state = state, onAction = onAction)
        Card(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                RadarCanvas(state.nodes) { node -> onAction(AppAction.SelectTarget(node.id)) }
            }
        }
        if (state.groups.isNotEmpty()) {
            Text("Auto groups", fontWeight = FontWeight.SemiBold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.groups) { group ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(group.name, fontWeight = FontWeight.Bold)
                            Text(group.members.joinToString { it.displayName })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionControls(state: AppUiState, onAction: (AppAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.displayName,
            onValueChange = { onAction(AppAction.UpdateDisplayName(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Display name") }
        )
        OutlinedTextField(
            value = state.sessionCodeInput,
            onValueChange = { onAction(AppAction.UpdateSessionCode(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Session code") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAction(AppAction.CreateSession("Festival Squad")) }) { Text("Create") }
            Button(onClick = { onAction(AppAction.JoinSession) }) { Text("Join") }
        }
        state.activeSession?.let {
            Text("In session: ${it.name} (${it.id})")
        }
    }
}

@Composable
private fun RadarCanvas(nodes: List<FriendNode>, onClick: (FriendNode) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension * 0.42f
            drawCircle(color = Color(0x22000000), radius = maxRadius, center = center, style = Stroke(width = 4f))
            drawCircle(color = Color(0x22000000), radius = maxRadius * 0.66f, center = center, style = Stroke(width = 2f))
            drawCircle(color = Color(0x22000000), radius = maxRadius * 0.33f, center = center, style = Stroke(width = 2f))
        }

        nodes.forEach { node ->
            val angle = Math.toRadians(node.angleDeg.toDouble())
            val distance = node.radius * 140f
            val x = sin(angle).toFloat() * distance
            val y = -cos(angle).toFloat() * distance
            Column(
                modifier = Modifier
                    .padding(start = (x + 150).dp, top = (y + 150).dp)
                    .clickable { onClick(node) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(18.dp).background(Color(0xFF4A6CF7), CircleShape))
                Text(node.displayName)
            }
        }
    }
}

@Composable
private fun GuidanceScreen(state: AppUiState, onBack: () -> Unit) {
    val target = state.selectedTarget ?: return
    val relative = ((target.angleDeg - state.headingDeg) + 360f) % 360f
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(target.displayName, style = MaterialTheme.typography.headlineSmall)
        Text("Turn ${relative.toInt()}°", style = MaterialTheme.typography.displayMedium)
        Text(target.distanceLabel, style = MaterialTheme.typography.titleLarge)
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("Back") }
    }
}
