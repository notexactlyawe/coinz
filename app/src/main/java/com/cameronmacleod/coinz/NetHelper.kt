package com.cameronmacleod.coinz

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NetHelper(context: Context) {
    private val queue = Volley.newRequestQueue(context)
    private val rootUrl = "http://homepages.inf.ed.ac.uk/stg/coinz/"
    private val fileName = "coinzmap.geojson"

    fun getJSONForDay(day: Date, onSuccess: (JSONObject)->Unit){
        val format = SimpleDateFormat("yyyy/MM/dd/")
        val url = rootUrl + format.format(day) + fileName

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    onSuccess(response)
                },
                Response.ErrorListener { error ->
                    Log.e(javaClass.simpleName, error.localizedMessage)
                }
                )
        queue.add(jsonObjectRequest)
    }

    fun getProfilePicture(url: String, scaleType: ImageView.ScaleType, onSuccess: (Bitmap)->Unit) {
        Log.d(javaClass.simpleName, "Creating image request")
        val request = ImageRequest(url,
                Response.Listener {bitmap ->
                    Log.d(javaClass.simpleName, "Successfully got bitmap from $url")
                    onSuccess(bitmap)
                }, 0,0, scaleType, null,
                Response.ErrorListener {error ->
                    Log.e(javaClass.simpleName, error.localizedMessage)
                })
        queue.add(request)
    }
}