package com.example.studytimer

import android.app.ActionBar
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

//database variables
val DatabaseName = "StudyCounter"
val TableName = "DetailedTime"

class DatabaseHandler(var context: Context) : SQLiteOpenHelper(context, DatabaseName, null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        //나중에 데이터베이스 체크하는거 해야겠다

        //basic info lite sql
        val createBasicInfoTable =
            "CREATE TABLE DetailedTime(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "counterCode VARCHAR(10), " +
                    "sessionCount INTEGER, " +
                    "studyTime LONG, " +
                    "restTime Long)"
        db?.execSQL(createBasicInfoTable)

        //extra info lite sql
        val createIntervalInfoTable =
            "CREATE TABLE IntervalInfo(" +
                    "interval INTEGER PRIMARY KEY, " +
                    "counterCode VARCHAR(10), " +
                    "sessionNumber INTEGER)"
        db?.execSQL(createIntervalInfoTable)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        TODO("Not yet implemented")
    }

    fun insertData(context:Context,time : TimerDetail){
        val database = this.readableDatabase
        val contentValues = ContentValues()
        //counter code 뒤에 _1 붙일건데 이거는 따로 계산하는 function 만들어야 함
        contentValues.put("counterCode",time.counterCode)
        contentValues.put("sessionCount",time.sessionCount)
        contentValues.put("studyTime",time.studyTime)
        contentValues.put("restTime",time.restTime)

        val result = database.insert(TableName,null,contentValues)
        if(result == (0).toLong())
            CommonFunc().makeToast(context,"failed")
        else
            CommonFunc().makeToast(context,"success")
    }

    fun readAllData() : MutableList<TimerDetail> {
        val list : MutableList<TimerDetail> = ArrayList()
        val database = this.readableDatabase
        val query = "Select * from $TableName"
        var result = database.rawQuery(query,null)

        if(result.moveToFirst()){
            do{
                val timer = TimerDetail(
                    result.getString(result.getColumnIndex("counterCode")),
                    result.getString(result.getColumnIndex("sessionCount")).toInt(),
                    result.getString(result.getColumnIndex("studyTime")).toLong(),
                    result.getString(result.getColumnIndex("restTime")).toLong()
                )
                list.add(timer)
                Log.e("database row value : ",timer.toString())
            }while(result.moveToNext())
        }
        return list
    }

    fun getPreviousInterval() : Int {
        val list : MutableList<TimerDetail> = ArrayList()
        val database = this.readableDatabase

        return -1;
    }

    fun getTodayTimerDataLength(counterCode : String) : Int {

        //210724 Does counter code mean date?
        val list : MutableList<TimerDetail> = ArrayList()
        val database = this.readableDatabase
        val query = "Select * from $TableName where counterCode=$counterCode"
        var result = database.rawQuery(query,null)

        return result.count
    }

    fun getTodayTimerData(counterCode : String) : MutableList<TimerDetail> {
        val list : MutableList<TimerDetail> = ArrayList()
        val database = this.readableDatabase
        val query = "Select * from $TableName where counterCode=$counterCode"
        var result = database.rawQuery(query,null)

        if(result.moveToFirst()){
            do{
                //get first session count here?
                val timer = TimerDetail(
                    result.getString(result.getColumnIndex("counterCode")),
                    result.getString(result.getColumnIndex("sessionCount")).toInt(),
                    result.getString(result.getColumnIndex("studyTime")).toLong(),
                    result.getString(result.getColumnIndex("restTime")).toLong()
                )
                list.add(timer)
                Log.e("database row value : ",timer.toString())
            }while(result.moveToNext())
        }
        return list
    }

    /*fun getTodayTimerData(counterCode : String) : Map<String,Any> {
        val list : MutableList<TimerDetail> = ArrayList()
        val database = this.readableDatabase
        val query = "Select * from $TableName where counterCode=$counterCode"
        var result = database.rawQuery(query,null)

        if(result.moveToFirst()){

            do{

                val timer = TimerDetail(
                    result.getString(result.getColumnIndex("counterCode")),
                    result.getString(result.getColumnIndex("sessionCount")).toInt(),
                    result.getString(result.getColumnIndex("studyTime")).toLong(),
                    result.getString(result.getColumnIndex("restTime")).toLong()
                )
                list.add(timer)
                Log.e("database row value : ",timer.toString())


            }while(result.moveToNext())
        }

        return mapOf(
            "listData" to list
        )
    }*/

    fun cleanTable() {
        val database = this.readableDatabase
        val query = "Select * from $TableName"
        var result = database.delete(TableName,null,null)

        Log.e("delete result",result.toString())

    }

    fun addTodayDataToServer() {

    }

}