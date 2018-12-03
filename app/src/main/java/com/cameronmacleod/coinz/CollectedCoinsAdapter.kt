package com.cameronmacleod.coinz

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

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
        holder.view.findViewById<Button>(R.id.collectedActionButton).setOnClickListener {
            callback(position)
        }
    }

    override fun getItemCount() = coins.size
}