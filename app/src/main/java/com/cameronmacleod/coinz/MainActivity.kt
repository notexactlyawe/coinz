package com.cameronmacleod.coinz

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, OnFragmentInteractionListener {
    private lateinit var netHelper: NetHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        netHelper = NetHelper(this)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        // Make sure current screen is selected in navigation drawer
        nvView.setNavigationItemSelectedListener(this)
        nvView.setCheckedItem(R.id.nav_main)

        FirebaseAuth.getInstance().addAuthStateListener(::firebaseAuthListener)

        fetchData()

        // Set first fragment to the blind mode screen
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.flContent, MainFragment())
        fragmentTransaction.commit()
    }

    private fun firebaseAuthListener(auth: FirebaseAuth) {
        val user = auth.currentUser
        if (user == null) {
            // user signed out
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else {
            if (user.photoUrl == null) {
                Log.w("MainActivity", "User had no profile picture")
            } else {
                netHelper.getProfilePicture(user.photoUrl.toString(), userProfilePic.scaleType
                ) { bitmap ->
                    Log.d(javaClass.simpleName, "Got bitmap from url ${user.photoUrl}")
                    userProfilePic.setImageBitmap(bitmap)
                }
            }

            userEmail.text = user.email
        }
    }

    private fun fetchData() {
        netHelper.getJSONForDay(Calendar.getInstance().time) { response ->
            Log.d("FetchData response", response.toString())

            val sharedPref = this.getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString(getString(R.string.coinz_map_key), response.toString())
                apply()
            }
        }
    }

    override fun onFragmentInteraction(uri: Uri) {
        Log.d(this.javaClass.simpleName,"Fragment interaction with Uri: $uri")
    }

    fun onSignOutButtonClicked(view: View) {
        with (FirebaseAuth.getInstance()) {
            removeAuthStateListener(::firebaseAuthListener)
            signOut()
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        when (item.itemId) {
            R.id.nav_main -> {
                val fragment = MainFragment()
                fragmentTransaction.replace(R.id.flContent, fragment)
            }
            R.id.nav_shop -> {

            }
            R.id.nav_bank -> {

            }
            R.id.nav_friends -> {

            }
            R.id.nav_upgrade -> {

            }
            R.id.nav_leaderboard -> {

            }
            R.id.nav_map -> {
                val fragment = MapFragment()
                fragmentTransaction.replace(R.id.flContent, fragment)
            }
        }

        fragmentTransaction.commit()

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
