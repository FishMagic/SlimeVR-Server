package dev.slimevr.vr.trackers.udp

import dev.slimevr.vr.trackers.TrackerStatus
import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket15SensorInfo : UDPPacket(), SensorSpecificPacket {
	override var sensorId = 0
	var sensorStatus = 0
	var sensorType = 0
	override val packetId: Int
		get() = 15

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		sensorId = buf.get().toInt() and 0xFF
		sensorStatus = buf.get().toInt() and 0xFF
		if (buf.remaining() > 0) sensorType = buf.get().toInt() and 0xFF
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}

	companion object {
		fun getStatus(sensorStatus: Int): TrackerStatus? {
			when (sensorStatus) {
				0 -> return TrackerStatus.DISCONNECTED
				1 -> return TrackerStatus.OK
				2 -> return TrackerStatus.ERROR
			}
			return null
		}
	}
}
