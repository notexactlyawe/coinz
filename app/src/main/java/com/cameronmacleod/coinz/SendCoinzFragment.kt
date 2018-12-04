package com.cameronmacleod.coinz


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

class SendCoinzFragment : Fragment(), SendToFriendDialog.NoticeDialogListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var collectedCoins: MutableList<Coin>
    private lateinit var originalCoins: Coins
    private lateinit var bank: Bank
    private lateinit var fragmentView: View
    private var selectedCoinIndex: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_send_coinz, container, false)

        val mainActivity = (activity as MainActivity)

        originalCoins = mainActivity.coins!!

        collectedCoins = originalCoins.coins.filter {
            it.collected && !it.banked
        } as MutableList<Coin>

        if (collectedCoins.size == 0) {
            fragmentView.findViewById<TextView>(R.id.no_spare_change).visibility = View.VISIBLE
        }

        viewManager = LinearLayoutManager(activity)
        viewAdapter = CollectedCoinsAdapter(collectedCoins) {
            Log.d("Button clicked", "position was $it")
            selectedCoinIndex = it
            val emailDialog = SendToFriendDialog()
            emailDialog.setTargetFragment(this, 0)
            emailDialog.show(fragmentManager, "SendToFriendDialog")
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

    override fun onUserIDGottenClick(uid: String) {
        val currentUserId = (activity as MainActivity).userID
        if (uid == currentUserId) {
            val toast = Toast.makeText(activity, R.string.send_coin_to_self, Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        if (selectedCoinIndex == null) {
            Log.e(javaClass.simpleName, "No coin was selected when dialog returned")
            return
        }

        // copy in case another coin sent in mean time
        val copyOfIndex = selectedCoinIndex as Int

        getOrCreateBank(uid) { theirBank ->
            val coin = collectedCoins[copyOfIndex]
            when (coin.currency) {
                "DOLR" -> theirBank.dolrBalance += coin.amount
                "SHIL" -> theirBank.shilBalance += coin.amount
                "PENY" -> theirBank.penyBalance += coin.amount
                "QUID" -> theirBank.quidBalance += coin.amount
            }
            // WARNING: there are no locks around a 'bank' so we could have a race condition
            updateBank(theirBank)

            // update our coin
            coin.banked = true

            // update adapter dataset
            collectedCoins.removeAt(copyOfIndex)
            viewAdapter.notifyDataSetChanged()

            // notify user
            val toast = Toast.makeText(activity, R.string.coin_send_success, Toast.LENGTH_SHORT)
            toast.show()
        }
    }
}
