package com.example.whereuat.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class AppUser(
    val id: String,
    val displayName: String
)

data class EventSession(
    val id: String,
    val name: String,
    val members: List<AppUser> = emptyList()
)

data class FriendSignal(
    val friendId: String,
    val displayName: String,
    val rssi: Int,
    val azimuthDeg: Float,
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class FriendNode(
    val id: String,
    val displayName: String,
    val radius: Float,
    val angleDeg: Float,
    val distanceLabel: String,
    val score: Float
)

data class FriendGroup(
    val id: String,
    val name: String,
    val members: List<FriendNode>,
    val centerRadius: Float,
    val centerAngle: Float
)

enum class ScanProfile {
    FAST,
    BALANCED,
    BATTERY_SAVER
}

data class RadarProjection(
    val radius: Float,
    val angle: Float,
    val distanceScore: Float
)

fun FriendSignal.toProjection(txPower: Int = -59): RadarProjection {
    val meters = estimateDistanceMeters(rssi = rssi, txPower = txPower)
    val score = min(1f, max(0.1f, (1f / (meters / 2f + 1f)).toFloat()))
    val radius = 1f - score
    return RadarProjection(radius = radius, angle = azimuthDeg, distanceScore = score)
}

fun estimateDistanceMeters(rssi: Int, txPower: Int): Double {
    val ratio = (txPower - rssi).toDouble() / 20.0
    return 10.0.pow(ratio)
}

fun distanceLabel(distanceMeters: Double): String = when {
    distanceMeters < 2 -> "Very close"
    distanceMeters < 6 -> "Near"
    distanceMeters < 12 -> "Medium"
    else -> "Far"
}
