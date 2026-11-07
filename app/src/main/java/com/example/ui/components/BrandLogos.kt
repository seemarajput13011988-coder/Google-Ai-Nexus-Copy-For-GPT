package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryCategory
import com.example.data.model.PaymentMode

/**
 * Proper Brand Logo for ISP Providers (BSNL, RailWire, Air Fibre, Tikona, One Broadband)
 */
@Composable
fun IspProviderLogo(
    provider: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val norm = provider.lowercase()
    when {
        norm.contains("bsnl") -> {
            // Official BSNL Blue and Orange Rings
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF003882),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        val w = this.size.width
                        val h = this.size.height
                        // Orange swoosh
                        drawArc(
                            color = Color(0xFFF37021),
                            startAngle = 30f,
                            sweepAngle = 120f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawArc(
                            color = Color.White,
                            startAngle = 210f,
                            sweepAngle = 120f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    Text(
                        text = "BSNL",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.28).sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        norm.contains("rail") -> {
            // Official RailWire / RailTel Indian Railways Theme (Green & Orange)
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF006838),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(size * 0.35f)
                                .clip(CircleShape)
                                .background(Color(0xFFF37021)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(size * 0.28f)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "RAIL",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = (size.value * 0.24).sp
                        )
                    }
                }
            }
        }
        norm.contains("air") -> {
            // Air Fibre Wireless Tower & High-Speed Radio Waves
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF0284C7),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Air Fibre",
                        tint = Color(0xFF67E8F9),
                        modifier = Modifier.size(size * 0.65f)
                    )
                }
            }
        }
        norm.contains("tikona") -> {
            // Tikona Triangular Dynamic Delta
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFFDC2626),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                        val path = Path().apply {
                            moveTo(this@Canvas.size.width / 2f, 2f)
                            lineTo(this@Canvas.size.width - 2f, this@Canvas.size.height - 2f)
                            lineTo(2f, this@Canvas.size.height - 2f)
                            close()
                        }
                        drawPath(path, color = Color.White, style = Fill)
                    }
                    Text(
                        text = "T",
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.45).sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        norm.contains("one") -> {
            // One Broadband Optical Ring
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF7C3AED),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "1NET",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.26).sp
                    )
                }
            }
        }
        else -> {
            // Generic High-Speed Fiber Logo
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF0284C7),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Proper Brand Logo for Payment Modes (UPI, Cash, Bank Transfer, Cheque)
 */
@Composable
fun PaymentModeLogo(
    mode: PaymentMode,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    when (mode) {
        PaymentMode.UPI -> {
            // Official Style UPI Badge (Green & Orange arrow)
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF00897B),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "UPI",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = (size.value * 0.32).sp,
                            letterSpacing = 0.5.sp
                        )
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(size * 0.4f)
                        )
                    }
                }
            }
        }
        PaymentMode.CASH -> {
            // Vibrant Emerald Cash Rupee Note
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF10B981),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = (size.value * 0.52).sp
                        )
                    }
                }
            }
        }
        PaymentMode.BANK_TRANSFER -> {
            // Institutional Bank Pillars (NEFT / IMPS)
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF2563EB),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Bank Transfer",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.6f)
                    )
                }
            }
        }
        PaymentMode.CHEQUE -> {
            // Cheque Voucher
            Surface(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFFF59E0B),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Cheque",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Proper Brand Logo for Inventory & Telecom Equipment
 */
@Composable
fun EquipmentCategoryLogo(
    category: InventoryCategory,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val (bgColor, iconVector) = when (category) {
        InventoryCategory.ROUTER_WIFI6 -> Pair(Color(0xFF0284C7), Icons.Default.Router)
        InventoryCategory.ONU_ONT_DEVICE -> Pair(Color(0xFF8B5CF6), Icons.Default.Memory)
        InventoryCategory.FIBER_OPTIC_CABLE -> Pair(Color(0xFFF97316), Icons.Default.Cable)
        InventoryCategory.PATCH_CORD_CONNECTOR -> Pair(Color(0xFF06B6D4), Icons.Default.Power)
        InventoryCategory.SPLICING_TOOL_EQUIPMENT -> Pair(Color(0xFFEC4899), Icons.Default.Hardware)
    }

    Surface(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp)),
        color = bgColor,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = iconVector,
                contentDescription = category.name,
                tint = Color.White,
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}
