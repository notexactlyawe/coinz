package com.cameronmacleod.coinz


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class SendCoinzFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var collectedCoins: MutableList<Coin>
    private lateinit var originalCoins: Coins
    private lateinit var bank: Bank
    private lateinit var fragmentView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_send_coinz, container, false)

        val mainActivity = (activity as MainActivity)

        originalCoins = mainActivity.coins!!

        collectedCoins = originalCoins.coins.filter {
            it.collected && !it.banked
        } as MutableList<Coin>

        viewManager = LinearLayoutManager(activity)
        viewAdapter = CollectedCoinsAdapter(collectedCoins) {
            Log.d("Button clicked", "position was $it")
        }

        recyclerView = fragmentView.findViewById<RecyclerView>(R.id.collectedCoinsList).apply {
            // improves performance since each element is same size
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        mainActivity.animateProgressBarIn()

        val userID = (activity as MainActivity).userID!!
        getOrCreateBank(userID) {
            this.bank = it
            mainActivity.animateProgressBarOut()
        }
        return fragmentView
    }
}
