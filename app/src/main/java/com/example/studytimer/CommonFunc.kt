package com.example.studytimer

import android.content.Context
import android.widget.Toast

class CommonFunc {

    fun makeToast(context: Context, message:String){
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show()
    }
}