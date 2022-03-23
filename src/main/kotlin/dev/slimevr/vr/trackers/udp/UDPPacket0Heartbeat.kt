package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

open class UDPPacket0Heartbeat : UDPPacket() {
	override val packetId: Int
		get() = 0

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		// Empty packet
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Empty packet
	}
}
