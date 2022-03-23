package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket20Temperature : UDPPacket(), SensorSpecificPacket {
	override var sensorId = 0
	var temperature = 0f
	override val packetId: Int
		get() = 20

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		sensorId = buf.get().toInt() and 0xFF
		temperature = buf.float
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}
}
