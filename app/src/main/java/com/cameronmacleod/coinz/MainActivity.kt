package com.cameronmacleod.coinz

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val coinzDbName : String = getString(R.string.db_name)
        val db = applicationContext.getSharedPreferences(coinzDbName, Context.MODE_PRIVATE)
        if (db.getBoolean("isLoggedIn", false)) {
            val toast = Toast.makeText(applicationContext, "Logged in", Toast.LENGTH_SHORT)
            toast.show()
        }
        setContentView(R.layout.activity_main)
    }

    fun onLoginButtonClicked(view: View) {
        val password = passwordInput.text
        val toast = Toast.makeText(applicationContext, password, Toast.LENGTH_SHORT)
        toast.show()
        val coinzDbName : String = getString(R.string.db_name)
        val db = applicationContext.getSharedPreferences(coinzDbName, Context.MODE_PRIVATE)
        val editable = db.edit()
        editable.putBoolean("isLoggedIn", true)
        editable.commit()
    }
}
