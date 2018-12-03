package com.cameronmacleod.coinz


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class BankFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_bank, container, false)

        val collectedCoins = (activity as MainActivity).coins.coins.filter {
            it.collected
        }

        viewManager = LinearLayoutManager(activity)
        viewAdapter = CollectedCoinsAdapter(collectedCoins)

        recyclerView = view.findViewById<RecyclerView>(R.id.collectedCoinsList).apply {
            // improves performance since each element is same size
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
        return view
    }
}

class CollectedCoinsAdapter(private val coins: List<Coin>) :
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
            Log.d("Bound button", "Button on position $position pressed")
        }
    }

    override fun getItemCount() = coins.size
}