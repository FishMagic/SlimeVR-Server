package dev.slimevr.vr.trackers.udp

import dev.slimevr.vr.trackers.SensorTap
import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket13Tap : UDPPacket(), SensorSpecificPacket {
	override var sensorId = 0
	var tap: SensorTap? = null
	override val packetId: Int
		get() = 13

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		sensorId = buf.get().toInt() and 0xFF
		tap = SensorTap(buf.get().toInt() and 0xFF)
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}
}
