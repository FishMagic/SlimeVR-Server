package dev.slimevr.vr.trackers

import java.util.function.Consumer

interface CalibratingTracker {
	fun startCalibration(calibrationDataConsumer: Consumer<String?>?)
	fun requestCalibrationData(calibrationDataConsumer: Consumer<String?>?)
	fun uploadNewCalibrationData()
}
