package com.cameronmacleod.coinz

import android.location.Location
import org.junit.Test

import org.junit.Assert.*

/**
 * Tests [Coin] and [Coins] and the method contained within those classes
 */
class CoinsTest {
    @Test
    fun testGetLocation() {
        val someLatitude = 55.3456
        val someLongitude = -3.479
        val c = Coin(latitude = someLatitude, longitude = someLongitude)

        var loc = c.getLocation()
        assertEquals(someLatitude, loc.latitude, 0.1)
        assertEquals(someLongitude, loc.longitude, 0.1)

        // assert that changes work with the method
        c.latitude = someLongitude
        loc = c.getLocation()
        assertEquals(someLongitude, loc.latitude, 0.1)
        assertEquals(someLongitude, loc.longitude, 0.1)
    }

    @Test
    fun testIsCollectibleNearbyUser() {
        val someLatitude = 55.3456
        val someLongitude = -3.479
        val userLocation = Location("test").apply {
            latitude = someLatitude
            longitude = someLongitude
        }

        val c = Coin(latitude = someLatitude, longitude = someLongitude)
        assertTrue(c.isCollectible(userLocation))

        c.collect()

        // can't collect already collected coin
        assertFalse(c.isCollectible(userLocation))
    }

    @Test
    fun testIsCollectibleFarUser() {
        val someLatitude = 55.3456
        val someLongitude = -3.479
        val userLocation = Location("test").apply {
            latitude = someLatitude
            longitude = someLongitude
        }

        // make coin far away from user
        val c = Coin(latitude = someLatitude - 5, longitude = someLongitude)
        // user is too far away for coin to be collectible
        assertFalse(c.isCollectible(userLocation))

        c.collect()

        // can't collect already collected coin
        assertFalse(c.isCollectible(userLocation))
    }

    @Test
    fun testGetCoinByID() {
        val c = Coins(listOf(
                Coin(coinId = "1"),
                Coin(coinId = "2"),
                Coin(coinId = "3")
        ))

        // id that exists should not return null
        assertNotNull(c.getCoinById("1"))
        assertNotNull(c.getCoinById("2"))
        assertNotNull(c.getCoinById("3"))

        // id that doesn't exist should return null
        assertNull(c.getCoinById("Mr. Blobby"))
    }

    @Test
    fun testCollectCoinByID() {
        val latitude = 0.0
        val longitude = 0.0

        val coin1 = Coin(coinId = "1", latitude = latitude, longitude = longitude)
        val coin2 = Coin(coinId = "2", latitude = latitude, longitude = longitude)

        val coins = Coins(listOf(coin1, coin2))
        val userLocation = Location("test").apply {
            this.latitude = latitude
            this.longitude = longitude
        }

        assertFalse(coin1.collected)
        assertTrue(coins.collectCoinById("1", userLocation))
        assertTrue(coin1.collected)

        assertFalse(coin2.collected)
        assertTrue(coins.collectCoinById("2", userLocation))
        assertTrue(coin2.collected)

        // non-existent coin should return false
        assertFalse(coins.collectCoinById("Bloop", userLocation))
    }
}
