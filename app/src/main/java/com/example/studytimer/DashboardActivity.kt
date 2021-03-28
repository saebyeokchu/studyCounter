package com.example.studytimer

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity


class DashboardActivity : AppCompatActivity() {

    private lateinit var startBtn: Button
    private lateinit var pauseBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var stateOff: Button
    private lateinit var stateOn: Button

    lateinit var timerContent : TextView
    lateinit var restTimerContent : TextView

    var countDownTurnedOn : Int = -1

    var second : Long = 0
    var restSecond : Long = 0
    var settedSecond : Long = 0
    var settedRestSecond : Long = 0

    var studySessionCount : Int = 0
    var completeStudyCount : Int = 0
    var completeRestCount : Int = 0

    var isPauseDataUploaded : Boolean = false
    var isStartDataUploaded : Boolean = false

    var handler = Handler(Looper.getMainLooper())
    lateinit var handlerTask : Runnable
    lateinit var restTimerHandlerTask : Runnable

    var previousTimerState : TimerState = TimerState.unknown

    //array -> sqlite로
    data class Journey(var index:Int,var startTime : Long,var endTime : Long,var state : TimerState,var interval : Long)
    var tempJourneys : MutableList<JourneyV2> = mutableListOf<JourneyV2>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        val timerFunc : TimerFuncs = TimerFuncs()

        initialViewSetup()
        initialTimeSetup()


        //카운트다운, 카운트업 초를 진행하면서 시행되는 task
        handlerTask = object : Runnable {
            override fun run() {

                second += countDownTurnedOn * -1
                resetTimerContentText()

                //decide proceed timer or move to nextStep
                if(second.toInt() == 0 && countDownTurnedOn == 1) { //this session is count down and study count down done
                    triggerTimerAction(TimerState.pauseStudy)
                }else{
                    handler.postDelayed(this, 1000)
                }

            }
        }

        restTimerHandlerTask = object : Runnable {
            override fun run() {

                //update timer content
                restSecond += countDownTurnedOn * -1
                resetRestTimerContentText()

                //decide proceed timer or move to nextStep
                if(restSecond.toInt() == 0 && countDownTurnedOn == 1) {
                    triggerTimerAction(TimerState.startStudy)
                }else{
                    handler.postDelayed(this, 1000)
                }

            }
        }
    }

    private fun initialTimeSetup(){

        val countdownSwitch : Switch = findViewById(R.id.countdownSwitch)

        if(countDownTurnedOn==-1) {//시간 올리는 버전
            countdownSwitch.isChecked = false

            second = 0
            restSecond = 0
        }else{ // 시간 내리는 버전
            countdownSwitch.isChecked = true
            //db에서 불러오기
            second = 10
            restSecond = 10
        }

        studySessionCount = 0
        tempJourneys = mutableListOf<JourneyV2>()
        resetTimerContentText()
        resetRestTimerContentText()
    }

    private fun initialViewSetup(){
        timerContent = findViewById(R.id.timerContent)
        restTimerContent = findViewById(R.id.restTimerContent)
        startBtn = findViewById<Button>(R.id.start)
        pauseBtn = findViewById<Button>(R.id.pause)
        stopBtn = findViewById<Button>(R.id.stop)
        stateOff = findViewById<Button>(R.id.stateOff)
        stateOn = findViewById<Button>(R.id.stateOn)

        startBtn.setOnClickListener { triggerTimerAction(TimerState.startStudy) }
        pauseBtn.setOnClickListener{ triggerTimerAction(TimerState.pauseStudy) }
        stopBtn.setOnClickListener{ triggerTimerAction(TimerState.stopStudy)}
        stateOn.setOnClickListener{ setTimerTextVisibility(TimerState.startStudy) ; setButtonColors(TimerState.startStudy) }
        stateOff.setOnClickListener{ setTimerTextVisibility(TimerState.pauseStudy) ; setButtonColors(TimerState.pauseStudy)}

        //한번 지우고 추가해야 하나?
        ExpandTableLayout(DatabaseHandler(this).getTodayTimerData(TimerFuncs().GetTodayString("yyMMdd")));
    }

    private fun triggerTimerAction(timerState : TimerState){

        changeContentUI(timerState)
        timerAction(timerState)

    }

    private fun timerAction(state : TimerState) {

        val firstStartButtonClicked : Boolean = state==TimerState.startStudy && (previousTimerState == TimerState.unknown || previousTimerState == TimerState.stopStudy)
        val studyAgainButtonClicked : Boolean = state==TimerState.startStudy && previousTimerState == TimerState.pauseStudy
        val pauseStudyButtonClicked : Boolean = state==TimerState.pauseStudy
        val stopStudyButtonClicked : Boolean = state==TimerState.stopStudy

       /********************
       처음 시작 버튼을 눌렀을때
        *******************/
        if(firstStartButtonClicked){
            createNewSessionRecord()
            handler.post(handlerTask)
        }

        /********************
        쉬기 버튼을 눌렀을때
         *******************/
        if(pauseStudyButtonClicked){
            handler.removeCallbacks(handlerTask)
            handler.post(restTimerHandlerTask)

            //db
            tempJourneys[studySessionCount-1].studyEndTime = second
            tempJourneys[studySessionCount-1].restStartTime = restSecond

            //카운트 다운시 시간 체크
            if(second.toInt() == 0 && countDownTurnedOn == 1) {
                second = settedSecond
                resetTimerContentText()
            }

            startBtn.text = "다시 공부하러 가기\uD83D\uDCAA"
        }

        /********************
        다시 공부하기 버튼을 눌렀을때
         *******************/
        if(studyAgainButtonClicked){
            handler.removeCallbacks(restTimerHandlerTask)
            handler.post(handlerTask)

            //db
            tempJourneys[studySessionCount-1].restEndTime = restSecond
            createNewSessionRecord()

            //카운트 다운시 시간 체크
            if(restSecond.toInt() == 0 && countDownTurnedOn == 1) {
                restSecond = settedRestSecond
                resetRestTimerContentText()
            }
        }

        /********************
        공부 그만하기 버튼을 눌렀을때
         *******************/
        if(stopStudyButtonClicked){
            handler.removeCallbacks(handlerTask)

            //db
            tempJourneys[studySessionCount-1].studyEndTime = second
            studySessionCount++

            //add to under table layout
            ExpandTableLayout(TimerFuncs().resolveTimerData(countDownTurnedOn,tempJourneys,this))

            //초기화
            initialTimeSetup()
            startBtn.text = "공부 시작하기"
        }

        previousTimerState = state

    }

    private fun createNewSessionRecord() {
        studySessionCount++
        tempJourneys.add(JourneyV2(studySessionCount, second, -1, -1, -1))
    }

    private fun resetTimerContentText() {
        timerContent.text =
            TimerFuncs().secondToStopwatchText(second, ":", ":", "", false)
    }

    private fun resetRestTimerContentText() {
        restTimerContent.text =
            TimerFuncs().secondToStopwatchText(restSecond, ":", ":", "", false)
    }

    private fun changeContentUI(state:TimerState) {

        //button colors.. ui...
        setButtonColors(state)
        setButtonVisibility(state)
        setTimerTextVisibility(state)

    }

    private fun setButtonColors(state : TimerState) {

        if(state==TimerState.startStudy) {
            stateOn.setBackgroundColor(Color.BLACK)
            stateOn.setTextColor(Color.WHITE)

            stateOff.setBackgroundColor(Color.WHITE)
            stateOff.setTextColor(Color.BLACK)
        }else if(state==TimerState.pauseStudy) {
            stateOff.setBackgroundColor(Color.BLACK)
            stateOff.setTextColor(Color.WHITE)

            stateOn.setBackgroundColor(Color.WHITE)
            stateOn.setTextColor(Color.BLACK)
        }else{
            stateOn.setBackgroundColor(Color.WHITE)
            stateOn.setTextColor(Color.BLACK)

            stateOff.setBackgroundColor(Color.WHITE)
            stateOff.setTextColor(Color.BLACK)
        }
    }

    private fun setButtonVisibility(state : TimerState){
        if(state == TimerState.startStudy) {
            startBtn.visibility = View.INVISIBLE;
            pauseBtn.visibility = View.VISIBLE;
            stopBtn.visibility = View.VISIBLE;
        }else{
            startBtn.visibility = View.VISIBLE;
            pauseBtn.visibility = View.INVISIBLE;
            stopBtn.visibility = View.INVISIBLE;
        }
    }

    private fun setTimerTextVisibility(state : TimerState){
        if(state==TimerState.startStudy){
            timerContent.visibility = View.VISIBLE
            restTimerContent.visibility = View.INVISIBLE
        }else{
            timerContent.visibility = View.INVISIBLE
            restTimerContent.visibility = View.VISIBLE
        }
    }

    private fun ExpandTableLayout(timeInfos : MutableList<TimerDetail>) {
        val tableLayout : TableLayout = findViewById<TableLayout>(R.id.studyLog)
        var timerFuncs : TimerFuncs = TimerFuncs()

        var layoutParmas = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT
        )

        timeInfos.map {

            var tempTableRow = TableRow(this)
            var tempTableIndexTextView = TextView(this)
            var tempTableStudyTimeTextView = TextView(this)
            var tempTableRestTimeTextView = TextView(this)

            tempTableIndexTextView.gravity = Gravity.CENTER
            tempTableStudyTimeTextView.gravity = Gravity.CENTER
            tempTableRestTimeTextView.gravity = Gravity.CENTER

            Log.e(it.sessionCount.toString(),it.toString())
            tempTableIndexTextView.text = it.sessionCount.toString()
            tempTableStudyTimeTextView.text =
                timerFuncs.secondToStopwatchText(
                    it.studyTime,
                    "시",
                    "분",
                    "초",
                    true)
            tempTableRestTimeTextView.text =
                timerFuncs.secondToStopwatchText(
                    it.restTime,
                    "시",
                    "분",
                    "초",
                    true)

            /*tempTableIndexTextView.text = studySessionCount.toString()
            tempTableStudyTimeTextView.text =
                timerFuncs.secondToStopwatchText(
                    tempJourneys.find { it.index == studySeionCount && it.state == TimerState.startStudy }?.interval!!,
                    "시",
                    "분",
                    "초",
                    true)
            tempTableRestTimeTextView.text =
                timerFuncs.secondToStopwatchText(
                    tempJourneys.find { it.index == studySessionCount && it.state == TimerState.pauseStudy }?.interval!!,
                    "시",
                    "분",
                    "초",
                    true)*/

            tempTableRow.addView(tempTableIndexTextView, layoutParmas)
            tempTableRow.addView(tempTableStudyTimeTextView, layoutParmas)
            tempTableRow.addView(tempTableRestTimeTextView, layoutParmas)

            tableLayout.addView(tempTableRow)

        }

}

}

/*private fun StartCountdown(){
//24시간이 넘었으면 false
val remainDays: Long = Convert.ToInt64(GetReaminDays(registerDate).TotalMilliseconds)
Console.WriteLine("remain seconds : $remainDays")
return if (remainDays <= 0) {
false
} else {
val c = CountDown(view, remainDays, 1000)
c.Start()
true
}
}

fun GetReaminDays(startDate: Date, endDate: Date): Int {
return (endDate.Subtract(startDate).TotalDays)
}

fun GetReanDays(registerDate: string): TimeSpan? {
val startDate: DateTime = DateTime.Now
val endDate: DateTime
endDate = if (registerDate == "NOW") {
DateTime.Now.AddDays(1)
} else {
val dateInfo: Array<string> = FormatDate(registerDate)
val timeInfo: Array<string> = FormatTime(registerDate)
InitDate(dateInfo, timeInfo).AddDays(1)
}
Console.WriteLine("APP1 DEBUG : startDate => " + startDate.ToString())
Console.WriteLine("APP1 DEBUG : endDATE => " + endDate.ToString())
return endDate.Subtract(startDate)
}

}*/
