package com.babooin.si3

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
//import kotlinx.coroutines.CoroutineScope
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
//import java.lang.Exception

class PositionsFragment : Fragment() {
    private var docID:String? = null
    private var docNumber:String? = null
    private lateinit var params:Bundle
    private lateinit var request:Request

    private lateinit var client: OkHttpClient
    private lateinit var lv: ListView
    private lateinit var contextF: Context

    private lateinit var navController: NavController


    override fun onAttach(context: Context) {
        contextF = context
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        params = Bundle()
        arguments?.let {
            docID = it.getString("DocID")
            docNumber = it.getString("DocNumber")
            val ip = it.getString("IP_ADDRESS_SERVER")
            params.putString("IP_ADDRESS_SERVER", ip)
            params.putString("orders", it.getString("orders"))
            params.putString("type", it.getString("type"))

        }
        client = OkHttpClient()
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.confirm_positions_menuitem -> {
                confirmAction()
                true
            }


            R.id.refresh_positions_menuitem -> {
                getPositions(docID.toString())
                true
            }

            android.R.id.home -> {
                navController.navigate(R.id.action_positionsFragment_to_docListFragment)
               true
            }
            R.id.add_doc_separate -> {
                separateAction()
            true
            }

            else -> false
        }
    }


    private fun separateAction() {
        val builder = AlertDialog.Builder(contextF)
        builder.setTitle( getString(R.string.separate_title))
        builder.setMessage(getString(R.string.separate_body))
        builder.setPositiveButton(R.string.yes) { _, _ ->
            separateDocument(arguments?.getString("DocID").toString())
        }

        builder.setNegativeButton(R.string.no) { _, _ ->
            requireActivity().runOnUiThread {
                Toast.makeText(contextF,
                        R.string.no, Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    private fun  separateDocument(docID: String) {
        val act = (activity as MainActivity)
        val body = FormBody.Builder()
                .add("id", docID)
                .build()


        val paramIB =  (activity as MainActivity).prefs.getString("ibSession","")
        request = if (paramIB=="") {
            Request.Builder()
                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$docID&action=separate")
                .addHeader("IBSession","start")
                .post(body)
                .build()
        } else {
            Request.Builder()
                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$docID&action=separate")
                .addHeader("Cookie","IBSession=${paramIB}")
                .post(body)
                .build()
        }


//        request = Request.Builder()
//                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$docID&action=separate")
//                .post(body)
//                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    httpError(e.stackTrace.toString(),"separate")
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        if (response.code()==404) {
                            (activity as MainActivity).prefs.edit().remove("ibSession").apply() //{putString(, "")}
                            separateDocument(docID)
                            Log.d("ID",response.code().toString())
                        } else {
                        activity!!.runOnUiThread {
                            httpError(response.toString(), "separate")
                        }
                        }
                    } else {
                        //                val jso = JSONObject(response.body!!.string())

                        val jso = JSONObject(response.body()?.string().toString())
                        activity!!.runOnUiThread {Toast.makeText(context, jso.toString(), Toast.LENGTH_LONG).show()
                            act.navController.navigate(R.id.action_positionsFragment_to_docListFragment)

                        }

                    }}}})

    }




    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.positions_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_positions, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val act =   (activity as MainActivity)
        act.supportActionBar?.title = docNumber
        act.supportActionBar?.subtitle = docID
        act.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
        act.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        super.onViewCreated(view, savedInstanceState)
        lv = view.findViewById(R.id.positionsListView)
        lv.setOnItemClickListener { parent, _, pos, _ ->  openEditPosition(parent.adapter.getItem(pos) as CPositions)
        }
        navController = Navigation.findNavController(view)
        val rl = view.findViewById<SwipeRefreshLayout>(R.id.refresh_positions_layout)
        rl.setOnRefreshListener {
            rl.isRefreshing = false
            getPositions(docID.toString())
        }
    }

     private fun openEditPosition(selected: CPositions) {
         params.putString("DocID", docID)
         params.putString("BarCode", selected.barcode)
         params.putString("Number", arguments?.getString("Number"))
         navController.navigate(R.id.action_positionsFragment_to_editFragment,params)
     }


        private fun httpError(message: String, action:String) {
//            val act =  (activity as MainActivity)
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.dialog_error_title))
            builder.setMessage(message)
            builder.setPositiveButton(R.string.dialog_retry) { _, _ ->
               when (action) {
                    "get" -> getPositions(docID.toString())
                   "confirm" ->confirmDocument(docID.toString())
              //     "delete" -> deleteDocument(requireView().context)
                   "separate" -> separateDocument(docID.toString())
               }
            }
            builder.setNegativeButton(R.string.dialog_exit) { _, _ ->

                (activity as MainActivity).navController.navigate(R.id.action_positionsFragment_to_docListFragment)
            }
            builder.show()
        }


    private val keyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
//            val chr = intent?.extras?.getString("char")
            when (intent?.extras?.getInt("keycode")) {
                KeyEvent.KEYCODE_ESCAPE,  KeyEvent.KEYCODE_BACK -> {
                    if  (navController.currentDestination?.label == "fragment_positions" ) {
                        navController.navigate(R.id.action_positionsFragment_to_docListFragment, params)
                    }
                }
            }
        }
    }


    private val broadCastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
//            println("intent ${intent.toString()}")
            when (intent?.action) {
                "android.intent.ACTION_DECODE_DATA" -> {
                    val bent = intent.getStringExtra("barcode_string")
                    params.putString("DocID", docID)
                    params.putString("BarCode", bent)

                            navController.navigate(R.id.action_positionsFragment_to_editFragment, params)
                    }
                } } }


        override fun onResume() {
            requireActivity().registerReceiver(broadCastReceiver, IntentFilter("android.intent.ACTION_DECODE_DATA"))
            LocalBroadcastManager.getInstance(contextF).registerReceiver(keyReceiver, IntentFilter("key_press_intent"))

            super.onResume()
            getPositions(arguments?.get("DocID").toString())
        }

        override fun onPause() {
            requireActivity().unregisterReceiver(broadCastReceiver)
                LocalBroadcastManager.getInstance(contextF).unregisterReceiver(keyReceiver)
                super.onPause()

    }

        private fun JSONArray.toMutableList(): List<JSONObject> = List(length(), this::getJSONObject)

        private fun  getPositions(docID:String){
         //   val act = (activity as MainActivity)

            val paramIB =  (activity as MainActivity).prefs.getString("ibSession","")
            request = if (paramIB=="") {
                Request.Builder()
                    .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$docID")
                    .addHeader("IBSession","start")
                    .build()
            } else {
                Request.Builder()
                    .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$docID")
                    .addHeader("Cookie","IBSession=${paramIB}")
                    .build()
            }

//            request = Request.Builder()
//            .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$docID").build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity!!.runOnUiThread {
                        httpError(e.stackTrace.toString(),"get")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            if (response.code()==404) {
                                (activity as MainActivity).prefs.edit().remove("ibSession").apply() //{putString(, "")}
                                getPositions(docID)
                                Log.d("ID",response.code().toString())
                            } else {
                            activity!!.runOnUiThread {
                                httpError(response.toString(),"get")
                            }}
                        } else {

                            val jsa = JSONArray(response.body()?.string().toString()).toMutableList()
                            val cPos = arrayListOf<CPositions>()

                            jsa.forEach {
if (params.getString("type")=="Плановый"){
                                cPos.add(
                                    CPositions(
                                        it.getString("Code"),
                                        it.getString("view"),
                                        it.getString("barcode"),
                                        it.getDouble("amount"),
                                        it.getDouble("plan_amount"),
                                        )
                                )
} else {
    cPos.add(
        CPositions(
            it.getString("Code"),
            it.getString("view"),
            it.getString("barcode"),
            it.getDouble("amount"),
            0.0
        )
    )

}

                            }

                            val adapter = PositionsAdapter(contextF, cPos, params.getString("type").toString())
                            activity!!.runOnUiThread { lv.adapter = adapter }
                        }}}})}





        private fun confirmAction(){
            val builder = AlertDialog.Builder(contextF)
            builder.setTitle( getString(R.string.confirm_title))
            builder.setMessage(getString(R.string.confirm_body))
    //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

            builder.setPositiveButton(R.string.yes) { _, _ ->
                confirmDocument(arguments?.getString("DocID").toString())
            }

            builder.setNegativeButton(R.string.no) { _, _ ->
    //            Toast.makeText(contextF,
    //                R.string.no, Toast.LENGTH_SHORT).show()
            }

    //        builder.setNeutralButton("Maybe") { dialog, which ->
    //            Toast.makeText(applicationContext,
    //                "Maybe", Toast.LENGTH_SHORT).show()
    //        }
            builder.show()
        }


        private fun  confirmDocument(docID:String) {
            val act = (activity as MainActivity)
            val body = FormBody.Builder()
                .add("id", docID)
                .build()


            val paramIB =  (activity as MainActivity).prefs.getString("ibSession","")
            request = if (paramIB=="") {
                Request.Builder()
                    .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$docID&action=finish")
                    .addHeader("IBSession","start")
                    .post(body)
                    .build()
            } else {
                Request.Builder()
                    .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$docID&action=finish")
                    .addHeader("Cookie","IBSession=${paramIB}")
                    .post(body)
                    .build()
            }

//             request = Request.Builder()
//                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$docID&action=finish")
//                .post(body)
//                .build()



            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity!!.runOnUiThread {
                        httpError(e.stackTrace.toString(),"confirm")
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            if (response.code()==404) {
                                (activity as MainActivity).prefs.edit().remove("ibSession").apply()
                                confirmDocument(docID)
                                Log.d("ID",response.code().toString())
                            } else {
                            activity!!.runOnUiThread {
                                httpError(response.toString(),"confirm")
                            }}
                        } else {
                            //                val jso = JSONObject(response.body!!.string())

                            val jso = JSONObject(response.body()?.string().toString())
                            activity!!.runOnUiThread {Toast.makeText(context, jso.toString(), Toast.LENGTH_LONG).show()
                                act.navController.navigate(R.id.action_positionsFragment_to_docListFragment)

                            }

                        }}}})

        }

}



