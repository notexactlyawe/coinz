package com.cameronmacleod.coinz

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import org.json.JSONObject


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OnFragmentInteractionListener] interface
 * to handle interaction events.
 *
 */
class MapFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity != null) {
            val nonNullActivity = activity as Activity
            Mapbox.getInstance(nonNullActivity.applicationContext, "pk.eyJ1Ijoibm90ZXhhY3RseWF3ZSIsImEiOiJjam92enp0b2sxbjdqM3JucmJxbGcxbzI1In0.zNr5YfYwwBzpkp_ReEw2-w");
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity != null) {
            val nonNullActivity = activity as Activity
            mapView = nonNullActivity.findViewById(R.id.mapView) as MapView
            mapView.onCreate(savedInstanceState)

            val sharedPrefs = nonNullActivity.getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE)

            mapView.getMapAsync { mapboxMap ->
                try {
                    val json = sharedPrefs.getString(getString(R.string.coinz_map_key), "")
                    val markerOptions = FeatureCollection.fromJson(json).features()?.forEach {
                        val coords = (it.geometry() as Point).coordinates()
                        val m = MarkerOptions().apply {
                            position = LatLng(coords[1], coords[0])
                            title = it.properties()?.get("currency")?.asString
                            snippet = it.properties()?.get("marker-symbol")?.asString
                        }
                        mapboxMap.addMarker(m)
                    }
                } catch (e: Exception) {
                    Log.e(this.javaClass.simpleName, "Couldn't create GeoJSON source ${e.printStackTrace()}")
                }
            }
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

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

}
