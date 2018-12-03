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

fun getOrCreateBank(userID: String, callback: (Bank) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var bank = Bank(name=userID)
    db.collection("bank").document(userID).get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful || !task.result!!.exists()) {
                    updateBank(bank)
                } else {
                    Log.d("Firestore", "result: ${task.result}")
                    bank = task.result?.toObject(Bank::class.java)!!
                }
                callback(bank)
            }
            .addOnFailureListener {
                Log.d("FireStore", "Couldn't get $userID from bank table\n$it")
                updateBank(bank)
                callback(bank)
            }
}