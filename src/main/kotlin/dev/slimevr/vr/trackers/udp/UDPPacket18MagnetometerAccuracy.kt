package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket18MagnetometerAccuracy : UDPPacket(), SensorSpecificPacket {
	override var sensorId = 0
	var accuracyInfo = 0f
	override val packetId: Int
		get() = 18

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		sensorId = buf.get().toInt() and 0xFF
		accuracyInfo = buf.float
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}
}
