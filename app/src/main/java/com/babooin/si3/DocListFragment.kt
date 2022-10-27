package com.babooin.si3

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.animation.BounceInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.baoyz.swipemenulistview.SwipeMenuCreator
import com.baoyz.swipemenulistview.SwipeMenuItem
import com.baoyz.swipemenulistview.SwipeMenuListView
import com.baoyz.swipemenulistview.SwipeMenuListView.OnSwipeListener
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern


class DocListFragment : Fragment() {
    private lateinit var lv:SwipeMenuListView
    private lateinit var client: OkHttpClient
    private  lateinit var navController:NavController
    private  lateinit var request:Request
    private var selectedDocId:String =""
    private lateinit var params:Bundle
    lateinit var contextF:Context


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        params = Bundle()
        client = OkHttpClient()
        (activity as MainActivity).prefs = (activity as AppCompatActivity).getSharedPreferences(
            "settings",
            Context.MODE_PRIVATE
        )
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh_doc_list_menuitem -> {
                getDocuments()
                true
            }

            R.id.add_doc_list_menuitem -> {
                newDocument()
                true
            }
            R.id.settings_menuitem -> {
                navController.navigate(R.id.action_docListFragment_to_settingsFragment)
                true
            }



            else -> false
        }
//        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.doc_list_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_doc_list, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        lv = view.findViewById(R.id.docListView) as SwipeMenuListView
        lv.setOnItemClickListener { parent, _, pos, _ ->
            val selected = parent.adapter.getItem(pos) as CDocuments
            params.putString("DocID", selected.id)
            params.putString("DocNumber", selected.Number)
            params.putString("type", selected.type)
            navController.navigate(R.id.action_docListFragment_to_positionsFragment, params)
        }


        val rl = view.findViewById<SwipeRefreshLayout>(R.id.refresh_docList_layout)
        rl.setOnRefreshListener {
            rl.isRefreshing = false
            getDocuments()
        }
        (activity as MainActivity).supportActionBar?.title = getString(R.string.app_name)
        (activity as MainActivity).supportActionBar?.subtitle = null
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)


        val creator = SwipeMenuCreator { menu -> // create "open" item
            val deleteItem = SwipeMenuItem(contextF)
            deleteItem.background = ColorDrawable(
                Color.rgb(
                    0xF9,
                    0x3F, 0x25
                )
            )
            deleteItem.width = dp2px(90)
            deleteItem.setIcon(R.drawable.ic_delete)
            menu.addMenuItem(deleteItem)
        }
        lv.setMenuCreator(creator)
        lv.closeInterpolator = BounceInterpolator()
        lv.setOnSwipeListener(object : OnSwipeListener {
            override fun onSwipeStart(position: Int) {
                lv.smoothOpenMenu(position)
            }

            override fun onSwipeEnd(position: Int) {
                // swipe end
            }
        })

        lv.setOnMenuItemClickListener { position, _, index ->
            when (index) {
                0 -> {
                    val v: CDocuments = lv.adapter.getItem(position) as CDocuments
                    selectedDocId = v.id
                    deleteAction(selectedDocId)
//                    false
                }
                1 -> {
//                    false
                }
            }
            // false : close the menu; true : not close the menu
            false
        }

    }

    private fun dp2px(dp: Int=80): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun deleteAction(id: String){
        val builder = AlertDialog.Builder(contextF)
        builder.setTitle(getString(R.string.del_title))
        builder.setMessage(getString(R.string.del_body))
        builder.setPositiveButton(R.string.yes) { _, _ ->
            deleteDocument(id)
        }

        builder.setNegativeButton(R.string.no) { _, _ ->
            requireActivity().runOnUiThread {
                Toast.makeText(
                    contextF,
                    R.string.no, Toast.LENGTH_SHORT
                ).show()
            }
        }
        builder.show()
    }

    private fun deleteDocument( id: String) { //.post(body)
//        val act  =(activity as MainActivity)
        val body = FormBody.Builder()
            .add("id", id)
            .build()
        val paramIB =  (activity as MainActivity).prefs.getString("ibSession","")
        request = if (paramIB=="") {
            Request.Builder()
                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$id")
                .addHeader("IBSession","start")
                .delete(body)
                .build()
        } else {
            Request.Builder()
                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$id")
                .addHeader("Cookie","IBSession=${paramIB}") //Cookie: IBSession=43CD8102-F970-4FFB-939A-C47948F49885
                .delete(body)
                .build()
        }

//        request = Request.Builder()
//            .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=$id")
//            .delete(body)
//            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    httpError(e.stackTrace.toString(), "delete")
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        if (response.code()==404) {
                            (activity as MainActivity).prefs.edit().remove("ibSession").apply() //{putString(, "")}
                            deleteDocument(id)
                            Log.d("ID",response.code().toString())
                        } else {
                        activity!!.runOnUiThread {
                            httpError(response.toString(), "delete")
                        }}
                    } else {
                        //                val jso = JSONObject(response.body!!.string())

//                        val jso = JSONObject(response.body()?.string().toString())
                        activity!!.runOnUiThread {
                            //Toast.makeText(context, jso.toString(), Toast.LENGTH_LONG).show()
                            //act.navController.navigate(R.id.action_positionsFragment_to_docListFragment)
                            getDocuments()
                        }

                    }
                }
            }
        })
    }


    private fun httpError(message: String, action: String) {
//            val act =  (activity as MainActivity)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.dialog_error_title))
        builder.setMessage(message)
//        builder.setNeutralButton("")
        builder.setPositiveButton(R.string.dialog_retry) { _, _ ->
            when (action) {
                "settings" -> getDocuments()
                "delete" -> deleteDocument(selectedDocId)
              //  "separate" -> separateDocument(docID.toString())
            }
        }
        builder.setNegativeButton(R.string.dialog_exit) { _, _ ->
            requireActivity().finish()
        }
        builder.show()
    }

    private  fun JSONArray.toMutableList(): List<JSONObject> = List(length(), this::getJSONObject)

    private fun getDocuments(){
//        val act = activity as MainActivity
        val paramIB =  (activity as MainActivity).prefs.getString("ibSession","")

try {
    request = if (paramIB == "") {
        Request.Builder()
            .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&orders=${params.getBoolean("ENABLE_ORDERS")}")
            .addHeader("IBSession", "start")
            .build()
    } else {
        Request.Builder()
            .url(
                "${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&orders=${
                    params.getBoolean(
                        "ENABLE_ORDERS"
                    )
                }"
            )
            .addHeader("Cookie", "IBSession=${paramIB}")
            .build()
    }

}
catch (err:IOException) {
    requireActivity().runOnUiThread {
        err.localizedMessage?.let { settingsError(it) }
    }
}

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    httpError(e.stackTraceToString(), "settings")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        if (response.code()==404) {
                            (activity as MainActivity).prefs.edit().remove("ibSession").apply() //{putString(, "")}
                            getDocuments()
                            Log.d("ID",response.code().toString())
                        } else {
                        activity!!.runOnUiThread {
                            httpError(response.toString(), "settings")
                        }
                        }
                    }
                  else {
                        for ( x  in  response.headers("Set-Cookie")) {
                            if (x.contains("ibsession")) {
                                Log.d("",x.toString())
                               val  ibSession = x.split(";")[0].split("=")[1]
                                Log.d("ibSession", ibSession)
                                (activity as MainActivity).prefs.edit {putString("ibSession", ibSession)}
                               }
                        }

                        val x = response.body()?.string().toString()
                        val jsa = JSONArray(x).toMutableList()
                        val cdList = arrayListOf<CDocuments>()
                        jsa.forEach {
                            cdList.add(
                                CDocuments(
                                    it.getString("id"),
                                    it.getString("Date"),
                                    it.getString("Number"),
                                    it.getString("Status"),
                                    it.getString("type"),
                                    it.getString("Supplier")

                                )
                            )
                        }
                        val adapter = DocumentsAdapter(contextF, cdList)
                        activity!!.runOnUiThread { lv.adapter = adapter }

                    }
                }
            }
        })
    }

    private fun newDocument(){
//val act = (activity as MainActivity)
        val formBody = FormBody.Builder()
            .add("DevID", "Fuck")
            .build()
        val paramIB =  (activity as MainActivity).prefs.getString("ibSession","")
        request = if (paramIB=="") {
            Request.Builder()
                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin")
                .addHeader("IBSession","start")
                .header("Accept", "application/json, text/plain, */*")
                .header("Content-Type", "application/json;charset=utf-8")
                .post(formBody)
                .build()
        } else {
            Request.Builder()
                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin")
                .header("Accept", "application/json, text/plain, */*")
                .header("Content-Type", "application/json;charset=utf-8")
                .addHeader("Cookie","IBSession=${paramIB}") //Cookie: IBSession=43CD8102-F970-4FFB-939A-C47948F49885
                .build()
        }


//         request = Request.Builder()
//            .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin")
//
//
//
//            .header("Accept", "application/json, text/plain, */*")
//            .header("Content-Type", "application/json;charset=utf-8")
//            .post(formBody)
//            .build()



        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    httpError(e.stackTraceToString(), "settings")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        if (response.code()==404) {
                            (activity as MainActivity).prefs.edit().remove("ibSession").apply() //{putString(, "")}
                            newDocument()
                            Log.d("ID",response.code().toString())
                        } else {
                        activity!!.runOnUiThread {
                            httpError(response.toString(), "settings")
                        }}
                    } else {
                        val jso = JSONObject(response.body()?.string().toString())
                        params.putString("DocID", jso.getString("id"))
                        params.putString("DocNumber", jso.getString("Number"))
                        params.putString("type", "Stupid")
                        params.putString("IP_ADDRESS_SERVER", params.getString("IP_ADDRESS_SERVER"))
                        activity!!.runOnUiThread {
                            navController.navigate(
                                R.id.action_docListFragment_to_positionsFragment,
                                params
                            )
                        }

                    }
                }
            }
        })
}

    private fun settingsError(message: String) {
//        val act = (activity as MainActivity)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.dialog_error_title))
        builder.setMessage(message)
        builder.setPositiveButton(R.string.settings_menu_item) { _, _ ->
            requireActivity().runOnUiThread {
                navController.navigate(R.id.action_docListFragment_to_settingsFragment)
            }
        }
        builder.setNegativeButton(R.string.dialog_exit) { _, _ ->
//            act.navController.navigate(R.id.action_shopSelectFragment_to_docListFragment)
        }
        builder.show()
    }

    override fun onResume() {
        val act = (activity as MainActivity)
        if (act.prefs.contains("IP_ADDRESS_SERVER")) {
            params.putString("IP_ADDRESS_SERVER", act.prefs.getString("IP_ADDRESS_SERVER", null))
            params.putBoolean("ENABLE_ORDERS", act.prefs.getBoolean("ENABLE_ORDERS", false))

            getDocuments()
        } else {
            requireActivity().runOnUiThread {
                settingsError(getString(R.string.settings_error))}
        }


        super.onResume()
    }


    override fun onAttach(context: Context) {
        contextF=context
        super.onAttach(context)
    }



}