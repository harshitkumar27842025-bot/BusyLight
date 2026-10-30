package com.example.busylight

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager

/** Supports Kuando/Plenom Busylight (Alpha, UC, Omega), Luxafor Flag and Blink(1) over Android USB host. */
class UsbLight(private val ctx: Context) {
    private val usb = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
    private var dev: UsbDevice? = null
    private var conn: UsbDeviceConnection? = null
    private var ifaceId = 0

    val isOpen get() = conn != null
    val present get() = findDevice() != null

    private fun isKuando(d: UsbDevice) = d.vendorId == 0x27BB && d.productId in 0x3BCA..0x3BCF
    private fun isLuxafor(d: UsbDevice) = d.vendorId == 0x04D8 && d.productId == 0xF372
    private fun isBlink1(d: UsbDevice) = d.vendorId == 0x27B8 && d.productId == 0x01ED
    private fun findDevice(): UsbDevice? =
        usb.deviceList.values.firstOrNull { isKuando(it) || isLuxafor(it) || isBlink1(it) }

    private fun open(): Boolean {
        if (conn != null) return true
        val d = findDevice() ?: return false
        if (!usb.hasPermission(d)) {
            val pi = PendingIntent.getBroadcast(
                ctx, 0,
                Intent("com.example.busylight.USB_PERMISSION").setPackage(ctx.packageName),
                PendingIntent.FLAG_MUTABLE
            )
            usb.requestPermission(d, pi)   // user taps "Allow"; we retry on next poll
            return false
        }
        val c = usb.openDevice(d) ?: return false
        var iface = d.getInterface(0)
        for (i in 0 until d.interfaceCount) {
            if (d.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_HID) { iface = d.getInterface(i); break }
        }
        c.claimInterface(iface, true)
        ifaceId = iface.id
        dev = d; conn = c
        return true
    }

    /** Kuando Busylight protocol rev 2.2: 64 bytes = 7 steps x 8 bytes + footer with checksum. */
    private fun kuandoPacket(r: Int, g: Int, b: Int): ByteArray {
        val p = ByteArray(64)
        p[0] = 0x10                                   // step 0: jump to step 0 (hold)
        p[2] = (r * 100 / 255).toByte()               // intensities are 0..100
        p[3] = (g * 100 / 255).toByte()
        p[4] = (b * 100 / 255).toByte()
        p[7] = 0x80.toByte()                          // audio setting applies: silent
        for (i in 58..61) p[i] = 0xFF.toByte()
        var sum = 0
        for (i in 0..61) sum += p[i].toInt() and 0xFF
        p[62] = (sum shr 8).toByte()
        p[63] = (sum and 0xFF).toByte()
        return p
    }

    @Synchronized
    fun set(r: Int, g: Int, b: Int): Boolean {
        if (!open()) return false
        val d = dev!!; val c = conn!!
        val res = when {
            isKuando(d) -> {
                val buf = kuandoPacket(r, g, b)
                c.controlTransfer(0x21, 0x09, 0x0200, ifaceId, buf, buf.size, 1000)   // HID SET_REPORT (output)
            }
            isBlink1(d) -> {
                val buf = byteArrayOf(1, 'n'.code.toByte(), r.toByte(), g.toByte(), b.toByte(), 0, 0, 0, 0)
                c.controlTransfer(0x21, 0x09, 0x0301, ifaceId, buf, buf.size, 1000)
            }
            else -> {
                val buf = byteArrayOf(1, 255.toByte(), r.toByte(), g.toByte(), b.toByte(), 0, 0, 0)
                c.controlTransfer(0x21, 0x09, 0x0200, ifaceId, buf, buf.size, 1000)
            }
        }
        if (res < 0) { close(); return false }
        return true
    }

    @Synchronized
    fun close() {
        conn?.close(); conn = null; dev = null
    }
}
