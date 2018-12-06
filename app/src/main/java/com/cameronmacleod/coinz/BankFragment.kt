package com.cameronmacleod.coinz


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.json.JSONException
import org.json.JSONObject
import java.text.DecimalFormat

/**
 * Fragment that handles banking for the user. That is putting coins into the bank and also
 * exchanging coins that are already in the bank
 */
class BankFragment : Fragment(), View.OnClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var collectedCoins: MutableList<Coin>
    private lateinit var originalCoins: Coins
    private lateinit var bank: Bank
    private lateinit var fragmentView: View
    private var exchangeRates = hashMapOf(
            "DOLR" to 0.0,
            "SHIL" to 0.0,
            "QUID" to 0.0,
            "PENY" to 0.0)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_bank, container, false)

        // get a reference to the overall coins object
        val mainActivity = (activity as MainActivity)
        originalCoins = mainActivity.coins!!

        // get all coins that are collected but not banked to display to the user
        collectedCoins = originalCoins.coins.filter {
            it.collected && !it.banked
        } as MutableList<Coin>

        viewManager = LinearLayoutManager(activity)
        viewAdapter = CollectedCoinsAdapter(collectedCoins, ::bankCoin)

        recyclerView = fragmentView.findViewById<RecyclerView>(R.id.collectedCoinsList).apply {
            // improves performance since each element is same size
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        // set loading spinner whilst we get the user's bank from firestore
        mainActivity.animateProgressBarIn()

        fillRates()

        fragmentView.findViewById<Button>(R.id.convertQuid).setOnClickListener(this)
        fragmentView.findViewById<Button>(R.id.convertPeny).setOnClickListener(this)
        fragmentView.findViewById<Button>(R.id.convertDolr).setOnClickListener(this)
        fragmentView.findViewById<Button>(R.id.convertShil).setOnClickListener(this)

        val user = mainActivity.user!!
        getOrCreateBank(user.uid, user.email!!) { bank ->
            this.bank = bank
            mainActivity.animateProgressBarOut()
            setBalanceLabels()
        }
        return fragmentView
    }

    /**
     * Gets the exchange rates from the JSON stored in shared preferences
     *
     * Needed for exchanging coins based on today's rates
     *
     * TODO: Extract to helper function so can be shared with MainFragment
     */
    private fun fillRates() {
        val sharedPrefs = activity?.getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE) ?: return
        val geojsonStr = sharedPrefs.getString(getString(R.string.coinz_map_key), "")
        val geojson = JSONObject(geojsonStr)

        try {
            val rates = geojson.getJSONObject("rates")

            exchangeRates["SHIL"] = rates.getString("SHIL").toDouble()
            exchangeRates["DOLR"] = rates.getString("DOLR").toDouble()
            exchangeRates["QUID"] = rates.getString("QUID").toDouble()
            exchangeRates["PENY"] = rates.getString("PENY").toDouble()
        } catch (e: JSONException) {
            val toast = Toast.makeText(activity, R.string.no_geojson_toast, Toast.LENGTH_LONG)
            toast.show()
            Log.e(this.javaClass.simpleName, "Error getting rates from geojson: ${e.localizedMessage}")
        }
    }

    /**
     * Displays the user's bank balance
     */
    private fun setBalanceLabels() {
        val format = DecimalFormat("####.##")
        fragmentView.findViewById<TextView>(R.id.shilAmount).text = format.format(bank.shilBalance)
        fragmentView.findViewById<TextView>(R.id.quidAmount).text = format.format(bank.quidBalance)
        fragmentView.findViewById<TextView>(R.id.dolrAmount).text = format.format(bank.dolrBalance)
        fragmentView.findViewById<TextView>(R.id.penyAmount).text = format.format(bank.penyBalance)
        fragmentView.findViewById<TextView>(R.id.goldAmount).text = format.format(bank.goldBalance)
    }

    /**
     * Banks a coin
     *
     * @param position Index into [collectedCoins] of the coin to bank
     */
    private fun bankCoin(position: Int) {
        val coin = collectedCoins[position]
        // check number banked today

        val numBankedCoins = originalCoins.coins.filter { it.banked }.size

        Log.d(javaClass.simpleName, "In bankCoin, number of banked coins $numBankedCoins")

        if (numBankedCoins > 24 && !coin.received) {
            val toast = Toast.makeText(activity, "Already banked 25 coins today!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        // stick coin in bank table
        when (coin.currency) {
            "DOLR" -> bank.dolrBalance += coin.amount
            "SHIL" -> bank.shilBalance += coin.amount
            "PENY" -> bank.penyBalance += coin.amount
            "QUID" -> bank.quidBalance += coin.amount
        }

        updateBank(bank)

        // update display of balance
        setBalanceLabels()

        // mark coin as banked
        coin.banked = true

        updateUsersToCoinz(originalCoins)

        // refresh the adapter
        collectedCoins.removeAt(position)
        // can't notifyItemRemoved because need to rebind existing button callbacks
        viewAdapter.notifyDataSetChanged()
    }

    /**
     * Function to handle all button presses for the fragment
     *
     * By default button presses will go to the activity, but since [BankFragment] implements
     * [View.OnClickListener] we can redirect presses to us using this function
     *
     * @param view The view that was clicked
     */
    override fun onClick(view: View) {
        when (view.id) {
            R.id.convertDolr -> {
                bank.goldBalance +=
                        bank.dolrBalance * exchangeRates.getOrDefault("DOLR", 0.0)
                bank.dolrBalance = 0.0
            }
            R.id.convertShil -> {
                bank.goldBalance +=
                        bank.shilBalance * exchangeRates.getOrDefault("SHIL", 0.0)
                bank.shilBalance = 0.0
            }
            R.id.convertPeny -> {
                bank.goldBalance +=
                        bank.penyBalance * exchangeRates.getOrDefault("PENY", 0.0)
                bank.penyBalance = 0.0
            }
            R.id.convertQuid -> {
                bank.goldBalance +=
                        bank.quidBalance * exchangeRates.getOrDefault("QUID", 0.0)
                bank.quidBalance = 0.0
            }
        }

        updateBank(bank)
        setBalanceLabels()
    }
}