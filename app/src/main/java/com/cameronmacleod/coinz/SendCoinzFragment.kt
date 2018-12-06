package com.cameronmacleod.coinz


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import java.util.*

/**
 * Fragment that enables users to send spare change or coins that they haven't yet
 * banked to their friends
 */
class SendCoinzFragment : Fragment(), SendToFriendDialog.NoticeDialogListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var collectedCoins: MutableList<Coin>
    private lateinit var originalCoins: Coins
    private lateinit var fragmentView: View
    // index into collectedCoins
    private var selectedCoinIndex: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_send_coinz, container, false)

        val mainActivity = (activity as MainActivity)

        originalCoins = mainActivity.coins!!

        // obtain a list of collected but not banked coins
        collectedCoins = originalCoins.coins.filter {
            it.collected && !it.banked
        } as MutableList<Coin>

        // show a message if no coins can be sent
        if (collectedCoins.size == 0) {
            fragmentView.findViewById<TextView>(R.id.no_spare_change).visibility = View.VISIBLE
        }

        // Initialise the RecyclerView that displays the coins to send
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

        return fragmentView
    }

    /**
     * Called when SendToFriendDialog gets a valid user ID
     *
     * @param uid The ID of the user to send the coin to
     */
    override fun onUserIDGottenClick(uid: String) {
        val currentUser = (activity as MainActivity).user!!
        if (uid == currentUser.uid) {
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

        // get their coinz to edit it
        getCoinsObject(uid, Calendar.getInstance().time) {
            if (it == null) {
                Log.e(javaClass.simpleName, "Their coins object was null")
                val toast = Toast.makeText(activity, "Something went wrong, please try again later", Toast.LENGTH_SHORT)
                toast.show()
            } else {
                val coin = collectedCoins[copyOfIndex]
                // collect with the coin's location to ensure we are nearby
                it.collectCoinById(coin.coinId, coin.getLocation())
                // WARNING: there are no locks around the db so we could have a race condition
                updateUsersToCoinz(it)

                // update our coin
                coin.banked = true
                updateUsersToCoinz(originalCoins)

                // update adapter dataset
                collectedCoins.removeAt(copyOfIndex)
                viewAdapter.notifyDataSetChanged()

                // notify user
                val toast = Toast.makeText(activity, R.string.coin_send_success, Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }
}
