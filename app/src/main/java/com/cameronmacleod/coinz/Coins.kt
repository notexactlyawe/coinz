package com.cameronmacleod.coinz

import android.location.Location
import android.util.Log
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import java.text.SimpleDateFormat
import java.util.*

// Convenience function to get the name of a Coins object
fun getCoinsName(userId: String, date: String): String {
    return "$userId:$date"
}

/**
 * Serializable (to Firestore) class that represents a coin on the map
 */
data class Coin(var coinId: String = "",
                var latitude: Double = 0.0,
                var longitude: Double = 0.0,
                var currency: String = "",
                var amount: Double = 0.0,
                var collected: Boolean = false,
                var banked: Boolean = false) {

    /**
     * Returns the location of the coin as a [Location]
     */
    fun getLocation(): Location {
        val loc = Location("geojson")
        loc.latitude = latitude
        loc.longitude = longitude
        return loc
    }

    /**
     * Convenience toString for debugging purposes
     */
    override fun toString(): String {
        return "$coinId, $latitude, $longitude, $currency, $amount, $collected"
    }

    /**
     * Mark a coin as collected. Check [isCollectable] before using.
     */
    fun collect() {
        collected = true
    }

    /**
     * Checks if a coin is collectable.
     *
     * Only true when the coin is not yet collected and the user is within 25 metres
     *
     * @param user The location of the user
     * @return Whether the coin is collectable
     */
    fun isCollectable(user: Location): Boolean {
        if (!collected && getLocation().distanceTo(user) < 25) {
            return true
        }
        return false
    }
}

/**
 * Serializable (to Firestore) class that represents a document in the usersToCoinz table
 */
data class Coins(var coins: List<Coin> = listOf(),
                 var userId: String = "",
                 var date: String = "") {
    val name = getCoinsName(userId, date)

    /**
     * Returns the number of collected coins within the [coins] list
     */
    fun getNumCollected(): Int {
        return coins.filter { it.collected }.size
    }

    /**
     * Convenience function to collect a contained coin that has a specific ID
     *
     * @param id The unique ID of the coin
     * @param userLocation The current location of the user
     * @return Whether or not the coin was successfully collected
     */
    fun collectCoinById(id: String, userLocation: Location): Boolean {
        try {
            val coin = coins.first { it.coinId == id }

            if (!coin.isCollectable(userLocation)) {
                Log.e(javaClass.simpleName, "Coin $id not collectable")
                return false
            }

            coin.collect()
        } catch (e: NoSuchElementException) {
            Log.e(javaClass.simpleName, "No coin found for ID $id")
            return false
        }
        return true
    }

    /**
     * Convenience toString for debugging
     */
    override fun toString(): String {
        return "${coins.size} coins. User: $userId, date: $date"
    }

    /**
     * A factory companion object containing methods to allow constructing of [Coins] objects
     *
     * Usage: Coins.fromJson(jsonString, userID, Calendar.getInstance().time)
     */
    companion object Factory {
        /**
         * Constructs a [Coins] object from a GeoJSON string
         *
         * @param json The GeoJSON string
         * @param userId The ID of the user to construct the object for
         * @param date The date to store the [Coins] object under
         * @returns A [Coins] object
         */
        fun fromJson(json: String, userId: String, date: Date): Coins {
            val dateString = SimpleDateFormat("yyyy.MM.dd", Locale.UK).format(date)
            val features = FeatureCollection.fromJson(json).features()
            if (features == null) {
                Log.e(this::class.java.simpleName, "No features in GeoJSON when creating list of coinz")
                return Coins(listOf(), userId, dateString)
            }
            val coins = features.map {
                val point = it.geometry() as Point
                Coin(
                        it.getStringProperty("id"),
                        point.latitude(),
                        point.longitude(),
                        it.getStringProperty("currency"),
                        it.getStringProperty("value").toDouble()
                )
            }
            return Coins(coins, userId, dateString)
        }
    }
}