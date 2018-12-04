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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.DecimalFormat

class LeaderboardFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val fragmentView = inflater.inflate(R.layout.fragment_leaderboard, container, false)

        val mainActivity = (activity as MainActivity)

        mainActivity.animateProgressBarIn()

        val db = FirebaseFirestore.getInstance()

        db.collection("bank")
                .orderBy("goldBalance", Query.Direction.DESCENDING)
                .limit(10).get()
                .addOnCompleteListener {
                    val banks = it.result?.toObjects(Bank::class.java)

                    if (banks == null) {
                        Log.e(javaClass.simpleName, "Couldn't get banks")
                    } else {
                        viewManager = LinearLayoutManager(activity)
                        viewAdapter = LeaderboardAdapter(banks)

                        recyclerView = fragmentView.findViewById<RecyclerView>(R.id.topPlayersList).apply {
                            // improves performance since each element is same size
                            setHasFixedSize(true)
                            layoutManager = viewManager
                            adapter = viewAdapter
                        }

                        mainActivity.animateProgressBarOut()
                    }
                }

        return fragmentView
    }
}

class LeaderboardAdapter(private val banks: MutableList<Bank>) :
        RecyclerView.Adapter<LeaderboardAdapter.MyViewHolder>() {

    class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): LeaderboardAdapter.MyViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.leaderboard_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val bank = banks[position]
        val format = DecimalFormat("#")
        holder.view.findViewById<TextView>(R.id.position).text = (position + 1).toString()
        holder.view.findViewById<TextView>(R.id.emailAddress).text = bank.email
        holder.view.findViewById<TextView>(R.id.goldBalance).text = format.format(bank.goldBalance)
    }

    override fun getItemCount() = banks.size
}