package com.example.openthermalprint.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class EscPosEncoderTest {

    @Test
    fun testInitPrinter() {
        val bytes = EscPosEncoder.CMD_INIT
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun testFeedPaper() {
        val bytes = EscPosEncoder.feedPaper(3)
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x64.toByte(), bytes[1])
        assertEquals(3.toByte(), bytes[2])
    }

    @Test
    fun testSetDensity() {
        val light = EscPosEncoder.setDensity(0)
        assertEquals(0x1B.toByte(), light[0])
        assertEquals(0x23.toByte(), light[1])
        assertEquals(0x4E.toByte(), light[2])
        assertEquals(12.toByte(), light[3])

        val dark = EscPosEncoder.setDensity(2)
        assertEquals(25.toByte(), dark[3])
    }

    @Test
    fun testFormatText() {
        val bytes = EscPosEncoder.formatText("Test", EscPosEncoder.TextAlign.CENTER, bold = true)
        // Check alignment: ESC a 1
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x61.toByte(), bytes[1])
        assertEquals(0x01.toByte(), bytes[2]) // Center
    }
}
