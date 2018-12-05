package com.cameronmacleod.coinz

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.Toast
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONException
import org.json.JSONObject

/**
 * Fragment that implements blind mode. Shows nearest 4 coins and exchange rates for the day
 */
class MainFragment : Fragment(), View.OnClickListener {
    private var currLocation: Location? = null
    private val stringIconMap = hashMapOf(
            "DOLR" to R.drawable.ic_dolr,
            "SHIL" to R.drawable.ic_shil,
            "PENY" to R.drawable.ic_peny,
            "QUID" to R.drawable.ic_quid)
    private var topFourCoins = arrayOf("", "", "", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Acquire a reference to the system Location Manager
        val locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Define a listener that responds to location updates
        val locationListener = object : LocationListener {

            override fun onLocationChanged(location: Location) {
                // Called when a new location is found by the network location provider.
                currLocation = location
                fillFourNearestCoins()
            }

            // The below functions are just included since they are abstract
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
            // try network provider, but if null, try GPS
            currLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?:
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            Log.d(javaClass.simpleName, "Location listener registered, last known location: $currLocation")
        } catch (e: SecurityException) {
            // user hasn't enabled location
            Log.e(this.javaClass.simpleName, "User hasn't enabled location: $e")
            val toast = Toast.makeText(activity, "Please enable location", Toast.LENGTH_LONG)
            toast.show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.content_main, container, false)

        view.findViewById<TableRow>(R.id.coinRow1).setOnClickListener(this)
        view.findViewById<TableRow>(R.id.coinRow2).setOnClickListener(this)
        view.findViewById<TableRow>(R.id.coinRow3).setOnClickListener(this)
        view.findViewById<TableRow>(R.id.coinRow4).setOnClickListener(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillRates()
        fillFourNearestCoins()
    }

    /**
     * Displays the exchange rates for the day
     *
     * Gets data from GeoJSON in SharedPrefs
     */
    private fun fillRates() {
        val sharedPrefs = activity?.getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE)
        if (sharedPrefs == null) {
            return
        }
        val geojsonStr = sharedPrefs.getString(getString(R.string.coinz_map_key), "")

        if (geojsonStr == "") {
            Log.w(javaClass.simpleName, "coinzmap hasn't yet been initialized")
            return
        }

        val geojson = JSONObject(geojsonStr)

        try {
            val rates = geojson.getJSONObject("rates")

            shil_conversion_rate.text = rates.getString("SHIL")
            dolr_conversion_rate.text = rates.getString("DOLR")
            quid_conversion_rate.text = rates.getString("QUID")
            peny_conversion_rate.text = rates.getString("PENY")
        } catch (e: JSONException) {
            val toast = Toast.makeText(activity, R.string.no_geojson_toast, Toast.LENGTH_LONG)
            toast.show()
            Log.e(this.javaClass.simpleName, "Error getting rates from geojson: ${e.localizedMessage}")
        }
    }

    /**
     * Displays information about the four nearest coinz
     */
    private fun fillFourNearestCoins() {
        if (currLocation == null || activity == null) {
            // can't do anything until location comes in or if we have no attached activity
            return
        }

        val coins = (activity as MainActivity).coins ?: return
        // sort based on distance
        coins.coins.sortedBy {
            distanceToCoin(it)
        }.filter {
            !it.collected
        }.apply {
            // set currencies
            try {
                this[0].currency.apply {
                    coinType1.text = this
                    imgCoinIcon1.setImageResource(
                            stringIconMap.getOrDefault(this, R.drawable.ic_coinz_24dp))
                }
                this[1].currency.apply {
                    coinType2.text = this
                    imgCoinIcon2.setImageResource(
                            stringIconMap.getOrDefault(this, R.drawable.ic_coinz_24dp))
                }
                this[2].currency.apply {
                    coinType3.text = this
                    imgCoinIcon3.setImageResource(
                            stringIconMap.getOrDefault(this, R.drawable.ic_coinz_24dp))
                }
                this[3].currency.apply {
                    coinType4.text = this
                    imgCoinIcon4.setImageResource(
                            stringIconMap.getOrDefault(this, R.drawable.ic_coinz_24dp))
                }
            } catch (npe: NullPointerException) {
                Log.e("Setting currencies", "Something was null $npe")
            }

            // set values
            coinValue1.text = this[0].amount.toInt().toString()
            coinValue2.text = this[1].amount.toInt().toString()
            coinValue3.text = this[2].amount.toInt().toString()
            coinValue4.text = this[3].amount.toInt().toString()

            // set distances
            coinDistance1.text = "${distanceToCoin(this[0]).toInt()} metres"
            coinDistance2.text = "${distanceToCoin(this[1]).toInt()} metres"
            coinDistance3.text = "${distanceToCoin(this[2]).toInt()} metres"
            coinDistance4.text = "${distanceToCoin(this[3]).toInt()} metres"

            // set directions
            coinDirection1.text = getDirectionToCoin(this[0])
            coinDirection2.text = getDirectionToCoin(this[1])
            coinDirection3.text = getDirectionToCoin(this[2])
            coinDirection4.text = getDirectionToCoin(this[3])

            // set collectable
            if (this[0].isCollectable(currLocation!!))
                coinRow1.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
            else
                coinRow1.setBackgroundColor(Color.TRANSPARENT)
            if (this[1].isCollectable(currLocation!!))
                coinRow2.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
            else
                coinRow2.setBackgroundColor(Color.TRANSPARENT)
            if (this[2].isCollectable(currLocation!!))
                coinRow3.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
            else
                coinRow3.setBackgroundColor(Color.TRANSPARENT)
            if (this[3].isCollectable(currLocation!!))
                coinRow4.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
            else
                coinRow4.setBackgroundColor(Color.TRANSPARENT)

            // keep reference to coins
            topFourCoins = arrayOf(this[0].coinId, this[1].coinId, this[2].coinId, this[3].coinId)
        }
    }

    /**
     * Listens to click events for the four nearest coins
     */
    override fun onClick(view: View) {
        if (view == coinRow1) {
            collectCoin(topFourCoins[0])
        } else if (view == coinRow2) {
            collectCoin(topFourCoins[1])
        } else if (view == coinRow3) {
            collectCoin(topFourCoins[2])
        } else if (view == coinRow4) {
            collectCoin(topFourCoins[3])
        }
    }

    /**
     * Attempts to collect a coin
     */
    private fun collectCoin(coinId: String) {
        if (activity == null || currLocation == null) {
            Log.e("collectCoin", "Activity or location was null")
            return
        }
        val coins = (activity as MainActivity).coins

        if (coins == null) {
            Log.w(javaClass.simpleName, "coins was null in collectCoin")
            return
        }

        if (coins.collectCoinById(coinId, currLocation!!)) {
            // update list
            fillFourNearestCoins()

            updateUsersToCoinz(coins)

            val toast = Toast.makeText(activity!!, R.string.coin_collected_text, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    /**
     * Returns the distance between a coin and the user
     */
    private fun distanceToCoin(coin: Coin): Float {
        if (currLocation == null) return 0f

        val loc = currLocation as Location

        return loc.distanceTo(coin.getLocation())
    }

    /**
     * Gets the direction to a coin as a string
     *
     * Returns based on segments around each of 8 cardinal directions
     */
    private fun getDirectionToCoin(coin: Coin): String {
        if (currLocation == null) return "North"

        val loc = currLocation as Location

        val bearingDegrees = loc.bearingTo(coin.getLocation())

        when {
            bearingDegrees < 22.5  -> return "N"
            bearingDegrees < 67.5  -> return "NE"
            bearingDegrees < 112.5 -> return "E"
            bearingDegrees < 157.5 -> return "SE"
            bearingDegrees < 202.5 -> return "S"
            bearingDegrees < 247.5 -> return "SW"
            bearingDegrees < 292.5 -> return "W"
            bearingDegrees < 337.5 -> return "NW"
            else -> return "N"
        }
    }
}
