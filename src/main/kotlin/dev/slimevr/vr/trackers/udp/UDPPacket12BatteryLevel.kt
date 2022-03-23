package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket12BatteryLevel : UDPPacket() {
	var voltage = 0f
	var level = 0f
	override val packetId: Int
		get() = 12

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		voltage = buf.float
		if (buf.remaining() > 3) {
			level = buf.float
		} else {
			level = voltage
			voltage = 0.0f
		}
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}
}
