package dev.slimevr.vr.trackers.udp

interface SensorSpecificPacket {
	val sensorId: Int

	companion object {
		/**
		 * Sensor with id 255 is "global" representing a whole device
		 * @param sensorId
		 * @return
		 */
		fun isGlobal(sensorId: Int): Boolean {
			return sensorId == 255
		}
	}
}
