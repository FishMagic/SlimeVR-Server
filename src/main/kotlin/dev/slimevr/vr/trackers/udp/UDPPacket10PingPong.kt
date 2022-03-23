package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket10PingPong : UDPPacket {
	var pingId = 0

	constructor() {}
	constructor(pingId: Int) {
		this.pingId = pingId
	}

	override val packetId: Int
		get() = 10

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		pingId = buf.int
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		buf.putInt(pingId)
	}
}
