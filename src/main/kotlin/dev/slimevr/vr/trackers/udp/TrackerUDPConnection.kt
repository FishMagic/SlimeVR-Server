package dev.slimevr.vr.trackers.udp

import dev.slimevr.NetworkProtocol
import dev.slimevr.vr.trackers.IMUTracker
import java.net.InetAddress
import java.net.SocketAddress

class TrackerUDPConnection(var address: SocketAddress, var ipAddress: InetAddress) {
	var sensors: MutableMap<Int, IMUTracker> = HashMap()
	var lastPacket = System.currentTimeMillis()
	var lastPingPacketId = -1
	var lastPingPacketTime: Long = 0
	var name: String? = null
	var descriptiveName: String? = null
	var serialBuffer = StringBuilder()
	var lastSerialUpdate: Long = 0
	var lastPacketNumber: Long = -1
	var protocol: NetworkProtocol? = null
	var firmwareBuild = 0
	var timedOut = false
	fun isNextPacket(packetId: Long): Boolean {
		if (packetId != 0L && packetId <= lastPacketNumber) return false
		lastPacketNumber = packetId
		return true
	}

	override fun toString(): String {
		return "udp:/$ipAddress"
	}
}
