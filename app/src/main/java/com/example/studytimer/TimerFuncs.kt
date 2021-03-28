package com.example.studytimer

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

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

    fun GetTodayString(pattern : String) : String {
        val simpleDateFormat = SimpleDateFormat(pattern)
        return simpleDateFormat.format(Date())
    }

    //회차당 시간 계산해서 db에 넣어야 함. 필요한 정보가 세션당 공부 쉬는 시간 계산해야 되고 세션정보 기반으로 카운트 전체 공부/ 쉬는 시간이랑 평균 내야 함
    fun resolveTimerData(countDownTurnedOn : Int , tempJourneys : MutableList<JourneyV2>,context: Context) : MutableList<TimerDetail> {
         var totalStudyTime : Long = 0
        var totalRestTime : Long = 0

        val date: String = GetTodayString("yyMMdd")

        var list : MutableList<TimerDetail> = mutableListOf<TimerDetail>()
        var index = DatabaseHandler(context).getTodayTimerDataLength(date)

        tempJourneys.map {

            totalStudyTime = 0
            totalRestTime = 0
            index ++

            if (countDownTurnedOn == -1) {
                if (it.studyEndTime.toInt() != -1) //endTime이 -1이라는건 데이터가 없다는 뜻
                    totalStudyTime = it.studyEndTime - it.studyStartTime
                totalRestTime = it.restEndTime - it.restStartTime
            } else {
                if (it.studyEndTime.toInt() != -1) //endTime이 -1이라는건 데이터가 없다는 뜻
                    totalStudyTime = it.studyStartTime - it.studyEndTime
                totalRestTime = it.restStartTime - it.restEndTime
            }

            //하나씩 db sqlite에 삽입하기
            insertToDetailTimeDB(date,index, totalStudyTime, totalRestTime,context);

            //하나씩 리턴할 리스트에 삽입하기
            list.add(TimerDetail(date,index, totalStudyTime, totalRestTime))
            /*Log.e("code : ",date)
            Log.e("resolve index : ",it.index.toString())
            Log.e("resolve studyTime : ",totalStudyTime.toString())
            Log.e("resolve restTime : ",totalRestTime.toString())*/
        }

        return list

    }

    private fun insertToDetailTimeDB(date:String,index : Int, totalStudyTime:Long , totalRestTime:Long,context : Context) {

        val db = DatabaseHandler(context)
        db.insertData(context, TimerDetail(date,index,totalStudyTime,totalRestTime))

        CommonFunc().makeToast(context,"all sessions for this counter added")


    }

}