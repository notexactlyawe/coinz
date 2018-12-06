package com.cameronmacleod.coinz

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
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
import android.util.AttributeSet
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
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseUser


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var netHelper: NetHelper
    // public so fragments can use
    // TODO: use a ViewModel to keep these
    var coins: Coins? = null
    var user: FirebaseUser? = null
    // value returned in onRequestPermissionsResult
    private val REQUEST_LOCATION = 1
    // used to track if we've explained to the user why we need the location permission
    private var shownLocationExplanationDialog = false
    private lateinit var progressOverlay: View
    private lateinit var userProfilePic: ImageView
    private lateinit var userEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        netHelper = NetHelper(this)

        userProfilePic = nvView.getHeaderView(0).findViewById(R.id.userProfilePic)
        userEmail = nvView.getHeaderView(0).findViewById(R.id.userEmail)

        // instantiates the ViewModel used to keep track of our user
        val mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        mainViewModel.user.observe(this, Observer { user ->
            // when user is null, they've signed out so no need to update UI
            if (user != null) {
                this.user = user
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

                // can force non-null because only use Google auth which ensures an email address
                updateUser(user.uid, user.email!!)
            }
        })

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        // Put a loading spinner over the main screen while we load the coins object
        progressOverlay = findViewById(R.id.progress_overlay)
        animateProgressBarIn()

        // Make sure current screen is selected in navigation drawer
        nvView.setNavigationItemSelectedListener(this)
        nvView.setCheckedItem(R.id.nav_main)

        fetchData()

        // Set first fragment to the blind mode screen
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.flContent, MainFragment())
        fragmentTransaction.commit()

        checkLocationPermission()
    }

    /**
     * Makes a spinner visible through an animation and disables interaction with the screen
     */
    fun animateProgressBarIn() {
        val inAnimation = AlphaAnimation(0f, 1f)
        inAnimation.setDuration(200)
        progressOverlay.setAnimation(inAnimation)
        progressOverlay.setVisibility(View.VISIBLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    /**
     * Hides a spinner through an animation and enables interaction with the screen
     */
    fun animateProgressBarOut() {
        val outAnimation = AlphaAnimation(1f, 0f)
        outAnimation.setDuration(200)
        progressOverlay.setAnimation(outAnimation)
        progressOverlay.setVisibility(View.GONE)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    /**
     * Checks if the location permission has been granted. If not, shows an explanation and requests
     * the permission.
     */
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

    /**
     * Called when the user has either granted or denied the permission
     */
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
        }
    }

    /**
     * Fetches a coins object through asynchronous calls
     */
    private fun fetchData() {
        netHelper.getJSONForDay(Calendar.getInstance().time) { response ->
            Log.d("FetchData response", response.toString())

            val sharedPref = this.getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString(getString(R.string.coinz_map_key), response.toString())
                apply()
            }

            if (user?.uid == null) {
                Log.e(javaClass.simpleName, "User ID was null, can't access user data")
            } else {
                getOrCreateCoinsObject(user!!.uid, Calendar.getInstance().time,
                        response.toString()) {
                    this.coins = it
                    animateProgressBarOut()
                }
            }
        }
    }

    /**
     * Signs the user out when the sign out button is clicked
     *
     * @param view Unused, but needed to match signature of onClick
     */
    fun onSignOutButtonClicked(view: View) {
        with (FirebaseAuth.getInstance()) {
            signOut()
        }
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    /**
     * Takes the user to the shop when the "Go To Shop" button is clicked on MainFragment
     *
     * @param view Unused, but needed to match signature of onClick
     */
    fun onShopButtonClicked(view: View) {
        nvView.setCheckedItem(R.id.nav_shop)
        onNavigationItemSelected(nvView.menu.findItem(R.id.nav_shop))
    }

    /**
     * Override to disable the back button
     *
     * If the back button was enabled then it would take the user back to the login screen which has
     * a bug when the app is already open and it is started
     */
    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
    }

    /**
     * Handles click events for the navigation menu on the left.
     *
     * Main swapping point between fragments
     */
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
                val fragment = ShopFragment()
                fragmentTransaction.replace(R.id.flContent, fragment)
            }
            R.id.nav_bank -> {
                val fragment = BankFragment()
                fragmentTransaction.replace(R.id.flContent, fragment)
            }
            R.id.nav_friends -> {
                val fragment = SendCoinzFragment()
                fragmentTransaction.replace(R.id.flContent, fragment)
            }
            R.id.nav_leaderboard -> {
                val fragment = LeaderboardFragment()
                fragmentTransaction.replace(R.id.flContent, fragment)
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

/**
 * The ViewModel for the MainActivity. Currently only keeps track of the current [FirebaseUser]
 */
class MainViewModel: ViewModel() {
    val user = UserLiveData()
}

/**
 * Observable object that tracks changes in the current [FirebaseUser]
 */
class UserLiveData: LiveData<FirebaseUser?>() {
    init {
        value = null
        FirebaseAuth.getInstance().addAuthStateListener {
            value = it.currentUser
        }
    }
}