package com.cameronmacleod.coinz

import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.util.*

/**
 * Tests of the Firebase helper methods.
 *
 * Requires internet access to work since Firebase is not 100% locally testable.
 * Because deleting items requires internet
 */
@RunWith(AndroidJUnit4::class)
class FirebaseHelperTest {
    private lateinit var fireStore: FirebaseFirestore
    private val userID = "SomeUser"
    private val email = "someemail@google.com"

    /**
     * Synchronous deletion of a document
     *
     * Turns Firebase online to delete a document, deletes it, restores offline mode and returns
     * Needs to go online because deletion doesn't work offline and there is currently no way to
     * clear the local cache
     */
    private fun deleteDocument(collection: String, document: String) {
        var finished = false

        fireStore.enableNetwork().addOnCompleteListener { _ ->
            fireStore.collection(collection).document(document)
                    .delete().addOnCompleteListener {
                        if (!it.isSuccessful) {
                            Log.d(javaClass.simpleName, "Document not deleted")
                        }

                        fireStore.disableNetwork().addOnCompleteListener { _ ->
                            finished = true
                        }
                    }
        }

        while (!finished) {
            Thread.sleep(50)
        }
    }

    @Before
    fun firebaseSetup() {
        var networkDisabled = false

        // Test firebase safely by disabling network access
        fireStore = FirebaseFirestore.getInstance()
        fireStore.disableNetwork().addOnCompleteListener {
            networkDisabled = true
        }

        // Wait for network to be disabled for Firebase
        while (!networkDisabled) {
            Thread.sleep(50)
        }

        Log.d(javaClass.simpleName, "Disabled network for Firebase")
    }

    @Test
    fun getCoinsUserNotExist() {
        var coinsReturned = false
        // Get a coins object that doesn't exist
        getCoinsObject("SomeUnknownUser", Calendar.getInstance().time) {
            assertNull(it)
            coinsReturned = true
        }

        // Wait for the get to finish
        while (!coinsReturned) {
            Thread.sleep(50)
        }
    }

    @Test
    fun createThenGetCoins() {
        val date = Calendar.getInstance().time
        var gotCoins = false

        // create an empty Coins object
        val coins = Coins(
                userId = userID,
                date = formatDate(date))

        updateUsersToCoinz(coins)

        // wait for coins to be written
        Thread.sleep(100)

        getCoinsObject(userID, date) {
            assertEquals(coins, it)
            gotCoins = true
        }

        while (!gotCoins) {
            Thread.sleep(50)
        }

        // clean up
        deleteDocument("usersToCoinz", coins.name)
    }

    @Test
    fun newBankStartsEmpty() {
        var gotBank = false

        // get a bank that doesn't yet exist, creating a new one in the process
        // assert default values
        getOrCreateBank(userID, email) {
            assertEquals(it.email, email)
            assertEquals(it.name, userID)
            assertEquals(it.dolrBalance, 0.0, 0.1)
            assertEquals(it.goldBalance, 0.0, 0.1)
            assertEquals(it.penyBalance, 0.0, 0.1)
            assertEquals(it.quidBalance, 0.0, 0.1)
            assertEquals(it.shilBalance, 0.0, 0.1)
            gotBank = true
        }

        while (!gotBank) {
            Thread.sleep(50)
        }

        // clean up
        deleteDocument("bank", userID)
    }

    @Test
    fun updateAndGetBank() {
        var finished = false

        getOrCreateBank(userID, email) { bank ->
            // this should be a freshly created bank (or should I say... newly minted)
            assertEquals(0.0, bank.dolrBalance, 0.1)

            bank.dolrBalance = 5.0
            updateBank(bank)
            Thread.sleep(100)
            getOrCreateBank(userID, email) {
                assertEquals(5.0, it.dolrBalance, 0.1)
                finished = true
            }
        }

        while (!finished) {
            Thread.sleep(50)
        }

        // clean up
        deleteDocument("bank", userID)
    }
}
