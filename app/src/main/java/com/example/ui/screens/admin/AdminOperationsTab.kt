package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AdminOperationsTab(
    repository: NexusRepository,
    modifier: Modifier = Modifier
) {
    val complaints by repository.complaints.collectAsState()
    val requests by repository.requests.collectAsState()
    val chats by repository.chatMessages.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    var opsTab by remember { mutableIntStateOf(0) } // 0: Complaints, 1: Employee Requests, 2: Announcements & Team Chat

    // Broadcast message state
    var broadcastContent by remember { mutableStateOf("") }

    val pendingRequests = requests.filter { it.status == RequestStatus.PENDING_REVIEW }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_operations_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            TabRow(
                selectedTabIndex = opsTab,
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ElectricBlue
            ) {
                Tab(
                    selected = opsTab == 0,
                    onClick = { opsTab = 0 },
                    text = { Text("Complaints (${complaints.count { it.status != ComplaintStatus.CLOSED }})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = opsTab == 1,
                    onClick = { opsTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Requests", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            if (pendingRequests.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = GoldenAmber
                                ) {
                                    Text(
                                        text = "${pendingRequests.size}",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = opsTab == 2,
                    onClick = { opsTab = 2 },
                    text = { Text("Announcements", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }

        when (opsTab) {
            0 -> {
                // Complaints Section
                item {
                    Text(
                        text = "Customer Service & Fiber Fault Tickets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                items(complaints, key = { it.id }) { tkt ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PriorityBadge(
                                        priorityName = tkt.priority.name,
                                        color = if (tkt.priority == ComplaintPriority.CRITICAL) CoralRose else GoldenAmber
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = tkt.issueTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                }
                                StatusBadge(
                                    text = tkt.status.name,
                                    color = when (tkt.status) {
                                        ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED -> MintEmerald
                                        ComplaintStatus.IN_PROGRESS -> ElectricBlue
                                        ComplaintStatus.ASSIGNED -> RoyalPurple
                                        ComplaintStatus.NEW -> CoralRose
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Customer: ${tkt.customerName} (${tkt.customerId}) • ${tkt.customerPhone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Address: ${tkt.address}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tkt.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tech: ${tkt.assignedTechnicianName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                if (tkt.status != ComplaintStatus.RESOLVED && tkt.status != ComplaintStatus.CLOSED) {
                                    Button(
                                        onClick = {
                                            repository.updateComplaintStatus(tkt.id, ComplaintStatus.RESOLVED, "Fault resolved by Super Admin")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text("Mark Resolved", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Employee Requests Section
                item {
                    Text(
                        text = "Employee Administrative Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                items(requests, key = { it.id }) { req ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = req.employeeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = req.type.name.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                StatusBadge(
                                    text = req.status.name.replace("_", " "),
                                    color = when (req.status) {
                                        RequestStatus.APPROVED -> MintEmerald
                                        RequestStatus.PENDING_REVIEW -> GoldenAmber
                                        RequestStatus.REJECTED -> CoralRose
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = req.requestTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = req.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (req.adminDecisionNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MintEmerald.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, MintEmerald.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                ) {
                                    Text(
                                        text = "Admin Remark: ${req.adminDecisionNotes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MintEmerald,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }

                            if (req.status == RequestStatus.PENDING_REVIEW) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { repository.reviewRequest(req.id, false, "Declined due to ongoing field emergencies") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRose),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { repository.reviewRequest(req.id, true, "Approved by Super Admin") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Announcements & Team Chat
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(16.dp))
                            .border(1.5.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Broadcast Official Announcement",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = broadcastContent,
                                onValueChange = { broadcastContent = it },
                                label = { Text("Write announcement for all employees...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (broadcastContent.isNotBlank() && currentUser != null) {
                                        repository.sendChatMessage(
                                            sender = currentUser!!,
                                            content = broadcastContent.trim(),
                                            channel = "ANNOUNCEMENT",
                                            isBroadcast = true
                                        )
                                        broadcastContent = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish to Staff", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Recent Team Communications & Bulletins",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                items(chats.reversed(), key = { it.id }) { msg ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isBroadcast)
                                LightBlueTint
                            else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(14.dp))
                            .border(
                                width = if (msg.isBroadcast) 1.5.dp else 1.dp,
                                color = if (msg.isBroadcast) ElectricBlue.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = msg.senderName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (msg.isBroadcast) ElectricBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (msg.isBroadcast) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StatusBadge(text = "📢 OFFICIAL BROADCAST", color = ElectricBlue)
                                    }
                                }
                                Text(
                                    text = NexusRepository.formatKolkataTime(msg.timestampMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
