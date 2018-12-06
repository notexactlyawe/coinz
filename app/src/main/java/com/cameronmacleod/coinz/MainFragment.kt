package com.cameronmacleod.coinz

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_main.*
import org.json.JSONException
import org.json.JSONObject

/**
 * Fragment that implements blind mode. Shows nearest 4 coins and exchange rates for the day
 */
class MainFragment : Fragment() {
    private var currLocation: Location? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: BlindModeAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var topFourCoins = listOf<Coin>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        viewManager = LinearLayoutManager(activity)
        viewAdapter = BlindModeAdapter(topFourCoins, ::getLocation, ::onClick, activity!!)

        recyclerView = view.findViewById<RecyclerView>(R.id.nearestFourCoins).apply {
            // improves performance since each element is same size
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

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

        return view
    }

    private fun getLocation(): Location {
        return currLocation!!
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
                Context.MODE_PRIVATE) ?: return
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
            Log.d(javaClass.simpleName, "currLocation null? ${currLocation==null}")
            return
        }

        val coins = (activity as MainActivity).coins ?: return
        // sort based on distance
        topFourCoins = coins.coins.sortedBy {
            distanceToCoin(it, currLocation)
        }.filter {
            !it.collected
        }.take(4)

        viewAdapter.changeData(topFourCoins)
    }

    /**
     * Listens to click events for the four nearest coins
     */
    fun onClick(position: Int) {
        Log.d(javaClass.simpleName, "Coin $position clicked")
        collectCoin(topFourCoins[position].coinId)
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
}

/**
 * A [RecyclerView.Adapter] for the blind mode. Displays a list of the top users
 */
class BlindModeAdapter(private var coins: List<Coin>,
                       private val getLocation: ()->Location,
                       private val clickCallback: (Int)->Unit,
                       private val activity: Activity) :
        RecyclerView.Adapter<BlindModeAdapter.MyViewHolder>() {
    private val stringIconMap = hashMapOf(
            "DOLR" to R.drawable.ic_dolr,
            "SHIL" to R.drawable.ic_shil,
            "PENY" to R.drawable.ic_peny,
            "QUID" to R.drawable.ic_quid)

    class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): BlindModeAdapter.MyViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.blind_coin_item, parent, false)
        return MyViewHolder(view)
    }

    fun changeData(data: List<Coin>) {
        coins = data
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val coin = coins[position]

        // currency
        holder.view.findViewById<TextView>(R.id.coinType).text = coin.currency
        holder.view.findViewById<ImageView>(R.id.imgCoinIcon).setImageResource(
                stringIconMap.getOrDefault(coin.currency, R.drawable.ic_coinz_24dp)
        )

        // set value to int approximation
        holder.view.findViewById<TextView>(R.id.coinValue).text = coin.amount.toInt().toString()

        // set distance
        holder.view.findViewById<TextView>(R.id.coinDistance).text = "${distanceToCoin(coin, getLocation()).toInt()} metres"

        // set direction
        holder.view.findViewById<TextView>(R.id.coinDirection).text = getDirectionToCoin(coin, getLocation())

        // set collectable
        if (coin.isCollectable(getLocation()))
            holder.view.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorPrimary))
        else
            holder.view.setBackgroundColor(Color.TRANSPARENT)

        holder.view.setOnClickListener {
            clickCallback(position)
        }
    }

    override fun getItemCount() = coins.size
}