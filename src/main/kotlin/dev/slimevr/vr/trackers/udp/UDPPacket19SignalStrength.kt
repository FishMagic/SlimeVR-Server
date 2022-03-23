package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket19SignalStrength : UDPPacket(), SensorSpecificPacket {
	override var sensorId = 0
	var signalStrength = 0
	override val packetId: Int
		get() = 19

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		sensorId = buf.get().toInt() and 0xFF
		signalStrength = buf.get().toInt()
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}
}
