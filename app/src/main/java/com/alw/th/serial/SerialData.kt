package com.alw.th.serial

import android.util.Log

class SerialData {

    private val TAG = "SerialData"
    private var logEnable = false

    private val requestPackageSize = 14
    private val responsePackegeSize = 15

    public fun getSerialPackage(optionType: Byte,  slaveIndex: Byte): ByteArray?
    {
        when(optionType)
        {
            SerialOptionType.optionTypeVersion-> {
                return byteArrayOf(0x55, 0x05, 0x00, 0x00, 0x00,
                    0xFF.toByte(), 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x5B, 0x0D)
            }
            SerialOptionType.optionTypeHumidity-> {
                return getDataPackageByOptAndIndex(optionType, slaveIndex)
            }
            SerialOptionType.optionTypeTemperature -> {
                return getDataPackageByOptAndIndex(optionType, slaveIndex)
            }
            else -> {
                log(TAG, "Unknown option")
                return null
            }
        }

        return null
    }

    private fun getDataPackageByOptAndIndex(opt: Byte, index: Byte): ByteArray
    {
        var data: ByteArray = ByteArray(requestPackageSize)
        var checksumUtil = SerialChecksum()
        data[0] = 0x55
        data[1] = 0x05
        data[2] = 0x00
        data[3] = 0x00
        data[4] = 0x00
        data[5] = 0xFF.toByte()
        data[6] = 0x01
        data[7] = 0x08
        data[8] = 0x05
        data[9] = opt
        data[10] = index
        data[11] = 0x00
        data[12] = checksumUtil.getCheckSum(data, 11)
        data[13] = 0x0D
        return data
    }

    public fun parseSerialData(array: ByteArray): SerialDataStruct
    {
        var data = SerialDataStruct()
        if (array.size >= responsePackegeSize)
        {
            var checksumUtil = SerialChecksum()
            var checksum: Byte = checksumUtil.getCheckSum(array, responsePackegeSize - 3)
            if (checksum != array[responsePackegeSize - 2])
            {
                log(TAG, "parseSerialData checksum error, checksumExcepted=${checksum} checksum=${array[responsePackegeSize - 2]} opt=${array[9]}, index=${array[10]}")
                return data
            }
            else if (array[responsePackegeSize - 1] != 0x0D.toByte())
            {
                log(TAG, "parseSerialData end error, end=${array[responsePackegeSize - 1]} opt=${array[9]}, index=${array[10]}")
                return data
            }
            data.option = array[9]
            data.slaveIndex = array[10]
            data.value = parseSerialValueData(data.option, array[11], array[12])
            log(TAG, "parseSerialData opt=${data.option}, index=${data.slaveIndex} value=${data.value}")
        }
        return data
    }

    private fun parseSerialValueData(option: Byte, valueHigh: Byte, valueLow: Byte): Float
    {
        var valueFloat: Float = 0F
        var valueHighLong: Long = valueHigh.toLong().and(0x00FF)
        var valueLowLong: Long = valueLow.toLong().and(0x00FF)
        log(TAG, "valueHigh=${valueHighLong} valueLow=${valueLowLong}")
        if (valueHigh == 0xFF.toByte() && valueLow == 0xFF.toByte())
        {
            return -100.0F
        }

        when (option)
        {
            SerialOptionType.optionTypeTemperature -> {
                var negative: Boolean = valueHighLong.and(0x80) == 0x80.toLong()
                Log.d(TAG, "temperature negative=${negative}")
                var valueLong: Long = valueHighLong.and(0x7F)
                Log.d(TAG, "temperature valueInt=${valueLong}")
                valueLong = valueLong.shl(8).or(valueLowLong)
                Log.d(TAG, "temperature valueInt=${valueLong}")
                valueFloat = valueLong.toFloat() / 10
                if (negative)
                {
                    valueFloat = -valueFloat
                }
            }
            SerialOptionType.optionTypeHumidity -> {
                var valueLong = valueHighLong.shl(8).or(valueLowLong)
                Log.d(TAG, "humidity valueInt=${valueLong}")
                valueFloat = valueLong.toFloat() / 10
            }
        }
        return valueFloat
    }

    private fun log(TAG: String, message: String)
    {
        if (logEnable)
        {
            Log.d(TAG, message)
        }
    }
}