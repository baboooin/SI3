package com.babooin.si3


import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.Navigation


class MainActivity : AppCompatActivity() {
    lateinit var prefs: SharedPreferences
    lateinit var navController:NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val typedValue = TypedValue()
                theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
                window.statusBarColor = typedValue.data
            }
        }

//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//            toolbar.setNavigationOnClickListener(View.OnClickListener {
//                backAction()
//            })
            navController = Navigation.findNavController(this, R.id.fragment_container)
//            fm = supportFragmentManager
//  }
    }


    override fun onBackPressed() {
//        false
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val intent = Intent("key_press_intent")
            intent.putExtra("keycode", keyCode)
            intent.putExtra("char", event?.displayLabel.toString())
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        return false //super.onKeyUp(keyCode, event)
    }

}