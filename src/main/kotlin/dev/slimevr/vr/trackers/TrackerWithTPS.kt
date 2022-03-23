package dev.slimevr.vr.trackers

interface TrackerWithTPS {
	val tps: Float

	fun dataTick()
}
