package com.cameronmacleod.coinz

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

fun updateUsersToCoinz(coins: Coins) {
    val db = FirebaseFirestore.getInstance()
    db.collection("usersToCoinz")
            .document(coins.name).set(coins)
            .addOnSuccessListener { reference ->
                Log.d("FireStore",
                        "Document snapshot added with ID ${coins.name}")
            }
            .addOnFailureListener { e ->
                Log.e("FireStore", "error adding document $e")
            }
}

fun updateBank(bank: Bank) {
    val db = FirebaseFirestore.getInstance()
    db.collection("bank")
            .document(bank.name).set(bank)
            .addOnSuccessListener { reference ->
                Log.d("FireStore",
                        "Document snapshot added with ID ${bank.name}")
            }
            .addOnFailureListener { e ->
                Log.e("FireStore", "error adding document $e")
            }
}