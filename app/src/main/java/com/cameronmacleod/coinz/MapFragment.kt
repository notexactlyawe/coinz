package com.cameronmacleod.coinz

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.v4.content.ContextCompat

class MapFragment : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var dolrBitmap: Bitmap
    private lateinit var shilBitmap: Bitmap
    private lateinit var quidBitmap: Bitmap
    private lateinit var penyBitmap: Bitmap
    private lateinit var collBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity != null) {
            val nonNullActivity = activity as Activity
            Mapbox.getInstance(nonNullActivity.applicationContext, "pk.eyJ1Ijoibm90ZXhhY3RseWF3ZSIsImEiOiJjam92enp0b2sxbjdqM3JucmJxbGcxbzI1In0.zNr5YfYwwBzpkp_ReEw2-w");

            // create bitmaps early so that they're only created once
            dolrBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_dolr)
            shilBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_shil)
            quidBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_quid)
            penyBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_peny)
            collBitmap = getBitmapFromVectorDrawable(nonNullActivity, R.drawable.ic_coinz_24dp)
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

            mapView.getMapAsync { mapboxMap ->
                try {
                    val parent = activity as MainActivity
                    parent.coins.coins.forEach { coin ->
                        val m = MarkerOptions().apply {
                            position = LatLng(coin.latitude, coin.longitude)
                            title = coin.currency
                            snippet = coin.amount.toInt().toString()
                            icon = getIcon(coin.collected, coin.currency)
                        }
                        mapboxMap.addMarker(m)
                    }
                } catch (e: Exception) {
                    Log.e(this.javaClass.simpleName, "Couldn't create GeoJSON source ${e.printStackTrace()}")
                }
            }
        }
    }

    fun getIcon(collected: Boolean, currency: String): Icon {
        val icon = IconFactory.getInstance(activity!!)
        var bitmap: Bitmap?
        if (collected) {
            bitmap = collBitmap
            return icon.fromBitmap(bitmap)
        }
        when (currency) {
            "SHIL" -> bitmap = shilBitmap
            "QUID" -> bitmap = quidBitmap
            "DOLR" -> bitmap = dolrBitmap
            "PENY" -> bitmap = penyBitmap
            else -> {
                bitmap = collBitmap
            }
        }
        return icon.fromBitmap(bitmap)
    }

    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        var drawable = ContextCompat.getDrawable(context, drawableId)!!

        val bitmap = Bitmap.createBitmap(100,
                100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)

        return bitmap
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
