package com.example.studytimer

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var google_btn =  findViewById<Button>(R.id.google_login_btn);
        var without_login_btn : TextView = findViewById<TextView>(R.id.enter_without_login);

        google_btn.setOnClickListener{
            goToDashboard()
        }
        without_login_btn.setOnClickListener{
            showLoginInfo("로그인 하시면 1주일이 넘는 기간의 기록을 확인 할 수 있어요.")
        }

        /*var go_to_dashboard = View.OnClickListener { view ->
            when (view.id) {
                R.id.google_login_btn -> go_to_dashboard()
                R.id.enter_without_login -> show_login_info("로그인 하시면 1주일이 넘는 기간의 기록을 확인 할 수 있어요.")
            }
        }*/
    }

    private fun goToDashboard() {
        var new_intent = Intent(this, DashboardActivity::class.java)
        startActivity(new_intent)
    }

    private fun showLoginInfo(title: String) {
        //밑에서 올라오는걸로 나중에 바꿀래

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_layout)
        val body = dialog.findViewById(R.id.dialog_title) as TextView
        body.text = title
        val goto_register = dialog.findViewById(R.id.register_btn) as Button
        val goto_dashboard = dialog.findViewById(R.id.goto_dashboard_btn) as TextView
        goto_register.setOnClickListener { dialog.dismiss() }
        goto_dashboard.setOnClickListener { goToDashboard() }
        dialog.show()

    }
}