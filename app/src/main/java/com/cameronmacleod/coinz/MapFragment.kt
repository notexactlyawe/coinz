package com.cameronmacleod.coinz

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.mapbox.mapboxsdk.maps.MapboxMap

/**
 * Fragment that displays a map with the coins and the user's location
 */
class MapFragment : Fragment() {
    private lateinit var mapView: MapView
    // Avoid re-generating bitmaps by keeping a reference to them
    private lateinit var dolrBitmap: Bitmap
    private lateinit var shilBitmap: Bitmap
    private lateinit var quidBitmap: Bitmap
    private lateinit var penyBitmap: Bitmap
    private lateinit var collBitmap: Bitmap
    private var map: MapboxMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity != null) {
            val nonNullActivity = activity as Activity
            Mapbox.getInstance(nonNullActivity.applicationContext, "pk.eyJ1Ijoibm90ZXhhY3RseWF3ZSIsImEiOiJjam92enp0b2sxbjdqM3JucmJxbGcxbzI1In0.zNr5YfYwwBzpkp_ReEw2-w");

            // create bitmaps early so that they're only created once
            dolrBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_dolr)
            shilBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_shil)
            quidBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_quid)
            penyBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_peny)
            collBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_coinz_24dp)

            // Acquire a reference to the system Location Manager
            val locationManager = nonNullActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Define a listener that responds to location updates
            val locationListener = object : LocationListener {

                override fun onLocationChanged(location: Location) {
                    // Called when a new location is found by the network location provider.
                    val main = nonNullActivity as MainActivity
                    if (main.coins == null) {
                        return
                    }
                    collectNearbyCoins(location, main.coins!!)

                    refreshMap()
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                }

                override fun onProviderEnabled(provider: String) {
                }

                override fun onProviderDisabled(provider: String) {
                }
            }

            try {
                // Register the listener with the Location Manager to receive location updates
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener)
            } catch (e: SecurityException) {
                // user hasn't enabled location
                Log.e(this.javaClass.simpleName, "User hasn't enabled location: $e")
                val toast = Toast.makeText(nonNullActivity, "Please enable location", Toast.LENGTH_LONG)
                toast.show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity != null) {
            val nonNullActivity = activity as Activity
            mapView = nonNullActivity.findViewById(R.id.mapView) as MapView
            mapView.onCreate(savedInstanceState)

            // Wait for map to be loaded
            mapView.getMapAsync { mapboxMap ->
                this.map = mapboxMap
                try {
                    val parent = activity as MainActivity
                    // for every coin create a marker and add it
                    parent.coins?.coins?.forEach { coin ->
                        val m = MarkerOptions().apply {
                            position = LatLng(coin.latitude, coin.longitude)
                            title = coin.currency
                            snippet = coin.amount.toInt().toString()
                            icon = getIcon(coin.collected, coin.currency)
                        }
                        mapboxMap.addMarker(m)
                    }

                    // enable showing of user location on map
                    val locationComponent = mapboxMap?.locationComponent
                    try {
                        locationComponent?.activateLocationComponent(activity!!)
                        locationComponent?.isLocationComponentEnabled = true
                    } catch (e: SecurityException) {
                        Log.e(javaClass.simpleName, "No location permission when activating map location")
                    }
                } catch (e: Exception) {
                    Log.e(this.javaClass.simpleName, "Error in creating map ${e.printStackTrace()}")
                }
            }
        }
    }

    /**
     * Checks if there are nearby coins and collects them
     *
     * @param location The user's location
     * @param coins The list of coins to collect from
     */
    private fun collectNearbyCoins(location: Location, coins: Coins) {
        if (coins.getNumCollected() > 24) {
            val toast = Toast.makeText(activity, "Can't collect any more coinz today!",
                    Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        // we only want to make a db call if we have updated coins to save bandwidth
        var coinsUpdated = false
        coins.coins.filter {
            // get all coins that are nearer than 25 metres and that aren't collected
            (location.distanceTo(it.getLocation()) < 25) && !it.collected
        }.forEach {
            it.collect()
            val toast = Toast.makeText(activity, R.string.coin_collected_text, Toast.LENGTH_SHORT)
            toast.show()
            coinsUpdated = true
        }

        if (coinsUpdated) {
            updateUsersToCoinz(coins)
        }
    }

    /**
     * Removes and re-adds all markers on the map
     */
    private fun refreshMap() {
        if (map == null || activity == null) {
            return
        }

        map!!.removeAnnotations()

        val parent = activity as MainActivity

        parent.coins?.coins?.forEach { coin ->
            val m = MarkerOptions().apply {
                position = LatLng(coin.latitude, coin.longitude)
                title = coin.currency
                snippet = coin.amount.toInt().toString()
                icon = getIcon(coin.collected, coin.currency)
            }
            map!!.addMarker(m)
        }
    }

    /**
     * Gets an icon to display on the map for a coin
     *
     * @param collected Whether the coin has been collected
     * @param currency The currency of the coin
     * @return An [Icon] to represent the coin
     */
    private fun getIcon(collected: Boolean, currency: String): Icon {
        val icon = IconFactory.getInstance(activity!!)
        val bitmap: Bitmap?
        if (collected) {
            bitmap = collBitmap
            return icon.fromBitmap(bitmap)
        }
        when (currency) {
            "SHIL" -> bitmap = shilBitmap
            "QUID" -> bitmap = quidBitmap
            "DOLR" -> bitmap = dolrBitmap
            "PENY" -> bitmap = penyBitmap
            else -> {
                bitmap = collBitmap
            }
        }
        return icon.fromBitmap(bitmap)
    }

    /**
     * Gets a bitmap from a drawable resource
     *
     * @param context The application context
     * @param drawableId The resource to convert
     * @return A bitmap of size (100, 100) from [drawableId]
     */
    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)!!

        val bitmap = Bitmap.createBitmap(100,
                100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    // Below functions are needed for the map to track lifecycle changes
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

}
