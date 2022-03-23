package dev.slimevr.vr.trackers.udp

import com.jme3.math.Quaternion
import java.io.IOException
import java.nio.ByteBuffer

open class UDPPacket1Rotation : UDPPacket(), SensorSpecificPacket {
	val rotation = Quaternion()
	override val packetId: Int
		get() = 1

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		rotation[buf.float, buf.float, buf.float] = buf.float
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
	}

	override val sensorId: Int
		get() = 0
}
