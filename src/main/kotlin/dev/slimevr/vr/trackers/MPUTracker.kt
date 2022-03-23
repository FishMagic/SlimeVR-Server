package dev.slimevr.vr.trackers

import dev.slimevr.vr.trackers.udp.TrackersUDPServer
import java.nio.ByteBuffer

class MPUTracker(trackerId: Int, name: String?, descriptiveName: String?, server: TrackersUDPServer?) :
	IMUTracker(trackerId, name!!, descriptiveName!!, server!!) {
	var newCalibrationData: ConfigurationData? = null

	class ConfigurationData {
		//accel offsets and correction matrix
		var A_B = FloatArray(3)
		var A_Ainv = Array(3) { FloatArray(3) }

		// mag offsets and correction matrix
		var M_B = FloatArray(3)
		var M_Ainv = Array(3) { FloatArray(3) }

		//raw offsets, determined for gyro at rest
		var G_off = FloatArray(3)
		var deviceId = -1
		var deviceMode = -1

		constructor(
			accelBasis: DoubleArray,
			accelAInv: DoubleArray,
			magBasis: DoubleArray,
			magAInv: DoubleArray,
			gyroOffset: DoubleArray
		) {
			A_B[0] = accelBasis[0].toFloat()
			A_B[1] = accelBasis[1].toFloat()
			A_B[2] = accelBasis[2].toFloat()
			A_Ainv[0][0] = accelAInv[0].toFloat()
			A_Ainv[0][1] = accelAInv[1].toFloat()
			A_Ainv[0][2] = accelAInv[2].toFloat()
			A_Ainv[1][0] = accelAInv[3].toFloat()
			A_Ainv[1][1] = accelAInv[4].toFloat()
			A_Ainv[1][2] = accelAInv[5].toFloat()
			A_Ainv[2][0] = accelAInv[6].toFloat()
			A_Ainv[2][1] = accelAInv[7].toFloat()
			A_Ainv[2][2] = accelAInv[8].toFloat()
			M_B[0] = magBasis[0].toFloat()
			M_B[1] = magBasis[1].toFloat()
			M_B[2] = magBasis[2].toFloat()
			M_Ainv[0][0] = magAInv[0].toFloat()
			M_Ainv[0][1] = magAInv[1].toFloat()
			M_Ainv[0][2] = magAInv[2].toFloat()
			M_Ainv[1][0] = magAInv[3].toFloat()
			M_Ainv[1][1] = magAInv[4].toFloat()
			M_Ainv[1][2] = magAInv[5].toFloat()
			M_Ainv[2][0] = magAInv[6].toFloat()
			M_Ainv[2][1] = magAInv[7].toFloat()
			M_Ainv[2][2] = magAInv[8].toFloat()
			G_off[0] = gyroOffset[0].toFloat()
			G_off[1] = gyroOffset[1].toFloat()
			G_off[2] = gyroOffset[2].toFloat()
		}

		constructor(buffer: ByteBuffer) {
			deviceMode = buffer.int
			deviceId = buffer.int
			// Data is read in reverse, because it was reversed when sending
			G_off[2] = buffer.float
			G_off[1] = buffer.float
			G_off[0] = buffer.float
			M_Ainv[2][2] = buffer.float
			M_Ainv[2][1] = buffer.float
			M_Ainv[2][0] = buffer.float
			M_Ainv[1][2] = buffer.float
			M_Ainv[1][1] = buffer.float
			M_Ainv[1][0] = buffer.float
			M_Ainv[0][2] = buffer.float
			M_Ainv[0][1] = buffer.float
			M_Ainv[0][0] = buffer.float
			M_B[2] = buffer.float
			M_B[1] = buffer.float
			M_B[0] = buffer.float
			A_Ainv[2][2] = buffer.float
			A_Ainv[2][1] = buffer.float
			A_Ainv[2][0] = buffer.float
			A_Ainv[1][2] = buffer.float
			A_Ainv[1][1] = buffer.float
			A_Ainv[1][0] = buffer.float
			A_Ainv[0][2] = buffer.float
			A_Ainv[0][1] = buffer.float
			A_Ainv[0][0] = buffer.float
			A_B[2] = buffer.float
			A_B[1] = buffer.float
			A_B[0] = buffer.float
		}

		fun toTextMatrix(): String {
			val sb = StringBuilder()
			sb.append(String.format("{%8.2f,%8.2f,%8.2f},\n", A_B[0], A_B[1], A_B[2]))
			sb.append(String.format("{{%9.5f,%9.5f,%9.5f},\n", A_Ainv[0][0], A_Ainv[0][1], A_Ainv[0][2]))
			sb.append(String.format(" {%9.5f,%9.5f,%9.5f},\n", A_Ainv[1][0], A_Ainv[1][1], A_Ainv[1][2]))
			sb.append(
				String.format(
					" {%9.5f,%9.5f,%9.5f}},\n",
					A_Ainv[2][0], A_Ainv[2][1], A_Ainv[2][2]
				)
			)
			sb.append(String.format("{%8.2f,%8.2f,%8.2f},\n", M_B[0], M_B[1], M_B[2]))
			sb.append(String.format("{{%9.5f,%9.5f,%9.5f},\n", M_Ainv[0][0], M_Ainv[0][1], M_Ainv[0][2]))
			sb.append(String.format(" {%9.5f,%9.5f,%9.5f},\n", M_Ainv[1][0], M_Ainv[1][1], M_Ainv[1][2]))
			sb.append(
				String.format(
					" {%9.5f,%9.5f,%9.5f}},\n",
					M_Ainv[2][0], M_Ainv[2][1], M_Ainv[2][2]
				)
			)
			sb.append(String.format("{%8.2f, %8.2f, %8.2f}};\n", G_off[0], G_off[1], G_off[2]))
			return sb.toString()
		}
	}
}
