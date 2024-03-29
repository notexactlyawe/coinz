package com.cameronmacleod.coinz

import android.location.Location
import android.util.Log
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import java.util.*
import kotlin.NoSuchElementException

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
                var banked: Boolean = false,
                var received: Boolean = false) {

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
     * Mark a coin as collected. Check [isCollectible] before using.
     */
    fun collect() {
        collected = true
    }

    /**
     * Checks if a coin is collectible.
     *
     * Only true when the coin is not yet collected and the user is within 25 metres
     *
     * @param user The location of the user
     * @return Whether the coin is collectible
     */
    fun isCollectible(user: Location): Boolean {
        if (!collected && getLocation().distanceTo(user) < 25) {
            return true
        }
        return false
    }

    /**
     * Checks if object equal to another. Does a naive object comparison.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Coin) {
            return false
        }

        return (coinId == other.coinId &&
                latitude == other.latitude &&
                longitude == other.longitude &&
                currency == other.currency &&
                amount == other.amount &&
                collected == other.collected &&
                banked == other.banked &&
                received == other.received)
    }

    /**
     * Class should implement since implements equals
     */
    override fun hashCode(): Int {
        return coinId.hashCode()
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
     * Returns a [Coin] if it exists with a specified ID
     *
     * @param id The ID of the coin to find
     * @return The [Coin] if it exists, or null
     */
    fun getCoinById(id: String): Coin? {
        return try {
            coins.first { it.coinId == id }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    /**
     * Convenience function to collect a contained coin that has a specific ID
     *
     * @param id The unique ID of the coin
     * @param userLocation The current location of the user
     * @return Whether or not the coin was successfully collected
     */
    fun collectCoinById(id: String, userLocation: Location): Boolean {
        val coin = getCoinById(id)

        if (coin == null) {
            Log.w(javaClass.simpleName, "No coin found for ID $id")
            return false
        }

        if (!coin.isCollectible(userLocation)) {
            Log.w(javaClass.simpleName, "Coin $id not collectible")
            return false
        }

        coin.collect()
        return true
    }

    /**
     * Convenience toString for debugging
     */
    override fun toString(): String {
        return "${coins.size} coins. User: $userId, date: $date"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Coins) {
            return false
        }

        return (coins == other.coins &&
                userId == other.userId &&
                date == other.date)
    }

    /**
     * Class should implement since it implements equals
     */
    override fun hashCode(): Int {
        // It's OK if this wraps
        return coins.hashCode() + userId.hashCode() + date.hashCode()
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
            val dateString = formatDate(date)
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