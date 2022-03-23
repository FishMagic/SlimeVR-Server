package dev.slimevr.vr.trackers.udp

class UDPPacket1Heartbeat : UDPPacket0Heartbeat() {
	override val packetId: Int
		get() = 1
}
