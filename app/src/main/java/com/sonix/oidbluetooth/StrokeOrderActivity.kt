package com.sonix.oidbluetooth

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.sonix.oidbluetooth.bean.HanziBean
import com.sonix.oidbluetooth.view.HanziWriterView
import com.sonix.oidbluetooth.view.StrokeOrderView
import com.sonix.oidbluetooth.view.StrokeOrderViewJava
import com.sonix.oidbluetooth.view.StrokeOrderViewJavaNew
import com.sonix.util.LogUtils
import com.sonix.util.ThreadManager

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.concurrent.schedule

class StrokeOrderActivity : AppCompatActivity() {

    var svgSix: String? = null
    var svgOne: String? = null
    lateinit var strokeOrderView1: StrokeOrderViewJava
    lateinit var strokeOrderView2: StrokeOrderViewJavaNew
    private var hanziBean: HanziBean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroke_order_layout)


        strokeOrderView1 = findViewById(R.id.stroke_order_view1)
        strokeOrderView2 = findViewById(R.id.stroke_order_view2)

        findViewById<Button>(R.id.btn_load_svg_six).setOnClickListener {
            val name = "你.json" // 需要将 svg.json 放在 assets 或特定路径下
            svgSix = loadSvgFromAssets(name)
            svgSix?.let {
                strokeOrderView1.setStrokesBySvg(it)
            }
        }

        findViewById<Button>(R.id.btn_load_svg_one).setOnClickListener {
            val name = "你.json"
            svgOne = loadSvgFromAssets(name)

            svgOne?.let {
                hanziBean = Gson().fromJson(it, HanziBean::class.java)
                strokeOrderView2.setHanziBean(hanziBean)
                strokeOrderView2.writerHanzi()

                Handler().postDelayed({
                    strokeOrderView2.startAnimation()
                },1000);

            }
        }
//        strokeOrderView2.setOnAnimStrokeWriterStartListener {
//            if (it == hanziBean!!.strokeCount-1){
//                Handler().postDelayed({
//                    strokeOrderView2.setPosition(2)
//                },2000)
//            }
//
//        }

    }

    private fun loadSvgFromAssets(name: String): String? {
        try {
            assets.list("data")?.let {
                for (s in it) {
                    if (name == s) {
                        Log.d("zuo", "svgName-> $s")
                        return loadSvgJson("data/$s") ?: "NULL"
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun loadSvgJson(file: String): String? {
        var reader: BufferedReader? = null
        var inputStreamReader: InputStreamReader? = null
        try {
            val inputStream: InputStream = assets.open(file)
            inputStreamReader = InputStreamReader(inputStream)
            reader = BufferedReader(inputStreamReader)
            var line: String?
            val entity = java.lang.StringBuilder()
            while (reader.readLine().also { line = it } != null) {
                entity.append(line)
            }
            return entity.toString()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStreamReader?.close()
                reader?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }


}