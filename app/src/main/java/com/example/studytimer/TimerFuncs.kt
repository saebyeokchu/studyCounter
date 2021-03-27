package com.example.studytimer

class TimerFuncs {

    fun secondToStopwatchText(second : Long, firstDelimeter : String,secondDelimeter : String, thirdDelimeter: String,skip:Boolean) : String{

        val tempH = getHourFromSeconds(second)
        val tempM = getMinuteFromSeconds(second)
        val tempS = getSecond(second)

        val hour : String = if(tempH==0) "00" else tempH.toString()
        val minute : String = if(tempM==0) "00" else if(tempM < 10) "0$tempM" else tempM.toString()
        val sec : String = if(tempS==0) "00" else if(tempS < 10) "0$tempS" else tempS.toString()

        var returnStr : String = ""
        if(skip) {
            if(tempH!=0) returnStr += "$hour$firstDelimeter "
            if(tempM!=0) returnStr += "$minute$secondDelimeter "
            returnStr += "$sec$thirdDelimeter"
        }else{
            returnStr = "$hour$firstDelimeter$minute$secondDelimeter$sec$thirdDelimeter"
        }

        return returnStr

    }

    fun getMinuteFromSeconds(second : Long) : Int {
        return ((second-3600*(second/3600))/60).toInt()
    }

    fun getHourFromSeconds(second : Long) : Int {
        return (second/3600).toInt()
    }

    fun getSecond(second : Long) : Int {
        return ((second-3600*(second/3600))%60).toInt()
    }

}