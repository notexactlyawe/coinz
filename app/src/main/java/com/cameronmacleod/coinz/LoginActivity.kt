package com.cameronmacleod.coinz

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

const val RC_SIGN_IN = 1

/**
 * The launch activity of the class, ensures user is logged in then passes along to MainActivity
 */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val user = FirebaseAuth.getInstance().currentUser

        // If the user is null then the activity will display and allow the user to log in
        if (user != null) {
            Log.d(javaClass.simpleName, "User ${user.email} already logged in")
            redirectToMainScreen()
        }
    }

    /**
     * Function to handle login. Redirects to the Firebase login UI
     */
    fun onLoginButtonClicked(@Suppress("UNUSED_PARAMETER")view: View) {
        val providers = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN)
    }

    /**
     * Convenience function to start the MainActivity
     */
    private fun redirectToMainScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    /**
     * Called when the Firebase login UI returns. Checks if login worked and redirects appropriately
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                Log.d(javaClass.simpleName, "Successfully signed in ${user?.email}")
                redirectToMainScreen()
            } else if (response != null) {
                // if response was null, then user pressed the back button
                val toast = Toast.makeText(applicationContext,
                        "Login failed, please try again", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }
}
