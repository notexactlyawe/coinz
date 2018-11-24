package com.cameronmacleod.coinz

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONException
import org.json.JSONObject

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OnFragmentInteractionListener] interface
 * to handle interaction events.
 *
 */
class MainFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.content_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillRates()
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

            shil_conversion_rate.text = rates.getString("SHIL")
            dolr_conversion_rate.text = rates.getString("DOLR")
            quid_conversion_rate.text = rates.getString("QUID")
            peny_conversion_rate.text = rates.getString("PENY")
        } catch (e: JSONException) {
            val toast = Toast.makeText(activity, R.string.no_geojson_toast, Toast.LENGTH_LONG)
            toast.show()
            Log.e(this.javaClass.simpleName, "Error getting rates from geojson: ${e.localizedMessage}")
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
