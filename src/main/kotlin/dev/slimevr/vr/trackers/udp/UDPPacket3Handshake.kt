package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer

class UDPPacket3Handshake : UDPPacket() {
	var boardType = 0
	var imuType = 0
	var mcuType = 0
	var firmwareBuild = 0
	var firmware: String? = null
	var macString: String? = null
	override val packetId: Int
		get() = 3

	@Throws(IOException::class)
	override fun readData(buf: ByteBuffer) {
		if (buf.remaining() > 0) {
			val mac = ByteArray(6)
			if (buf.remaining() > 3) boardType = buf.int
			if (buf.remaining() > 3) imuType = buf.int
			if (buf.remaining() > 3) mcuType = buf.int // MCU TYPE
			if (buf.remaining() > 11) {
				buf.int // IMU info
				buf.int
				buf.int
			}
			if (buf.remaining() > 3) firmwareBuild = buf.int
			var length = 0
			if (buf.remaining() > 0) length =
				buf.get().toInt() // firmware version length is 1 longer than that because it's nul-terminated
			firmware = readASCIIString(buf, length)
			if (buf.remaining() >= mac.size) {
				buf[mac]
				macString = String.format(
					"%02X:%02X:%02X:%02X:%02X:%02X",
					mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]
				)
				if (macString == "00:00:00:00:00:00") macString = null
			}
		}
	}

	@Throws(IOException::class)
	override fun writeData(buf: ByteBuffer) {
		// Never sent back in current protocol
		// Handshake for RAW SlimeVR and legacy owoTrack has different packet id byte order from normal packets
		// So it's handled by raw protocol call
	}
}
