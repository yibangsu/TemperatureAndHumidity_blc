package com.alw.th.serial

class SerialChecksum {
    public fun getCheckSum(array: ByteArray, size: Int): Byte
    {
        var checksum: Byte = 0x00
        for (index in 0..size)
        {
            checksum = (checksum + array[index]).toByte()
        }
        return checksum
    }
}