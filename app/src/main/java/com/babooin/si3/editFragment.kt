package com.babooin.si3

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.Navigation
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class editFragment : Fragment() {

    private var docID: String? = null
    private var barcode: String? = null

    lateinit var viewEdit: EditText
    lateinit var barView: TextView

    lateinit var amountEdit: EditText


    private  var amount:Double=0.0

    private  lateinit var request: Request
    private lateinit var client:OkHttpClient

    private var changed:Boolean = false
    private var canScan:Boolean = false
    private var canPut:Boolean = true
    private var firstChar:Boolean = true

    private lateinit var contextF:Context
    private lateinit var params:Bundle
private  lateinit var helpBar:TextView
    private  lateinit var qtty:TextView

private lateinit var navController:NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        params = Bundle()
        super.onCreate(savedInstanceState)
        params = requireArguments()
        arguments?.let {
            docID = it.getString("DocID")
            barcode = it.getString("BarCode")
//            params.putString("orders", it.getString("orders"))
            params.putString("DocID", it.getString("DocID"))
            params.putString("BarCode", it.getString("BarCode"))
            params.putString("IP_ADDRESS_SERVER", it.getString("IP_ADDRESS_SERVER"))
            params.putString("Number", it.getString("Number"))
            params.putString("type", it.getString("type"))

        }

        client = OkHttpClient()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewEdit = view.findViewById(R.id.view_edit)
        barView = view.findViewById(R.id.bar_edit)
        amountEdit = view.findViewById(R.id.amount_edit)
        helpBar = view.findViewById(R.id.notAsked)
        qtty = view.findViewById(R.id.qtty)
        (activity as MainActivity).supportActionBar?.subtitle = params.getString("BarCode")
        (activity as MainActivity).supportActionBar?.setHomeButtonEnabled(true)

        amountEdit.addTextChangedListener(amountWatcher)
        viewEdit.addTextChangedListener(viewWatcher)
        amountEdit.setSelectAllOnFocus(true);
//        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        requireActivity().registerReceiver(barcodeCastReceiver, IntentFilter("android.intent.ACTION_DECODE_DATA"))
        LocalBroadcastManager.getInstance(contextF).registerReceiver(keyReceiver, IntentFilter("key_press_intent"))
        navController = Navigation.findNavController(requireView())
        getPosition()
        amountEdit.requestFocus()
        firstChar = true
        canPut =true
        canScan = true
        super.onResume()
        qtty.text = params.getDouble("plan_amount").toString()
    }

    override fun onPause() {
        putPosition()
        requireActivity().unregisterReceiver(barcodeCastReceiver)
        LocalBroadcastManager.getInstance(contextF).unregisterReceiver(keyReceiver)

        super.onPause()
    //        requireActivity().runOnUiThread{
//            (activity as MainActivity).navController.navigate(R.id.action_editFragment_to_positionsFragment,params)
//        }

    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.delete_edit_menuitem -> deleteAction()
                android.R.id.home -> onBack()

            }

        return super.onOptionsItemSelected(item)
    }

    private fun  onBack() {
        if (changed ) {
            putPosition()
        }
        if  (navController.currentDestination?.label == "fragment_edit" ) {
            navController.navigate(R.id.action_editFragment_to_positionsFragment, params)
        }
    }

    private  fun deleteAction(){
        val builder = AlertDialog.Builder(contextF)
        builder.setTitle(getString(R.string.del_title))
        builder.setMessage(getString(R.string.del_body))

        builder.setPositiveButton(R.string.yes) { _, _ ->
            deletePositionDetails()
        }

        builder.setNegativeButton(R.string.no) { _, _ ->
            Toast.makeText(contextF,
                    R.string.no, Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun deletePositionDetails () {
//        val act = (activity as MainActivity)
        val body = FormBody.Builder()
                .add("bar", params.getString("BarCode").toString())
                .add("id", params.getString("DocID").toString())
                .build()

        try {
            val paramIB =  (activity as MainActivity).prefs.getString("ibSession","")
            request = if (paramIB=="") {
                Request.Builder()
                    .url(
                        "${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=${
                            params.getString("DocID").toString()
                        }&bar=${params.getString("BarCode").toString()}"
                )
                    .addHeader("IBSession","start")
                    .delete(body)
                    .build()
            } else {
                Request.Builder()
                    .url(
                        "${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=${
                            params.getString("DocID").toString()
                        }&bar=${params.getString("BarCode").toString()}"
                    )
                    .addHeader("Cookie","IBSession=${paramIB}") //Cookie: IBSession=43CD8102-F970-4FFB-939A-C47948F49885
                    .delete(body)
                    .build()
            }

//            request = Request.Builder()
//                .url(
//                        "${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=${
//                            params.getString("DocID").toString()
//                        }&bar=${params.getString("BarCode").toString()}"
//                )
//                .delete(body)
//                .build()
        } catch (err: IOException) {httpError(err.toString(), "get")}


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    httpError(e.stackTrace.toString(), "get")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {


                        activity!!.runOnUiThread {
                            httpError(response.toString(), "get")
                        }
                    } else {
                        val jso = JSONObject(response.body()?.string().toString())
                        changed = false
                        activity!!.runOnUiThread {
                            Toast.makeText(context, jso.toString(), Toast.LENGTH_LONG).show()
                            navController.navigate(R.id.action_editFragment_to_positionsFragment, params)
                        }
                    }
                }
            }
        })
}

    private var amountWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            changed = true
            if (amountEdit.text.isNotEmpty()) {
                if (params.getString("type") == "Плановый" && amountEdit.text.toString().toDouble() > params.getDouble("plan_amount")) {
                    if (helpBar.isGone) {
                        if (params.getDouble("plan_amount") != 0.0) {
                            helpBar.text = getString(R.string.weOrder) + " ${params.getDouble("plan_amount")}"
                        }
                        helpBar.visibility = View.VISIBLE
                        qtty.visibility = View.GONE
                    }
                    val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_NETWORK_BUSY, 250)
                } else {
                    helpBar.visibility = View.GONE
                    qtty.visibility = View.VISIBLE
                }
            }
            else {
                helpBar.visibility = View.GONE
                firstChar = true
            }
        }
            }

    private var viewWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {changed = true}
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contextF = context
    }

    fun Fragment.vibratePhone() {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {

            vibrator.vibrate(1000)
        }
    }

    private val barcodeCastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            when (intent?.action) {
                "android.intent.ACTION_DECODE_DATA" -> {
                    if (canScan) {
                        val bent = intent.getStringExtra("barcode_string")
                        if (params.getString("BarCode").toString() != bent) {
                            putPosition()
                            firstChar = true
                            canScan = true
                        }
                        params.putString("BarCode", bent.toString())
                        getPosition()
                    }
                } } } }



  private val keyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
      override fun onReceive(contxt: Context?, intent: Intent?) {

//          val act = (activity as MainActivity)
          val chr = intent?.extras?.getString("char")
          val key = intent?.extras?.getInt("keycode")
          when (key) {
              KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> onBack()
              KeyEvent.KEYCODE_ENTER -> {if (helpBar.isGone){onBack()}}
              KeyEvent.KEYCODE_NUMPAD_DOT, 18 -> {
                  if (firstChar) {
                      amountEdit.setText("0.")
                      amountEdit.setSelection(amountEdit.text.length)
                      firstChar = false
                  }
                  else {
                      amountEdit.append(".")
                  }
              }
              KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
              KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9, KeyEvent.KEYCODE_NUMPAD_0 -> {
                  if (firstChar) {
                      try {
                          firstChar = false
                          amountEdit.setText(chr)
                          amountEdit.setSelection(amountEdit.text.length)
                      } catch (_: Exception) {
                      }
                  }
              }
          }
      }
  }
    private fun httpError(message: String, owner: String) {
//        val act =  (activity as MainActivity)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.dialog_error_title))
        builder.setMessage(message)
        builder.setPositiveButton(R.string.dialog_retry) { _, _ ->
            when (owner) {
                "get" -> getPosition()
                "put" -> putPosition()
            }
        }
        builder.setNegativeButton(R.string.dialog_exit) { _, _ ->
            requireActivity().runOnUiThread{
                (activity as MainActivity).navController.navigate(R.id.action_editFragment_to_positionsFragment, params)
            }
        }
        builder.show()
    }

    private fun getPosition() {

        val paramIB =  (activity as MainActivity).prefs.getString("ibSession","")
        request = if (paramIB=="") {
            Request.Builder()
                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=${params.getString("DocID")}&bar=${params.getString("BarCode")}")
                .addHeader("IBSession","start")
                .build()
        } else {
            Request.Builder()
                .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=${params.getString("DocID")}&bar=${params.getString("BarCode")}")
                .addHeader("Cookie","IBSession=${paramIB}")
                .build()
        }
//        request = Request.Builder()
//            .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&id=${params.getString("DocID")}&bar=${params.getString("BarCode")}")
//            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    httpError(e.stackTrace.toString(), "get")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                            if (response.code()==404) {
                                (activity as MainActivity).prefs.edit().remove("ibSession").apply()
                                getPosition()
                                Log.d("ID",response.code().toString())
                            } else {
                        activity!!.runOnUiThread {
                            httpError(response.toString(), "get")
                        }}
                    }
                        else {
                        val jso = JSONObject(response.body()?.string().toString())
                        params.putString("code", jso.getString("code"))
                        params.putString("BarCode", jso.getString("barcode"))
                        params.putDouble("amount", jso.getDouble("amount"))
                        params.putDouble("plan_amount", jso.getDouble("plan_amount"))


                        requireActivity().runOnUiThread {
                            viewEdit.setText(jso.getString("view"))
                            barView.setText(jso.getString("barcode"))
                            amountEdit.setText(jso.getDouble("amount").toString())

                            if (params.getString("type") == "Плановый" && params.getDouble("plan_amount") < params.getDouble("amount")) {
                                helpBar.visibility = View.VISIBLE
                                qtty.visibility = View.GONE

                            } else {
                                helpBar.visibility = View.GONE
                                qtty.visibility = View.VISIBLE
                                qtty.text =  jso.getDouble("plan_amount").toString()
                            }
                        }
                        canScan = true
                        changed = true
                        canPut = true

                    }
                }
            }
        })
    }
    @SuppressLint("SuspiciousIndentation")
    private fun putPosition() {
        if (canPut) {
        val body = FormBody.Builder()
            .addEncoded("view", viewEdit.text.toString())
            .add("bar", barView.text!!.toString())
            .add("code", params.getString("code").toString())
            .add("amount", amountEdit.text.toString().toDouble().toString()) //params.getDouble("amount").toString())
            .add("id", params.getString("DocID").toString()) //params.getString("DocID"))
            .build()

            try {
                val paramIB =  (activity as MainActivity).prefs.getString("ibSession","")
                request = if (paramIB=="") {
                    Request.Builder()
                        .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&bar=${params.getString("BarCode")}&code=${params.getString("code")}&id=${params.getString("DocID")}&amount=${params.getDouble("amount").toString()}")
                        .addHeader("IBSession","start")
                        .put(body)
                        .build()
                } else {
                    Request.Builder()
                        .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&bar=${params.getString("BarCode")}&code=${params.getString("code")}&id=${params.getString("DocID")}&amount=${params.getDouble("amount").toString()}")
                        .addHeader("Cookie","IBSession=${paramIB}")
                        .put(body)
                        .build()
                }


//         request = Request.Builder()
//            .url("${params.getString("IP_ADDRESS_SERVER")}/ut19/hs/api/spin/?DevID=1&bar=${params.getString("BarCode")}&code=${params.getString("code")}&id=${params.getString("DocID")}&amount=${params.getDouble("amount").toString()}")
//            .put(body)
//            .build()
            } catch (err: IOException) {
                httpError(err.toString(), "put")
            }


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    httpError(e.stackTrace.toString(), "put")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        if (response.code()==404) {
                            (activity as MainActivity).prefs.edit().remove("ibSession").apply()
                            putPosition()
                            Log.d("ID",response.code().toString())
                        } else {
                        activity!!.runOnUiThread {
                            httpError(response.toString(), "put")
                        }}
                    } else {
//                        val jso = JSONObject(response.body()?.string().toString())
                        changed = false
                        canScan = true
                        canPut = true

                    }
                }
            }
        })}
    }

}

