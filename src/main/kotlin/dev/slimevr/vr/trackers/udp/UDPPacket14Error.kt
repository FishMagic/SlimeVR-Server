package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket14Error : UDPPacket(), SensorSpecificPacket {
	override var sensorId = 0
	var errorNumber = 0
	override val packetId: Int
		get() = 14

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		sensorId = buf.get().toInt() and  0xFF
		errorNumber = buf.get().toInt() and 0xFF
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}
}
