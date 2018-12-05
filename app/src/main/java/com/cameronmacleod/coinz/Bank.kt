package com.cameronmacleod.coinz

/**
 * Serializable (to Firestore) class that represents a user's bank balance
 */
data class Bank(var shilBalance: Double = 0.0,
                var dolrBalance: Double = 0.0,
                var penyBalance: Double = 0.0,
                var quidBalance: Double = 0.0,
                var goldBalance: Double = 0.0,
                var email: String = "",
                var name: String = "") {
}