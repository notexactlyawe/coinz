package com.cameronmacleod.coinz

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONException
import org.json.JSONObject

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OnFragmentInteractionListener] interface
 * to handle interaction events.
 *
 */
class MainFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private var currLocation: Location? = null
    private val stringIconMap = hashMapOf(
            "DOLR" to R.drawable.ic_dolr,
            "SHIL" to R.drawable.ic_shil,
            "PENY" to R.drawable.ic_peny,
            "QUID" to R.drawable.ic_quid)

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
            val toast = Toast.makeText(activity, "Please enable location", Toast.LENGTH_LONG)
            toast.show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.content_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillRates()
        fillFourNearestCoins()
    }

    private fun fillRates() {
        val sharedPrefs = activity?.getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE)
        if (sharedPrefs == null) {
            return
        }
        val geojsonStr = sharedPrefs.getString(getString(R.string.coinz_map_key), "")
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

    private fun fillFourNearestCoins() {
        if (currLocation == null) {
            // can't do anything until location comes in
            return
        }

        val sharedPrefs = activity?.getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE)
        if (sharedPrefs == null) {
            return
        }
        val geojsonStr = sharedPrefs.getString(getString(R.string.coinz_map_key), "")
        val featureCollection = FeatureCollection.fromJson(geojsonStr)
        // sort based on distance
        featureCollection.features()?.sortedBy {
            distanceToFeature(it)
        }.apply {
            // set currencies
            try {
                this!!.get(0)?.properties()?.get("currency")!!.asString.apply {
                    coinType1.text = this
                    imgCoinIcon1.setImageResource(
                            stringIconMap.getOrDefault(this, R.drawable.ic_coinz_24dp))
                }
                get(1)?.properties()?.get("currency")!!.asString.apply {
                    coinType2.text = this
                    imgCoinIcon2.setImageResource(
                            stringIconMap.getOrDefault(this, R.drawable.ic_coinz_24dp))
                }
                get(2)?.properties()?.get("currency")!!.asString.apply {
                    coinType3.text = this
                    imgCoinIcon3.setImageResource(
                            stringIconMap.getOrDefault(this, R.drawable.ic_coinz_24dp))
                }
                get(3)?.properties()?.get("currency")!!.asString.apply {
                    coinType4.text = this
                    imgCoinIcon4.setImageResource(
                            stringIconMap.getOrDefault(this, R.drawable.ic_coinz_24dp))
                }
            } catch (npe: NullPointerException) {
                Log.e("Setting currencies", "Something was null $npe")
            }

            // set values
            coinValue1.text = this?.get(0)?.properties()?.get("marker-symbol")?.asString
            coinValue2.text = this?.get(1)?.properties()?.get("marker-symbol")?.asString
            coinValue3.text = this?.get(2)?.properties()?.get("marker-symbol")?.asString
            coinValue4.text = this?.get(3)?.properties()?.get("marker-symbol")?.asString

            // set distances
            coinDistance1.text = "${distanceToFeature(this?.get(0) as Feature).toInt()} metres"
            coinDistance2.text = "${distanceToFeature(this?.get(1) as Feature).toInt()} metres"
            coinDistance3.text = "${distanceToFeature(this?.get(2) as Feature).toInt()} metres"
            coinDistance4.text = "${distanceToFeature(this?.get(3) as Feature).toInt()} metres"

            // TODO: set directions
        }
    }

    private fun distanceToFeature(feature: Feature): Float {
        if (currLocation == null) return 0f;

        val loc = currLocation as Location

        return loc.distanceTo(Location(loc).apply {
            latitude = (feature.geometry() as Point).latitude()
            longitude = (feature.geometry() as Point).longitude()
        })
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
