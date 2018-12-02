package com.cameronmacleod.coinz

import android.location.Location
import android.util.Log
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import java.text.SimpleDateFormat
import java.util.*

data class Coin(var coinId: String = "",
                var latitude: Double = 0.0,
                var longitude: Double = 0.0,
                var currency: String = "",
                var amount: Double = 0.0,
                var collected: Boolean = false) {

    fun getLocation(): Location {
        val loc = Location("geojson")
        loc.latitude = latitude
        loc.longitude = longitude
        return loc
    }

    override fun toString(): String {
        return "${coinId}, ${latitude}, ${longitude}, ${currency}, ${amount}, ${collected}"
    }

    fun collect() {
        collected = true
    }

    fun isCollectable(user: Location): Boolean {
        if (!collected && getLocation().distanceTo(user) < 5) {
            return true
        }
        return false
    }
}

data class Coins(var coins: List<Coin> = listOf(),
                 var userId: String = "",
                 var date: String = "") {
    val name = "$userId:$date"

    override fun toString(): String {
        return "${coins.size} coins. User: ${userId}, date: {$date}"
    }

    companion object Factory {
        fun fromJson(json: String, userId: String, date: Date): Coins {
            val dateString = SimpleDateFormat("yyyy.MM.dd").format(date)
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