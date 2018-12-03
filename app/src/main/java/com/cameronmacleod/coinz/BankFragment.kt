package com.cameronmacleod.coinz


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

class BankFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var collectedCoins: MutableList<Coin>
    private lateinit var originalCoins: Coins
    private lateinit var bank: Bank
    private lateinit var progressOverlay: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_bank, container, false)

        originalCoins = (activity as MainActivity).coins

        collectedCoins = originalCoins.coins.filter {
            it.collected && !it.banked
        } as MutableList<Coin>

        viewManager = LinearLayoutManager(activity)
        viewAdapter = CollectedCoinsAdapter(collectedCoins, ::bankCoin)

        recyclerView = view.findViewById<RecyclerView>(R.id.collectedCoinsList).apply {
            // improves performance since each element is same size
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        progressOverlay = view.findViewById(R.id.progress_overlay)
        animateProgressBarIn()

        val userID = (activity as MainActivity).userID!!
        getOrCreateBank(userID)
        return view
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
                }
                .addOnFailureListener {
                    Log.d("FireStore", "Couldn't get $userID from bank table\n$it")
                    bank = Bank(name=userID)
                    updateBank(bank)
                    animateProgressBarOut()
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

        // mark coin as banked
        coin.banked = true

        updateUsersToCoinz(originalCoins)

        // refresh the adapter
        collectedCoins.removeAt(position)
        // can't notifyItemRemoved because messes with existing callbacks
        viewAdapter.notifyDataSetChanged()
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