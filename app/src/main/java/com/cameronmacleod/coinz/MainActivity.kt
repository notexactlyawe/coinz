package com.cameronmacleod.coinz

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import android.view.animation.AlphaAnimation
import android.view.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var netHelper: NetHelper
    // public so fragments can use
    var coins: Coins? = null
    private val REQUEST_LOCATION = 1
    private var shownLocationExplanationDialog = false
    var userID: String? = null
    private lateinit var progressOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        netHelper = NetHelper(this)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        progressOverlay = findViewById(R.id.progress_overlay)
        animateProgressBarIn()

        // Make sure current screen is selected in navigation drawer
        nvView.setNavigationItemSelectedListener(this)
        nvView.setCheckedItem(R.id.nav_main)

        FirebaseAuth.getInstance().addAuthStateListener(::firebaseAuthListener)

        fetchData()

        // Set first fragment to the blind mode screen
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.flContent, MainFragment())
        fragmentTransaction.commit()

        checkLocationPermission()
    }

    fun animateProgressBarIn() {
        val inAnimation = AlphaAnimation(0f, 1f)
        inAnimation.setDuration(200)
        progressOverlay.setAnimation(inAnimation)
        progressOverlay.setVisibility(View.VISIBLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun animateProgressBarOut() {
        val outAnimation = AlphaAnimation(1f, 0f)
        outAnimation.setDuration(200)
        progressOverlay.setAnimation(outAnimation)
        progressOverlay.setVisibility(View.GONE)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) && !shownLocationExplanationDialog) {
                // show explanation of permission
                val dialog = AlertDialog.Builder(this).apply {
                    setMessage(R.string.location_explanation)
                    setTitle("Location Permission")
                    setNeutralButton("OK") { result, something ->
                        checkLocationPermission()
                    }
                }
                shownLocationExplanationDialog = true
                dialog.show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission granted
                } else {
                    val toast = Toast.makeText(this, "Cannot operate without location", Toast.LENGTH_LONG)
                    toast.show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun firebaseAuthListener(auth: FirebaseAuth) {
        val user = auth.currentUser
        if (user == null) {
            // user signed out
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else {
            userID = user.uid
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

            // can force non-null because only use Google auth
            updateUser(user.uid, user.email!!)
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

            if (userID == null) {
                Log.e(javaClass.simpleName, "User ID was null, can't access user data")
            } else {
                retrieveOrCreateCoinsObject(response.toString())
            }
        }
    }

    private fun retrieveOrCreateCoinsObject(json: String) {
        // smart casts don't work when compiler can't guarantee that the variable can't
        // change between the check and the usage
        val db = FirebaseFirestore.getInstance()

        val userCoinz = Coins.fromJson(json, userID!!, Calendar.getInstance().time)

        db.collection("usersToCoinz").document(userCoinz.name).get()
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful || !task.result!!.exists()) {
                        coins = userCoinz
                        updateUsersToCoinz(userCoinz)
                    } else {
                        Log.d("FireStore", "result: ${task.result}")
                        coins = task.result?.toObject(Coins::class.java)!!
                        Log.d(javaClass.simpleName, coins.toString())
                        Log.d(javaClass.simpleName, coins?.coins?.get(0).toString())
                    }
                    animateProgressBarOut()
                }
                .addOnFailureListener {
                    Log.d("FireStore", "Couldn't get ${userCoinz.name} from db\n$it")
                    coins = userCoinz
                    updateUsersToCoinz(userCoinz)
                    animateProgressBarOut()
                }
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
                val fragment = BankFragment()
                fragmentTransaction.replace(R.id.flContent, fragment)
            }
            R.id.nav_friends -> {
                val fragment = SendCoinzFragment()
                fragmentTransaction.replace(R.id.flContent, fragment)
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
