package dev.slimevr.vr.trackers.udp

class UDPPacket16Rotation2 : UDPPacket1Rotation() {
	override val packetId: Int
		get() = 16
	override val sensorId: Int
		get() = 1
}
