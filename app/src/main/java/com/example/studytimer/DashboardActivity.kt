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

    var countDownTurnedOn : Int = 1

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
    var tempJourneys : MutableList<Journey> = mutableListOf<Journey>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)
        initialViewSetup()

        val timerFuncs : TimerFuncs = TimerFuncs()
        val countdownSwitch : Switch = findViewById(R.id.countdownSwitch)

        if(countDownTurnedOn==-1) {//시간 올리는 버전
            countdownSwitch.isChecked = false

            settedSecond = 0
            settedRestSecond = 0
        }else{ // 시간 내리는 버전
            countdownSwitch.isChecked = true
            //db에서 불러오기
            settedSecond = 10
            settedRestSecond = 10
        }

        //초기화
        second = settedSecond
        restSecond = settedRestSecond

        timerContent.text =
            timerFuncs.secondToStopwatchText(second, ":", ":", "", false)
        restTimerContent.text =
            timerFuncs.secondToStopwatchText(restSecond, ":", ":", "", false)

        //카운트다운, 카운트업 초를 진행하면서 시행되는 task
        handlerTask = object : Runnable {
            override fun run() {
                second += countDownTurnedOn * -1
                val isDoneWithCountDownOption : Boolean = second.toInt() == 0 && countDownTurnedOn == 1

                //0초면 자동으로 휴식으로 넘어가기
                if( isDoneWithCountDownOption) second=settedSecond+1

                timerContent.text =
                    timerFuncs.secondToStopwatchText(second, ":", ":", "", false)

                if( isDoneWithCountDownOption) {
                    completeStudyCount++
                    triggerTimerAction(TimerState.pause)
                }else{
                    handler.postDelayed(this, 1000)
                }

            }
        }
        restTimerHandlerTask = object : Runnable {
            override fun run() {
                restSecond += countDownTurnedOn * -1
                val isDoneWithCountDownOption : Boolean = restSecond.toInt() == 0 && countDownTurnedOn == 1

                //0초면 자동으로 공부로 넘어가기
                if(isDoneWithCountDownOption) restSecond = settedRestSecond+1

                restTimerContent.text =
                    timerFuncs.secondToStopwatchText(restSecond, ":", ":", "", false)

                if(isDoneWithCountDownOption) {
                    completeRestCount++
                    triggerTimerAction(TimerState.start)
                }else{
                    handler.postDelayed(this, 1000)
                }

            }
        }
    }

    private fun initialViewSetup(){
        timerContent = findViewById(R.id.timerContent)
        restTimerContent = findViewById(R.id.restTimerContent)
        startBtn = findViewById<Button>(R.id.start)
        pauseBtn = findViewById<Button>(R.id.pause)
        stopBtn = findViewById<Button>(R.id.stop)
        stateOff = findViewById<Button>(R.id.stateOff)
        stateOn = findViewById<Button>(R.id.stateOn)

        startBtn.setOnClickListener { triggerTimerAction(TimerState.start) }
        pauseBtn.setOnClickListener{ triggerTimerAction(TimerState.pause) }
        stopBtn.setOnClickListener{ triggerTimerAction(TimerState.stop)}
        stateOn.setOnClickListener{ setTimerTextVisibility(TimerState.start) ; setButtonColors(TimerState.start) }
        stateOff.setOnClickListener{ setTimerTextVisibility(TimerState.pause) ; setButtonColors(TimerState.pause)}
    }

    private fun triggerTimerAction(timerState : TimerState){

        changeContentUI(timerState)
        timerAction(timerState)

    }

    private fun timerAction(state : TimerState) {

        if(state==TimerState.start) { //시작버튼을 눌렀으면

            if (previousTimerState == TimerState.pause){//이전 일시정지에서
                handler.removeCallbacks(restTimerHandlerTask)
            }

            if(!isStartDataUploaded) {//공부시간 시작데이터 초기 삽입
                tempJourneys.add(Journey(studySessionCount, second, -1, state, -1))
                isStartDataUploaded = true
            }

            handler.post(handlerTask)
        }
        else {//일시정지 혹은 정리버튼을 눌렀으면
            handler.removeCallbacks(handlerTask)

            if(state==TimerState.stop) {
                //찾아야 함 인덱스가 studySessionCount이고 state가 start 친구들 찾아야 함

                //휴식, 공부 데이터 업로드
                AddToTodayStudyList()

                //add to under table layout
                ExpandTableLayout()
                studySessionCount++

                //초기화
                if(countDownTurnedOn==-1) {
                    timerContent.text = "00:00:00"
                    second = 0

                    restTimerContent.text = "00:00:00"
                    restSecond = 0
                }else{
                    val timerFuncs : TimerFuncs = TimerFuncs()

                    timerContent.text =
                        timerFuncs.secondToStopwatchText(settedSecond, ":", ":", "", false)
                    second = settedSecond

                    restTimerContent.text =
                        timerFuncs.secondToStopwatchText(settedRestSecond, ":", ":", "", false)
                    restSecond = settedRestSecond
                }

                startBtn.text = "공부 시작하기"
                isStartDataUploaded = false
                isPauseDataUploaded = false
            }else{
                if(!isPauseDataUploaded) {//초기 휴식데이터 삽입
                    tempJourneys.add(Journey(studySessionCount, restSecond, -1, state, -1))
                    isPauseDataUploaded = true
                }

                handler.post(restTimerHandlerTask)
                startBtn.text = "다시 공부하러 가기\uD83D\uDCAA"
            }
        }

        previousTimerState = state

    }

    private fun AddToTodayStudyList() {
        val tempStudy : Journey? = tempJourneys.find { it.index == studySessionCount && it.state == TimerState.start}
        tempStudy?.endTime = second
        if(countDownTurnedOn==-1) {
            tempStudy?.interval = (second - tempStudy!!.startTime)
        }
        else tempStudy?.interval = tempStudy!!.startTime - second + settedSecond*completeStudyCount

        val tempRest : Journey? = tempJourneys.find { it.index == studySessionCount && it.state == TimerState.pause}
        tempRest?.endTime = restSecond
        if(countDownTurnedOn==-1) {
            tempRest?.interval = restSecond - tempRest!!.startTime
        }
        else tempRest?.interval = tempRest!!.startTime - restSecond + settedRestSecond*completeRestCount
    }

    private fun changeContentUI(state:TimerState) {

        //button colors.. ui...
        setButtonColors(state)
        setButtonVisibility(state)
        setTimerTextVisibility(state)

    }

    private fun setButtonColors(state : TimerState) {

        if(state==TimerState.start) {
            stateOn.setBackgroundColor(Color.BLACK)
            stateOn.setTextColor(Color.WHITE)

            stateOff.setBackgroundColor(Color.WHITE)
            stateOff.setTextColor(Color.BLACK)
        }else if(state==TimerState.pause) {
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
        if(state == TimerState.start) {
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
        if(state==TimerState.start){
            timerContent.visibility = View.VISIBLE
            restTimerContent.visibility = View.INVISIBLE
        }else{
            timerContent.visibility = View.INVISIBLE
            restTimerContent.visibility = View.VISIBLE
        }
    }

    private fun ExpandTableLayout() {
        val tableLayout : TableLayout = findViewById<TableLayout>(R.id.studyLog)
        var timerFuncs : TimerFuncs = TimerFuncs()

        var layoutParmas = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.MATCH_PARENT
        )

        var tempTableRow = TableRow(this)
        var tempTableIndexTextView = TextView(this)
        var tempTableStudyTimeTextView = TextView(this)
        var tempTableRestTimeTextView = TextView(this)

        tempTableIndexTextView.gravity = Gravity.CENTER
        tempTableStudyTimeTextView.gravity = Gravity.CENTER
        tempTableRestTimeTextView.gravity = Gravity.CENTER

        tempTableIndexTextView.text = studySessionCount.toString()
        tempTableStudyTimeTextView.text =
            timerFuncs.secondToStopwatchText(
                tempJourneys.find { it.index == studySessionCount && it.state == TimerState.start} ?.interval!!,
                "시",
                "분",
                "초",
                true)
        tempTableRestTimeTextView.text =
            timerFuncs.secondToStopwatchText(
                tempJourneys.find { it.index == studySessionCount && it.state == TimerState.pause} ?.interval!!,
                "시",
                "분",
                "초",
                true)

        tempTableRow.addView(tempTableIndexTextView, layoutParmas)
        tempTableRow.addView(tempTableStudyTimeTextView, layoutParmas)
        tempTableRow.addView(tempTableRestTimeTextView, layoutParmas)

        tableLayout.addView(tempTableRow)

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
