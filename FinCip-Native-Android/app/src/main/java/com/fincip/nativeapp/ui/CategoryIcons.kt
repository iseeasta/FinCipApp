package com.fincip.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One icon + pastel tint per category, used by the form and every transaction list so they always agree. */
fun categoryIcon(category: String): ImageVector = when (category.trim().lowercase()) {
    "food" -> Icons.Default.Restaurant
    "transport" -> Icons.Default.DirectionsCar
    "shopping" -> Icons.Default.ShoppingBag
    "bills" -> Icons.Default.Receipt
    "health" -> Icons.Default.LocalHospital
    "education" -> Icons.Default.School
    "entertainment" -> Icons.Default.Movie
    "travel" -> Icons.Default.Flight
    "home" -> Icons.Default.Home
    "subscriptions" -> Icons.Default.Subscriptions
    "personal care" -> Icons.Default.Spa
    "emi" -> Icons.Default.AccountBalance
    "salary" -> Icons.Default.Payments
    "freelance" -> Icons.Default.Laptop
    "business" -> Icons.Default.Business
    "investment" -> Icons.Default.TrendingUp
    "gift" -> Icons.Default.CardGiftcard
    "refund" -> Icons.Default.Replay
    "transfer" -> Icons.Default.SwapHoriz
    else -> Icons.Default.MoreHoriz
}

fun categoryTint(category: String): Color = when (category.trim().lowercase()) {
    "food" -> Color(0xFFFFD9B8); "transport" -> Color(0xFFCFE3FF); "shopping" -> Color(0xFFF5D0E6); "bills" -> Color(0xFFFFF0B3)
    "health" -> Color(0xFFFFCFCF); "education" -> Color(0xFFD9D0F5); "entertainment" -> Color(0xFFE6CFFF); "travel" -> Color(0xFFC9F0F0)
    "home" -> Color(0xFFE6DCC8); "subscriptions" -> Color(0xFFD3DDFF); "personal care" -> Color(0xFFFFD6E0); "emi" -> Color(0xFFD9E2D0)
    "salary" -> Color(0xFFCDEFC0); "freelance" -> Color(0xFFC8EDDC); "business" -> Color(0xFFD7E8C0); "investment" -> Color(0xFFBFEBD0)
    "gift" -> Color(0xFFFFD0E8); "refund" -> Color(0xFFD5F0D0); "transfer" -> Color(0xFFDDE3EA)
    else -> Color(0xFFE8E4D8)
}

/** Rounded-square category badge. Dark ink on a pastel tint reads in light and dark themes alike. */
@Composable
fun CategoryBadge(category: String, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * .32f)).background(categoryTint(category)), contentAlignment = Alignment.Center) {
        Icon(categoryIcon(category), contentDescription = category, tint = Pop.Ink, modifier = Modifier.size(size * .52f))
    }
}

/** Short direction label shown small above the amount: ↗ Out, ↙ In, ⇄ Transfer, ↙ Refund. */
fun directionLabel(type: String): String = when (type) { "income" -> "↙ IN"; "refund" -> "↙ REFUND"; "transfer" -> "⇄ TRANSFER"; else -> "↗ OUT" }
