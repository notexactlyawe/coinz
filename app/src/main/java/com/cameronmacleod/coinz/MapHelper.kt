package com.cameronmacleod.coinz

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MapHelper(context: Context) {
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
                    Log.e("MapHelper", error.localizedMessage)
                }
                )
        queue.add(jsonObjectRequest)
    }
}