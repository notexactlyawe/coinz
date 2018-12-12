package com.cameronmacleod.coinz

import android.location.Location
import java.text.SimpleDateFormat
import java.util.*

/**
 * Returns the distance between a coin and the user
 */
fun distanceToCoin(coin: Coin, user: Location?): Float {
    if (user == null) return 0f

    return user.distanceTo(coin.getLocation())
}

/**
 * Gets the direction to a coin as a string
 *
 * Returns based on segments around each of 8 cardinal directions
 */
fun getDirectionToCoin(coin: Coin, user: Location?): String {
    if (user == null) return "N"

    val bearingDegrees = user.bearingTo(coin.getLocation())

    return when {
        bearingDegrees < 22.5  -> "N"
        bearingDegrees < 67.5  -> "NE"
        bearingDegrees < 112.5 -> "E"
        bearingDegrees < 157.5 -> "SE"
        bearingDegrees < 202.5 -> "S"
        bearingDegrees < 247.5 -> "SW"
        bearingDegrees < 292.5 -> "W"
        bearingDegrees < 337.5 -> "NW"
        else -> "N"
    }
}

/**
 * Formats a date in the way that it's stored in Firebase
 */
fun formatDate(date: Date): String {
    return SimpleDateFormat("yyyy.MM.dd", Locale.UK).format(date)
}