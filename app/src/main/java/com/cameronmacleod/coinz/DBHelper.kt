package com.cameronmacleod.coinz

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Update or create an entry in the usersToCoinz collection
 *
 * @param coins The [Coins] object to put in the table
 */
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

/**
 * Update or create an entry in the usersToCoinz collection
 *
 * @param bank The [Bank] object to put in the collection
 */
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

/**
 * Gets a [Bank] object from the collection or creates a new one if the userID isn't in there
 *
 * @param userID The ID of the document in the bank table
 * @param email The user's email address
 * @param callback Called when a [Bank] object has been retrieved or created with the object
 */
fun getOrCreateBank(userID: String, email: String = "", callback: (Bank) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var bank = Bank(email = email, name = userID)
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

/**
 * Gets the top [limit] bank balances by the amount of gold they contain
 *
 * @param limit The maximum number of bank accounts to return
 * @param callback Called when the DB query finishes passing the result
 */
fun getTopNBanks(limit: Long, callback: (List<Bank>?) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("bank")
            .orderBy("goldBalance", Query.Direction.DESCENDING)
            .limit(limit).get()
            .addOnCompleteListener {
                val banks = it.result?.toObjects(Bank::class.java)

                if (banks == null) {
                    Log.e("Firestore", "Couldn't get banks")
                }

                callback(banks)
            }
}

/**
 * Updates or creates an entry in the user collection
 *
 * @param userID The user's ID to stick in the collection
 * @param email The user's email address, also the ID of the document
 */
fun updateUser(userID: String, email: String) {
    val data = HashMap<String, Any>()
    data["id"] = userID

    val db = FirebaseFirestore.getInstance()
    db.collection("user")
            .document(email).set(data)
            .addOnSuccessListener { reference ->
                Log.d("FireStore",
                        "User added or updated with ID $userID")
            }
            .addOnFailureListener { e ->
                Log.e("FireStore", "error adding document $e")
            }
}

/**
 * Gets a user ID for an email address from the user collection
 *
 * @param email The email address of the user to get
 * @param callbackWithId Called on success and failure and returns either the ID or null
 */
fun getUserIDForEmail(email: String, callbackWithId: (String?) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("user")
            .document(email).get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful || !task.result!!.exists()) {
                    callbackWithId(null)
                } else {
                    Log.d("Firestore", "result: ${task.result}")
                    callbackWithId(task.result?.get("id").toString())
                }
            }
            .addOnFailureListener {
                Log.d("FireStore", "Couldn't get $email from user table\n$it")
                callbackWithId(null)
            }
}