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
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import org.json.JSONObject
import java.text.DecimalFormat

class BankFragment : Fragment(), View.OnClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var collectedCoins: MutableList<Coin>
    private lateinit var originalCoins: Coins
    private lateinit var bank: Bank
    private lateinit var progressOverlay: View
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

        originalCoins = (activity as MainActivity).coins

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

        progressOverlay = fragmentView.findViewById(R.id.progress_overlay)
        animateProgressBarIn()

        fillRates()

        fragmentView.findViewById<Button>(R.id.convertQuid).setOnClickListener(this)
        fragmentView.findViewById<Button>(R.id.convertPeny).setOnClickListener(this)
        fragmentView.findViewById<Button>(R.id.convertDolr).setOnClickListener(this)
        fragmentView.findViewById<Button>(R.id.convertShil).setOnClickListener(this)

        val userID = (activity as MainActivity).userID!!
        getOrCreateBank(userID)
        return fragmentView
    }

    private fun getOrCreateBank(userID: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("bank").document(userID).get()
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful || !task.result!!.exists()) {
                        bank = Bank(name=userID)
                        updateBank(bank)
                    } else {
                        Log.d("Firestore", "result: ${task.result}")
                        bank = task.result?.toObject(Bank::class.java)!!
                    }
                    animateProgressBarOut()
                    setBalanceLabels()
                }
                .addOnFailureListener {
                    Log.d("FireStore", "Couldn't get $userID from bank table\n$it")
                    bank = Bank(name=userID)
                    updateBank(bank)
                    animateProgressBarOut()
                    setBalanceLabels()
                }
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

    private fun animateProgressBarIn() {
        val inAnimation = AlphaAnimation(0f, 1f)
        inAnimation.setDuration(200)
        progressOverlay.setAnimation(inAnimation)
        progressOverlay.setVisibility(View.VISIBLE)
        activity!!.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun animateProgressBarOut() {
        val outAnimation = AlphaAnimation(1f, 0f)
        outAnimation.setDuration(200)
        progressOverlay.setAnimation(outAnimation)
        progressOverlay.setVisibility(View.GONE)
        activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun setBalanceLabels() {
        val format = DecimalFormat("####.##")
        fragmentView.findViewById<TextView>(R.id.shilAmount).text = format.format(bank.shilBalance)
        fragmentView.findViewById<TextView>(R.id.quidAmount).text = format.format(bank.quidBalance)
        fragmentView.findViewById<TextView>(R.id.dolrAmount).text = format.format(bank.dolrBalance)
        fragmentView.findViewById<TextView>(R.id.penyAmount).text = format.format(bank.penyBalance)
        fragmentView.findViewById<TextView>(R.id.goldAmount).text = format.format(bank.goldBalance)
    }

    private fun bankCoin(position: Int) {
        val coin = collectedCoins[position]
        // check number banked today
        if (collectedCoins.filter { it.banked }.size > 24) {
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
        // can't notifyItemRemoved because messes with existing callbacks
        viewAdapter.notifyDataSetChanged()
    }

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

class CollectedCoinsAdapter(private val coins: MutableList<Coin>, val callback: (Int) -> Unit) :
        RecyclerView.Adapter<CollectedCoinsAdapter.MyViewHolder>() {

    private val stringIconMap = hashMapOf(
            "DOLR" to R.drawable.ic_dolr,
            "SHIL" to R.drawable.ic_shil,
            "PENY" to R.drawable.ic_peny,
            "QUID" to R.drawable.ic_quid)

    class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): CollectedCoinsAdapter.MyViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.coin_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val coin = coins[position]
        holder.view.findViewById<ImageView>(R.id.coinIcon)
                .setImageResource(stringIconMap.getOrDefault(coin.currency, R.drawable.ic_coinz_24dp))
        holder.view.findViewById<TextView>(R.id.coinType).text = coin.currency
        holder.view.findViewById<TextView>(R.id.coinValue).text = coin.amount.toString()
        holder.view.findViewById<Button>(R.id.bankButton).setOnClickListener {
            callback(position)
        }
    }

    override fun getItemCount() = coins.size
}