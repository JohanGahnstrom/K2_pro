package com.marewise.openfilament.rfid
import org.junit.Assert.*
import org.junit.Test
class CfsCodecTest {
 @Test fun goldenVector(){ val key=CfsCodec.deriveSectorKey(byteArrayOf(0x35,0xB9.toByte(),0x4A,0x19)); assertEquals("239E7FE23653",key.joinToString(""){"%02X".format(it)}) }
}
