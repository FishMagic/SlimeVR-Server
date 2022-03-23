package dev.slimevr.bridge

import dev.slimevr.util.ann.VRServerThread
import dev.slimevr.vr.trackers.ShareableTracker

class OpenVRNativeBridge : Bridge {
	override fun dataRead() {
		// TODO Auto-generated method stub
	}

	override fun dataWrite() {
		// TODO Auto-generated method stub
	}

	override fun addSharedTracker(tracker: ShareableTracker?) {
		// TODO Auto-generated method stub
	}

	override fun removeSharedTracker(tracker: ShareableTracker?) {
		// TODO Auto-generated method stub
	}

	@VRServerThread
	override fun startBridge() {
	}
}
