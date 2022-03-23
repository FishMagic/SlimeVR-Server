package dev.slimevr.vr.trackers.udp

import com.jme3.math.Quaternion
import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket17RotationData : UDPPacket(), SensorSpecificPacket {
	override var sensorId = 0
	var dataType = 0
	val rotation = Quaternion()
	var calibrationInfo = 0
	override val packetId: Int
		get() = 17

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		sensorId = buf.get().toInt() and 0xFF
		dataType = buf.get().toInt() and 0xFF
		rotation[buf.float, buf.float, buf.float] = buf.float
		calibrationInfo = buf.get().toInt() and 0xFF
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}

	companion object {
		const val DATA_TYPE_NORMAL = 1
		const val DATA_TYPE_CORRECTION = 2
	}
}
