package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket11Serial : UDPPacket() {
	var serial: String? = null
	override val packetId: Int
		get() = 11

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		val length = buf.int
		val sb = StringBuilder(length)
		for (i in 0 until length) {
			val ch = Char(buf.get().toUShort())
			sb.append(ch)
		}
		serial = sb.toString()
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}
}
