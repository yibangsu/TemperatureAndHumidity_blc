package com.alw.th

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.alw.th.serial.SerialData
import com.alw.th.serial.SerialDataStruct
import com.alw.th.serial.SerialOptionType
import top.keepempty.sph.library.*

class MainActivity : AppCompatActivity() {

    private val TAG = "main"
    private var logEnable = false

    // a handler for view fresh
    var handler: Handler ?= null
    // serial params
    private var serialPortHelper: SerialPortHelper?= null
    private var isOpen: Boolean = false
    private val commPath0 = "/dev/ttyUSB0"
    private val commPath1 = "/dev/ttyUSB1"
    private var curSerialNum = -1
    private val SerialInterval = 100L
    // views to shows when serial callback
    private var title: TextView ?= null
    private var temperatureValue1: TextView ?= null
    private var temperatureValue2: TextView ?= null
    private var temperatureValue3: TextView ?= null
    private var temperatureValue4: TextView ?= null

    private var humidityValue1: TextView ?= null
    private var humidityValue2: TextView ?= null
    private var humidityValue3: TextView ?= null
    private var humidityValue4: TextView ?= null
    // package request index
    private var requestIndex: Int = 0
    private val requestIndexMax = 8
    // message type
    private val messageReadNext = 1
    private val messageRefresh = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        // get views which should refresh when serial callback
        title = findViewById(R.id.title)
        temperatureValue1 = findViewById(R.id.temperature_value_1)
        temperatureValue2 = findViewById(R.id.temperature_value_2)
        temperatureValue3 = findViewById(R.id.temperature_value_3)
        temperatureValue4 = findViewById(R.id.temperature_value_4)
        humidityValue1 = findViewById(R.id.humidity_value_1)
        humidityValue2 = findViewById(R.id.humidity_value_2)
        humidityValue3 = findViewById(R.id.humidity_value_3)
        humidityValue4 = findViewById(R.id.humidity_value_4)

//        check UI fresh
//        var button: Button = findViewById(R.id.button)
//        button.setOnClickListener {
//            var message: Message = Message()
//            var data = SerialDataStruct()
//            data.value = 38F
//            data.option = SerialOptionType.optionTypeTemperature
//            data.slaveIndex = 1
//            message.what = messageRefresh
//            message.obj = data
//            handler?.sendMessage(message)
//        }

//        test checksum
//        var button: Button = findViewById(R.id.button)
//        button.setOnClickListener {
//            var array = byteArrayOf(0x55, 0x06, 0x00, 0x00, 0x00,
//                0xFE.toByte(), 0x01, 0x08, 0x05, 0x00, 0x04, 0x01, 0x10)
//            var checksumUtil = SerialChecksum()
//            var checksum = checksumUtil.getCheckSum(array, array.size - 1)
//            Log.d(TAG, "checksum = ${checksum}")
//        }
    }

    override fun onStart() {
        super.onStart()
        initHandler()
        requestIndex = 0
        if (initPort(commPath0))
        {
            curSerialNum = 0
            isOpen = true
        }
        else if (initPort(commPath1))
        {
            curSerialNum = 1
            isOpen = true
        }
        log(TAG, "main start")
        // read write usb later
        if (isOpen)
        {
            title?.text = getText(R.string.title_serial).toString() + " " + curSerialNum
            writeAndReadCommDelay(0)
        }
        else
        {
//            var titleStr = getText(R.string.title_init_error)
//            title?.setText("${titleStr}\u00B0C")
            title?.setText(R.string.title_init_error)
        }
    }

    override fun onStop() {
        super.onStop()
        handler?.removeMessages(messageReadNext)
        serialPortHelper?.closeDevice()
        isOpen = false
        log(TAG, "main stop")
    }

    private fun writeAndReadCommDelay(delay: Long)
    {
        var message: Message = Message()
        message.what = messageReadNext
        if (delay > 0)
        {
            handler?.sendMessageDelayed(message, delay)
        }
        else
        {
            handler?.sendMessage(message)
        }
    }

    private fun initHandler()
    {
        handler = Handler{
            when(it.what) {

                0->{
                }
                // start usb write read
                messageReadNext->{
                    if (isOpen)
                    {
                        // 发送数据
                        var data = index2Option(requestIndex)
                        requestIndex += 1
                        if (requestIndex >= requestIndexMax)
                        {
                            requestIndex = 1
                        }
                        var serialData = SerialData()
                        var array = serialData.getSerialPackage(data.option, data.slaveIndex)
                        serialPortHelper?.addCommands(array);
                        writeAndReadCommDelay(SerialInterval)

                        log(TAG, "Comm write data")
                    } else {
                        log(TAG, "Comm is not open")
                    }
                }
                // refresh UI
                messageRefresh -> {
                    refreshUi(it.obj as SerialDataStruct)
                }
            }
            false
        }
    }

    private fun initPort(commPath: String): Boolean {
        //串口参数
        val serialPortConfig = SerialPortConfig()
        serialPortConfig.mode = 1 // 是否使用原始模式(Raw Mode)方式来通讯
        serialPortConfig.path = commPath // 串口地址
        serialPortConfig.baudRate = 115200 // 波特率
        serialPortConfig.dataBits = 8 // 数据位 取值 位 7或 8
        serialPortConfig.parity = 'N' // 检验类型 取值 N ,E, O
        serialPortConfig.stopBits = 1 // 停止位 取值 1 或者 2

        // 初始化串口
        serialPortHelper = SerialPortHelper(32)
        // 设置串口参数
        serialPortHelper?.setConfigInfo(serialPortConfig)
        // 开启串口
        if (!serialPortHelper?.openDevice()!!) {
            Toast.makeText(this, "串口打开失败！", Toast.LENGTH_LONG).show()
            return false;
        }
        // 数据接收回调
        serialPortHelper?.setSphResultCallback(object : SphResultCallback {
            override fun onSendData(sendCom: SphCmdEntity) {
                log(TAG, "发送命令：" + sendCom.commandsHex)
            }

            override fun onReceiveData(data: SphCmdEntity) {
                log(TAG, "收到命令：" + data.commandsHex)
                var parser = SerialData()
                var data = parser.parseSerialData(data.commands)
                var message: Message = Message()
                message.what = messageRefresh
                message.obj = data
                handler?.sendMessage(message)
            }

            override fun onComplete() {
                log(TAG, "完成")
            }
        })
        return true
    }

    private fun index2Option(index: Int): SerialDataStruct
    {
        var data = SerialDataStruct()
        data.option = (index % 2).toByte()
        data.slaveIndex = (index / 2 + 1).toByte()
        return data
    }

    private fun refreshUi(data: SerialDataStruct)
    {
        setViewValueByOption(data.option, data.value, getViewByData(data))
    }

    private fun getViewByData(data: SerialDataStruct): TextView?
    {
        var view: TextView? = null
        if (data.option == SerialOptionType.optionTypeTemperature)
        {
            when (data.slaveIndex)
            {
                0x01.toByte() -> {
                    view = temperatureValue1
                }
                0x02.toByte() -> {
                    view = temperatureValue2
                }
                0x03.toByte() -> {
                    view = temperatureValue3
                }
                0x04.toByte() -> {
                    view = temperatureValue4
                }
            }
        }
        else if (data.option == SerialOptionType.optionTypeHumidity)
        {
            when (data.slaveIndex)
            {
                0x01.toByte() -> {
                    view = humidityValue1
                }
                0x02.toByte() -> {
                    view = humidityValue2
                }
                0x03.toByte() -> {
                    view = humidityValue3
                }
                0x04.toByte() -> {
                    view = humidityValue4
                }
            }
        }
        return view
    }

    private fun setViewValueByOption(option: Byte, value: Float, view: TextView?)
    {
        if (option == SerialOptionType.optionTypeTemperature)
        {
            if (value != -100.0F)
            {
                view?.setText("${value}\u00B0C")
            }
            else
            {
                view?.setText(R.string.error)
            }
        }
        else if (option == SerialOptionType.optionTypeHumidity)
        {
            if (value != -100.0F) {
                view?.setText("${value}%")
            }
            else
            {
                view?.setText(R.string.error)
            }
        }
    }

    private fun log(TAG: String, message: String)
    {
        if (logEnable)
        {
            Log.d(TAG, message)
        }
    }
}