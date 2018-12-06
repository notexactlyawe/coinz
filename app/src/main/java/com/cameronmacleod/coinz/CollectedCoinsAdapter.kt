package com.cameronmacleod.coinz

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

/**
 * A [RecyclerView.Adapter] for use when displaying a list of collected coins
 *
 * @property coins The coins to display
 * @property callback A function called whenever a [R.id.collectedActionButton] is pressed, takes
 *  an integer which is the index into [coins] for which coin was clicked
 */
class CollectedCoinsAdapter(private val coins: MutableList<Coin>, val callback: (Int) -> Unit) :
        RecyclerView.Adapter<CollectedCoinsAdapter.MyViewHolder>() {

    // A map from currency strings to drawable icons representing them
    private val stringIconMap = hashMapOf(
            "DOLR" to R.drawable.ic_dolr,
            "SHIL" to R.drawable.ic_shil,
            "PENY" to R.drawable.ic_peny,
            "QUID" to R.drawable.ic_quid)

    // The object that contains each item of the RecyclerView
    class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    /**
     * Called whenever a ViewHolder needs to be created. Inflates [R.layout.coin_item]
     */
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): CollectedCoinsAdapter.MyViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.coin_item, parent, false)
        return MyViewHolder(view)
    }

    /**
     * Called to populate the data inside a ViewHolder
     */
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val coin = coins[position]
        holder.view.findViewById<ImageView>(R.id.coinIcon)
                .setImageResource(stringIconMap.getOrDefault(coin.currency, R.drawable.ic_coinz_24dp))
        holder.view.findViewById<TextView>(R.id.coinType).text = coin.currency
        holder.view.findViewById<TextView>(R.id.coinValue).text = coin.amount.toString()
        holder.view.findViewById<TextView>(R.id.receivedIndicator).text = if (coin.received) "R" else " "
        holder.view.findViewById<Button>(R.id.collectedActionButton).setOnClickListener {
            callback(position)
        }
    }

    override fun getItemCount() = coins.size
}