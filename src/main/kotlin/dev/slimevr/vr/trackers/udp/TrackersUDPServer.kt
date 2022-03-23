package dev.slimevr.vr.trackers.udp

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import dev.slimevr.NetworkProtocol
import dev.slimevr.vr.trackers.IMUTracker
import dev.slimevr.vr.trackers.ReferenceAdjustedTracker
import dev.slimevr.vr.trackers.Tracker
import dev.slimevr.vr.trackers.TrackerStatus
import io.eiren.util.Util
import io.eiren.util.collections.FastList
import io.eiren.util.logging.LogManager
import org.apache.commons.lang3.ArrayUtils
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.function.Consumer

/**
 * Receives trackers data by UDP using extended owoTrack protocol.
 */
class TrackersUDPServer(private val port: Int, name: String, private val trackersConsumer: Consumer<Tracker>) :
	Thread(name) {
	private val buf = Quaternion()
	private val random = Random()
	private val connections: MutableList<TrackerUDPConnection?> = FastList()
	private val connectionsByAddress: MutableMap<InetAddress, TrackerUDPConnection?> = HashMap()
	private val connectionsByMAC: MutableMap<String, TrackerUDPConnection> = HashMap()
	private val broadcastAddresses = ArrayList<SocketAddress>()
	private val parser = UDPProtocolParser()
	private val rcvBuffer = ByteArray(512)
	private val bb = ByteBuffer.wrap(rcvBuffer).order(ByteOrder.BIG_ENDIAN)
	protected var socket: DatagramSocket? = null
	protected var lastKeepup = System.currentTimeMillis()

	init {
		try {
			val ifaces = NetworkInterface.getNetworkInterfaces()
			while (ifaces.hasMoreElements()) {
				val iface = ifaces.nextElement()
				// Ignore loopback, PPP, virtual and disabled devices
				if (iface.isLoopback || !iface.isUp || iface.isPointToPoint || iface.isVirtual) {
					continue
				}
				val iaddrs = iface.inetAddresses
				while (iaddrs.hasMoreElements()) {
					val iaddr = iaddrs.nextElement()
					// Ignore IPv6 addresses
					if (iaddr is Inet6Address) {
						continue
					}
					val iaddrParts = iaddr.hostAddress.split("\\.").toTypedArray()
					broadcastAddresses.add(
						InetSocketAddress(
							String.format(
								"%s.%s.%s.255",
								iaddrParts[0], iaddrParts[1], iaddrParts[2]
							),
							port
						)
					)
				}
			}
		} catch (e: Exception) {
			LogManager.log.severe("[TrackerServer] Can't enumerate network interfaces", e)
		}
	}

	@Throws(IOException::class)
	private fun setUpNewConnection(handshakePacket: DatagramPacket, handshake: UDPPacket3Handshake) {
		LogManager.log.info("[TrackerServer] Handshake received from " + handshakePacket.address + ":" + handshakePacket.port)
		val addr = handshakePacket.address
		var connection: TrackerUDPConnection?
		synchronized(connections) { connection = connectionsByAddress[addr] }
		if (connection == null) {
			connection = TrackerUDPConnection(handshakePacket.socketAddress, addr)
			connection!!.firmwareBuild = handshake.firmwareBuild
			if ((handshake.firmware == null) || (handshake.firmware!!.isEmpty())) {
				// Only old owoTrack doesn't report firmware and have different packet IDs with SlimeVR
				connection!!.protocol = NetworkProtocol.OWO_LEGACY
			} else {
				connection!!.protocol = NetworkProtocol.SLIMEVR_RAW
			}
			connection!!.name =
				if (handshake.macString != null) "udp://" + handshake.macString else "udp:/" + handshakePacket.address.toString()
			connection!!.descriptiveName = "udp:/" + handshakePacket.address.toString()
			var i = 0
			synchronized(connections) {
				if (handshake.macString != null && connectionsByMAC.containsKey(handshake.macString)) {
					val previousConnection = connectionsByMAC[handshake.macString]
					i = connections.indexOf(previousConnection)
					connectionsByAddress.remove(previousConnection!!.ipAddress)
					previousConnection.lastPacketNumber = 0
					previousConnection.ipAddress = addr
					previousConnection.address = handshakePacket.socketAddress
					previousConnection.name = connection!!.name
					previousConnection.descriptiveName = connection!!.descriptiveName
					connectionsByAddress[addr] = previousConnection
					LogManager.log.info("[TrackerServer] Tracker " + i + " handed over to address " + handshakePacket.socketAddress + ". Board type: " + handshake.boardType + ", imu type: " + handshake.imuType + ", firmware: " + handshake.firmware + " (" + connection!!.firmwareBuild + "), mac: " + handshake.macString + ", name: " + previousConnection.name)
				} else {
					i = connections.size
					connections.add(connection)
					connectionsByAddress[addr] = connection
					if (handshake.macString != null) {
						connectionsByMAC[handshake.macString!!] = connection!!
					}
					LogManager.log.info("[TrackerServer] Tracker " + i + " added with address " + handshakePacket.socketAddress + ". Board type: " + handshake.boardType + ", imu type: " + handshake.imuType + ", firmware: " + handshake.firmware + " (" + connection!!.firmwareBuild + "), mac: " + handshake.macString + ", name: " + connection!!.name)
				}
			}
			if (connection!!.protocol === NetworkProtocol.OWO_LEGACY || connection!!.firmwareBuild < 9) {
				// Set up new sensor for older firmware
				// Firmware after 7 should send sensor status packet and sensor will be created when it's received
				setUpSensor(connection!!, 0, handshake.imuType, 1)
			}
		}
		bb.limit(bb.capacity())
		bb.rewind()
		parser.writeHandshakeResponse(bb, connection)
		socket!!.send(DatagramPacket(rcvBuffer, bb.position(), connection!!.address))
	}

	@Throws(IOException::class)
	private fun setUpSensor(connection: TrackerUDPConnection, trackerId: Int, sensorType: Int, sensorStatus: Int) {
		LogManager.log.info("[TrackerServer] Sensor " + trackerId + " for " + connection.name + " status: " + sensorStatus)
		var imu = connection.sensors[trackerId]
		if (imu == null) {
			imu = IMUTracker(
				Tracker.getNextLocalTrackerId(),
				connection.name + "/" + trackerId,
				connection.descriptiveName + "/" + trackerId,
				this
			)
			connection.sensors[trackerId] = imu
			val adjustedTracker = ReferenceAdjustedTracker(imu)
			trackersConsumer.accept(adjustedTracker)
			LogManager.log.info("[TrackerServer] Added sensor " + trackerId + " for " + connection.name + ", type " + sensorType)
		}
		val status = UDPPacket15SensorInfo.getStatus(sensorStatus)
		if (status != null) imu.status = status
	}

	override fun run() {
		val serialBuffer2 = StringBuilder()
		try {
			socket = DatagramSocket(port)
			var prevPacketTime = System.currentTimeMillis()
			socket!!.soTimeout = 250
			while (true) {
				var received: DatagramPacket? = null
				try {
					var hasActiveTrackers = false
					for (tracker in connections) {
						if (tracker!!.sensors.size > 0) {
							hasActiveTrackers = true
							break
						}
					}
					if (!hasActiveTrackers) {
						val discoveryPacketTime = System.currentTimeMillis()
						if (discoveryPacketTime - prevPacketTime >= 2000) {
							for (addr in broadcastAddresses) {
								bb.limit(bb.capacity())
								bb.rewind()
								parser.write(bb, null, UDPPacket0Heartbeat())
								socket!!.send(DatagramPacket(rcvBuffer, bb.position(), addr))
							}
							prevPacketTime = discoveryPacketTime
						}
					}
					received = DatagramPacket(rcvBuffer, rcvBuffer.size)
					socket!!.receive(received)
					bb.limit(received.length)
					bb.rewind()
					var connection: TrackerUDPConnection?
					synchronized(connections) { connection = connectionsByAddress[received.address] }
					val packet = parser.parse(bb, connection)
					packet?.let { processPacket(received, it, connection) }
				} catch (_: SocketTimeoutException) {
				} catch (e: Exception) {
					LogManager.log.warning("[TrackerServer] Error parsing packet " + packetToString(received), e)
				}
				if (lastKeepup + 500 < System.currentTimeMillis()) {
					lastKeepup = System.currentTimeMillis()
					synchronized(connections) {
						for (i in connections.indices) {
							val conn = connections[i]
							bb.limit(bb.capacity())
							bb.rewind()
							parser.write(bb, conn, UDPPacket1Heartbeat())
							socket!!.send(DatagramPacket(rcvBuffer, bb.position(), conn!!.address))
							if (conn.lastPacket + 1000 < System.currentTimeMillis()) {
								val iterator: Iterator<IMUTracker> = conn.sensors.values.iterator()
								while (iterator.hasNext()) {
									val tracker = iterator.next()
									if (tracker.status == TrackerStatus.OK) tracker.status = TrackerStatus.DISCONNECTED
								}
								if (!conn.timedOut) {
									conn.timedOut = true
									LogManager.log.info("[TrackerServer] Tracker timed out: $conn")
								}
							} else {
								conn.timedOut = false
								val iterator: Iterator<IMUTracker> = conn.sensors.values.iterator()
								while (iterator.hasNext()) {
									val tracker = iterator.next()
									if (tracker.status == TrackerStatus.DISCONNECTED) tracker.status =
										TrackerStatus.OK
								}
							}
							if (conn.serialBuffer.length > 0) {
								if (conn.lastSerialUpdate + 500L < System.currentTimeMillis()) {
									serialBuffer2.append('[').append(conn.name).append("] ").append(conn.serialBuffer)
									println(serialBuffer2.toString())
									serialBuffer2.setLength(0)
									conn.serialBuffer.setLength(0)
								}
							}
							if (conn.lastPingPacketTime + 500 < System.currentTimeMillis()) {
								conn.lastPingPacketId = random.nextInt()
								conn.lastPingPacketTime = System.currentTimeMillis()
								bb.limit(bb.capacity())
								bb.rewind()
								bb.putInt(10)
								bb.putLong(0)
								bb.putInt(conn.lastPingPacketId)
								socket!!.send(DatagramPacket(rcvBuffer, bb.position(), conn.address))
							}
						}
					}
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			Util.close(socket)
		}
	}

	@Throws(IOException::class)
	protected fun processPacket(received: DatagramPacket, packet: UDPPacket, connection: TrackerUDPConnection?) {
		var tracker: IMUTracker?
		when (packet.packetId) {
			UDPProtocolParser.PACKET_HEARTBEAT -> {}
			UDPProtocolParser.PACKET_HANDSHAKE -> setUpNewConnection(received, packet as UDPPacket3Handshake)
			UDPProtocolParser.PACKET_ROTATION, UDPProtocolParser.PACKET_ROTATION_2 -> {
				if (connection == null) return
				val rotationPacket = packet as UDPPacket1Rotation
				buf.set(rotationPacket.rotation)
				offset.mult(buf, buf)
				tracker = connection.sensors[rotationPacket.sensorId]
				if (tracker == null) return
				tracker.rotQuaternion.set(buf)
				tracker.dataTick()
			}
			UDPProtocolParser.PACKET_ROTATION_DATA -> {
				if (connection == null) return
				val rotationData = packet as UDPPacket17RotationData
				tracker = connection.sensors[rotationData.sensorId]
				if (tracker == null) return
				buf.set(rotationData.rotation)
				offset.mult(buf, buf)
				when (rotationData.dataType) {
					UDPPacket17RotationData.DATA_TYPE_NORMAL -> {
						tracker.rotQuaternion.set(buf)
						tracker.calibrationStatus = rotationData.calibrationInfo
						tracker.dataTick()
					}
					UDPPacket17RotationData.DATA_TYPE_CORRECTION -> {
						tracker.rotMagQuaternion.set(buf)
						tracker.magCalibrationStatus = rotationData.calibrationInfo
						tracker.hasNewCorrectionData = true
					}
				}
			}
			UDPProtocolParser.PACKET_MAGNETOMETER_ACCURACY -> {
				if (connection == null) return
				val magAccuracy = packet as UDPPacket18MagnetometerAccuracy
				tracker = connection.sensors[magAccuracy.sensorId]
				if (tracker == null) return
				tracker.magnetometerAccuracy = magAccuracy.accuracyInfo
			}
			2, 4, 5, 9 -> {}
			8 -> if (connection == null) return
			UDPProtocolParser.PACKET_PING_PONG -> {
				if (connection == null) return
				val ping = packet as UDPPacket10PingPong
				if (connection.lastPingPacketId == ping.pingId) {
					var i = 0
					while (i < connection.sensors.size) {
						tracker = connection.sensors[i]
						tracker!!.ping = (System.currentTimeMillis() - connection.lastPingPacketTime).toInt() / 2
						tracker.dataTick()
						++i
					}
				} else {
					LogManager.log.debug("[TrackerServer] Wrong ping id " + ping.pingId + " != " + connection.lastPingPacketId)
				}
			}
			UDPProtocolParser.PACKET_SERIAL -> {
				if (connection == null) return
				val serial = packet as UDPPacket11Serial
				println("[" + connection.name + "] " + serial.serial)
			}
			UDPProtocolParser.PACKET_BATTERY_LEVEL -> {
				if (connection == null) return
				val battery = packet as UDPPacket12BatteryLevel
				if (connection.sensors.size > 0) {
					val trackers: Collection<IMUTracker> = connection.sensors.values
					val iterator = trackers.iterator()
					while (iterator.hasNext()) {
						val tr = iterator.next()
						tr.batteryVoltage = battery.voltage
						tr.batteryLevel = battery.level * 100
					}
				}
			}
			UDPProtocolParser.PACKET_TAP -> {
				if (connection == null) return
				val tap = packet as UDPPacket13Tap
				tracker = connection.sensors[tap.sensorId]
				if (tracker == null) return
				LogManager.log.info("[TrackerServer] Tap packet received from " + tracker.name + ": " + tap.tap)
			}
			UDPProtocolParser.PACKET_ERROR -> {
				val error = packet as UDPPacket14Error
				LogManager.log.severe("[TrackerServer] Error received from " + received.socketAddress + ": " + error.errorNumber)
				if (connection == null) return
				tracker = connection.sensors[error.sensorId]
				if (tracker == null) return
				tracker.status = TrackerStatus.ERROR
			}
			UDPProtocolParser.PACKET_SENSOR_INFO -> {
				if (connection == null) return
				val info = packet as UDPPacket15SensorInfo
				setUpSensor(connection, info.sensorId, info.sensorType, info.sensorStatus)
				// Send ack
				bb.limit(bb.capacity())
				bb.rewind()
				parser.writeSensorInfoResponse(bb, connection, info)
				socket!!.send(DatagramPacket(rcvBuffer, bb.position(), connection.address))
				LogManager.log.info("[TrackerServer] Sensor info for " + connection.descriptiveName + "/" + info.sensorId + ": " + info.sensorStatus)
			}
			UDPProtocolParser.PACKET_SIGNAL_STRENGTH -> {
				if (connection == null) return
				val signalStrength = packet as UDPPacket19SignalStrength
				if (connection.sensors.size > 0) {
					val trackers: Collection<IMUTracker> = connection.sensors.values
					val iterator = trackers.iterator()
					while (iterator.hasNext()) {
						val tr = iterator.next()
						tr.signalStrength = signalStrength.signalStrength
					}
				}
			}
			UDPProtocolParser.PACKET_TEMPERATURE -> {
				if (connection == null) return
				val temp = packet as UDPPacket20Temperature
				tracker = connection.sensors[temp.sensorId]
				if (tracker == null) return
				tracker.temperature = temp.temperature
			}
			else -> LogManager.log.warning("[TrackerServer] Skipped packet $packet")
		}
	}

	companion object {
		/**
		 * Change between IMU axes and OpenGL/SteamVR axes
		 */
		private val offset = Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X)
		private fun packetToString(packet: DatagramPacket?): String {
			val sb = StringBuilder()
			sb.append("DatagramPacket{")
			sb.append(packet!!.address.toString())
			sb.append(packet.port)
			sb.append(',')
			sb.append(packet.length)
			sb.append(',')
			sb.append(ArrayUtils.toString(packet.data))
			sb.append('}')
			return sb.toString()
		}
	}
}
