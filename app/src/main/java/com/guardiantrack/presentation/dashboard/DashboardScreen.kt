package com.guardiantrack.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.location.TrackingState
import com.guardiantrack.presentation.theme.AccentAmber
import com.guardiantrack.presentation.theme.AccentRed
import com.guardiantrack.presentation.theme.PrimaryGreen
import com.guardiantrack.presentation.theme.SecondaryTeal
import java.util.Locale

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Status & Hero Action Header
        HeroControlCard(
            trackingState = state.trackingState,
            onToggle = { onEvent(DashboardUiEvent.ToggleTrackingClicked) }
        )

        // 2. Battery & Accuracy KPI Row (<2%/hr Invariant Display)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiMetricCard(
                title = "Battery Drain",
                value = String.format(Locale.US, "%.1f%%/hr", state.batteryDrainPerHour),
                subtext = if (state.batteryDrainPerHour <= 2.0f) "Target Met (<2%/hr)" else "Exceeds Target",
                statusColor = if (state.batteryDrainPerHour <= 2.0f) PrimaryGreen else AccentAmber,
                icon = { Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = PrimaryGreen) },
                modifier = Modifier.weight(1f)
            )

            KpiMetricCard(
                title = "Precision",
                value = String.format(Locale.US, "±%.1fm", state.horizontalAccuracyMeters),
                subtext = state.activeProvider.name,
                statusColor = if (state.horizontalAccuracyMeters < 15f) PrimaryGreen else AccentAmber,
                icon = { Icon(Icons.Default.GpsFixed, contentDescription = null, tint = SecondaryTeal) },
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Offline Buffer & Cloud Sync Card
        SyncStatusCard(
            pendingCount = state.pendingSyncCount,
            totalCount = state.totalRecordedCount,
            onSync = { onEvent(DashboardUiEvent.ForceSyncClicked) }
        )

        // 4. Sampling Profile Selector
        SamplingProfileSelector(
            currentProfile = state.selectedProfile,
            onProfileSelected = { onEvent(DashboardUiEvent.ChangeSamplingProfile(it)) }
        )

        // 5. Breadcrumb Polyline Visualizer Canvas
        BreadcrumbCanvasCard(
            breadcrumbs = state.recentBreadcrumbs
        )
    }
}

@Composable
private fun HeroControlCard(
    trackingState: TrackingState,
    onToggle: () -> Unit
) {
    val isTracking = trackingState != TrackingState.STOPPED

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isTracking) PrimaryGreen else Color.Gray,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTracking) "ENGINE ACTIVE" else "ENGINE STOPPED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isTracking) PrimaryGreen else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isTracking) "Continuous Background GPS" else "Tap to activate tracking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) AccentRed else PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = if (isTracking) "Stop" else "Start")
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtext: String,
    statusColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                icon()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtext, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SyncStatusCard(
    pendingCount: Int,
    totalCount: Int,
    onSync: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Offline Queue Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$pendingCount pending sync • $totalCount total saved",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onSync,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Sync Now")
            }
        }
    }
}

@Composable
private fun SamplingProfileSelector(
    currentProfile: SamplingProfile,
    onProfileSelected: (SamplingProfile) -> Unit
) {
    Column {
        Text(
            text = "Sampling Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SamplingProfile.entries.forEach { profile ->
                val label = when (profile) {
                    SamplingProfile.STANDARD -> "Standard (10s)"
                    SamplingProfile.ECO -> "Eco (60s)"
                    SamplingProfile.STATIONARY_ADAPTIVE -> "Adaptive"
                }

                FilterChip(
                    selected = currentProfile == profile,
                    onClick = { onProfileSelected(profile) },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbCanvasCard(
    breadcrumbs: List<com.guardiantrack.core.model.location.LocationPoint>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Breadcrumb Polyline (${breadcrumbs.size} points)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (breadcrumbs.size < 2) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Collecting coordinates to draw trajectory...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val bounds = remember(breadcrumbs) {
                    val minLat = breadcrumbs.minOf { it.latitude }
                    val maxLat = breadcrumbs.maxOf { it.latitude }
                    val minLng = breadcrumbs.minOf { it.longitude }
                    val maxLng = breadcrumbs.maxOf { it.longitude }
                    val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
                    val lngSpan = (maxLng - minLng).coerceAtLeast(0.0001)
                    Triple(minLat to latSpan, minLng to lngSpan, breadcrumbs.last())
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val (latData, lngData, latest) = bounds
                    val (minLat, latSpan) = latData
                    val (minLng, lngSpan) = lngData

                    val path = Path()
                    breadcrumbs.forEachIndexed { index, point ->
                        val x = ((point.longitude - minLng) / lngSpan * (size.width - 40)).toFloat() + 20f
                        val y = (size.height - ((point.latitude - minLat) / latSpan * (size.height - 40))).toFloat() - 20f

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = PrimaryGreen,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Draw latest point dot
                    val lastX = ((latest.longitude - minLng) / lngSpan * (size.width - 40)).toFloat() + 20f
                    val lastY = (size.height - ((latest.latitude - minLat) / latSpan * (size.height - 40))).toFloat() - 20f
                    drawCircle(
                        color = SecondaryTeal,
                        radius = 6.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
            }
        }
    }
}
