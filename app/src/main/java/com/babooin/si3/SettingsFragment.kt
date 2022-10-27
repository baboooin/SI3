package com.babooin.si3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.AppCompatCheckBox
import android.widget.EditText
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.Navigation
//import websocket

class SettingsFragment : Fragment() {
    private lateinit var  navController: NavController
    private lateinit var ipEditText:EditText
private  lateinit var params:Bundle
private lateinit var _context:Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        params = Bundle()
        setHasOptionsMenu(true)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        navController.navigate(R.id.action_settingsFragment_to_docListFragment,params)
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onAttach(context: Context) {
        _context = context
        super.onAttach(context)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)
        val act = (activity as MainActivity)

        act.supportActionBar?.title = getString(R.string.settings_menu_item)
        act.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
        act.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ipEditText = view.findViewById<EditText>(R.id.server_ip_address)
        ipEditText.setText(act.prefs.getString("IP_ADDRESS_SERVER",""))
        ipEditText.addTextChangedListener(textWatcher)



        val checkOrder = view.findViewById<AppCompatCheckBox>(R.id.enableorders)
        checkOrder.isChecked = (activity as MainActivity).prefs.getBoolean("ENABLE_ORDERS",false)
        checkOrder.setOnClickListener {
            (activity as MainActivity).prefs.edit {putBoolean("ENABLE_ORDERS", checkOrder.isChecked)}
            params.putBoolean("ENABLE_ORDERS", checkOrder.isChecked)
        }

    }

    private var textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
//        val floatingUsernameLabel =
//            view?.findViewById<TextInputLayout>(R.id.username_text_input_layout);
//
//        floatingUsernameLabel.setErr
        }
        override fun afterTextChanged(s: Editable) {

            val addr:String = if (s.toString().contains("http",true) ) {
                s.toString()
            }
            else {
                "http://$s"
            }

            (activity as MainActivity).prefs.edit {
                putString("IP_ADDRESS_SERVER", addr)
            }
            params.putString("IP_ADDRESS_SERVER", addr)
            }
        }

    override fun onResume() {
        LocalBroadcastManager.getInstance(_context).registerReceiver(keyReceiver, IntentFilter("key_press_intent"))
        super.onResume()
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(_context).unregisterReceiver(keyReceiver)
        super.onPause()
    }

    private val keyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {

//          val act = (activity as MainActivity)
//            val chr = intent?.extras?.getString("char")
            val key = intent?.extras?.getInt("keycode")
            when (key) {
                KeyEvent.KEYCODE_ESCAPE,  KeyEvent.KEYCODE_BACK,  KeyEvent.KEYCODE_ENTER ->
                    navController.navigate(R.id.action_settingsFragment_to_docListFragment, params)
                }
            }
        }

}