package dev.slimevr.vr.trackers.udp

import java.io.IOException
import java.nio.ByteBuffer
import kotlin.experimental.and

abstract class UDPPacket {
	abstract val packetId: Int

	@Throws(IOException::class)
	abstract fun readData(buf: ByteBuffer)
	@Throws(IOException::class)
	abstract fun writeData(buf: ByteBuffer)
	override fun toString(): String {
		val sb = StringBuilder()
		sb.append(javaClass.simpleName)
		sb.append('{')
		sb.append(packetId)
		if (this is SensorSpecificPacket) {
			sb.append(",sensor:")
			sb.append((this as SensorSpecificPacket).sensorId)
		}
		sb.append('}')
		return sb.toString()
	}

	companion object {
		/**
		 * Naively read null-terminated ASCII string from the byte buffer
		 * @param buf
		 * @return
		 * @throws IOException
		 */
		@Throws(IOException::class)
		fun readASCIIString(buf: ByteBuffer): String {
			val sb = StringBuilder()
			while (true) {
				val c: Char = (buf.get() and 0xFF.toByte()).toInt().toChar()
				if (c.code == 0) break
				sb.append(c)
			}
			return sb.toString()
		}

		@Throws(IOException::class)
		fun readASCIIString(buf: ByteBuffer, length: Int): String {
			var tempLength = length
			val sb = StringBuilder()
			while (tempLength-- > 0) {
				val c: Char = (buf.get() and 0xFF.toByte()).toInt().toChar()
				if (c.code == 0) break
				sb.append(c)
			}
			return sb.toString()
		}

		/**
		 * Naively write null-terminated ASCII string to byte buffer
		 * @param str
		 * @param buf
		 * @throws IOException
		 */
		@Throws(IOException::class)
		fun writeASCIIString(str: String, buf: ByteBuffer) {
			for (element in str) {
				buf.put((element.code and 0xFF).toByte())
			}
			buf.put(0.toByte())
		}
	}
}
