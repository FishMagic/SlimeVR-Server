package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket200ProtocolChange : UDPPacket() {
	var targetProtocol = 0
	var targetProtocolVersion = 0
	override val packetId: Int
		get() = 200

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		targetProtocol = buf.get().toInt() and 0xFF
		targetProtocolVersion = buf.get().toInt() and 0xFF
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		buf.put(targetProtocol.toByte())
		buf.put(targetProtocolVersion.toByte())
	}
}
