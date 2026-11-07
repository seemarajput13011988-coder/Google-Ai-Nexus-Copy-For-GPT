package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmployeeLocation
import com.example.data.repository.NexusRepository
import com.example.ui.theme.*

@Composable
fun GpsRadarMap(
    locations: List<EmployeeLocation>,
    selectedEmployeeId: String?,
    onSelectEmployee: (EmployeeLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf("ALL") }

    val filteredLocations = remember(locations, activeFilter) {
        when (activeFilter) {
            "LIVE" -> locations.filter { it.isLive }
            "LAST_KNOWN" -> locations.filter { !it.isLive }
            else -> locations
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val radarBg = if (isDark) Color(0xFF0F172A) else Color(0xFFE0F2FE)
    val radarGridColor = if (isDark) Color(0xFF1E2E4A) else Color(0xFFBAE6FD)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .testTag("gps_radar_map_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MintEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live GPS Fleet Radar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = activeFilter == "ALL",
                        onClick = { activeFilter = "ALL" },
                        label = { Text("All (${locations.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = activeFilter == "LIVE",
                        onClick = { activeFilter = "LIVE" },
                        label = { Text("Live (${locations.count { it.isLive }})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Radar Canvas Visualization (Bright Cyan Grid in Light Mode, Glowing Navy in Dark Mode)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(radarBg)
                    .border(1.dp, radarGridColor, RoundedCornerShape(14.dp))
                    .testTag("gps_radar_canvas_container")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                    // Concentric radar ranges (1km, 3km, 5km)
                    drawCircle(color = radarGridColor, radius = h * 0.22f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f))
                    drawCircle(color = radarGridColor, radius = h * 0.42f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f, pathEffect = dashedEffect))

                    // Crosshair axis
                    drawLine(color = radarGridColor, start = Offset(0f, center.y), end = Offset(w, center.y), strokeWidth = 1.2f)
                    drawLine(color = radarGridColor, start = Offset(center.x, 0f), end = Offset(center.x, h), strokeWidth = 1.2f)

                    // Field node hubs (Hub A, Hub B, Central OLT)
                    drawCircle(color = ElectricBlue.copy(alpha = 0.6f), radius = 5f, center = center)
                    drawCircle(color = VividCyan.copy(alpha = 0.6f), radius = 4f, center = Offset(w * 0.25f, h * 0.35f))
                    drawCircle(color = VividCyan.copy(alpha = 0.6f), radius = 4f, center = Offset(w * 0.75f, h * 0.65f))
                }

                // Overlay interactive technician markers
                filteredLocations.forEachIndexed { index, loc ->
                    val xFraction = when (index % 3) {
                        0 -> 0.35f + (index * 0.12f)
                        1 -> 0.65f - (index * 0.08f)
                        else -> 0.50f + (index * 0.15f)
                    }.coerceIn(0.15f, 0.85f)

                    val yFraction = when (index % 3) {
                        0 -> 0.40f + (index * 0.08f)
                        1 -> 0.28f + (index * 0.14f)
                        else -> 0.68f - (index * 0.09f)
                    }.coerceIn(0.15f, 0.85f)

                    val isSelected = loc.employeeId == selectedEmployeeId

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .offset(
                                    x = (xFraction * 260).dp,
                                    y = (yFraction * 140).dp
                                )
                                .clickable { onSelectEmployee(loc) }
                                .testTag("radar_marker_${loc.employeeId}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (loc.isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size((pulseRadius * 2).dp)
                                            .clip(CircleShape)
                                            .background(MintEmerald.copy(alpha = pulseAlpha))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 30.dp else 24.dp)
                                        .clip(CircleShape)
                                        .background(if (loc.isLive) MintEmerald else GoldenAmber)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.5.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (loc.isLive) Icons.Default.Navigation else Icons.Default.LocationOff,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isSelected) 18.dp else 14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isDark) DarkSurface else Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            ) {
                                Text(
                                    text = loc.employeeName.split(" ").first(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Legend at bottom of radar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(
                            if (isDark) DarkSurface.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.92f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(MintEmerald))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Live (Active Shift)", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(GoldenAmber))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Last Known (Offline)", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Focused employee telemetry card
            val focusedLoc = locations.firstOrNull { it.employeeId == selectedEmployeeId } ?: locations.firstOrNull()
            if (focusedLoc != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = focusedLoc.employeeName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (focusedLoc.isLive) {
                                    StatusBadge(text = "LIVE TRACKING", color = MintEmerald)
                                } else {
                                    StatusBadge(text = "LAST KNOWN", color = GoldenAmber)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Zone: ${focusedLoc.areaDescription}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Coordinates: ${String.format("%.4f", focusedLoc.latitude)}, ${String.format("%.4f", focusedLoc.longitude)} • Accuracy ±${focusedLoc.accuracyMeters.toInt()}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = MintEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${focusedLoc.batteryPercent}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = NexusRepository.formatKolkataTime(focusedLoc.timestampMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
