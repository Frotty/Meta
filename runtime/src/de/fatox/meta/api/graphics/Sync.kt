/*
 * Copyright (c) 2002-2012 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.fatox.meta.api.graphics

import org.lwjgl.glfw.GLFW
import kotlin.math.max

/**
 * A highly accurate sync method that continually adapts to the system
 * it runs on to provide reliable results.
 *
 * @author Riven
 * @author kappaOne
 */
object Sync {
	/** The time to sleep/yield until the next frame  */
	private var nextFrame: Long = 0

	/** whether the initialisation code has run  */
	private var initialised = false

	/** for calculating the averages the previous sleep/yield times are stored  */
	private val sleepDurations = RunningAvg(10)
	private val yieldDurations = RunningAvg(10)

	/**
	 * An accurate sync method that will attempt to run at a constant frame rate.
	 * It should be called once every frame.
	 *
	 * @param fps - the desired frame rate, in frames per second
	 */
	fun sync(fps: Int) {
		if (fps <= 0) return
		if (!initialised) initialise()
		try {
			// sleep until the average sleep time is greater than the time remaining till nextFrame
			run {
				var t0 = time
				var t1: Long
				while (nextFrame - t0 > sleepDurations.avg()) {
					Thread.sleep(1)
					sleepDurations.add(time.also { t1 = it } - t0) // update average sleep time
					t0 = t1
				}
			}

			// slowly dampen sleep average if too high to avoid yielding too much
			sleepDurations.dampenForLowResTicker()

			// yield until the average yield time is greater than the time remaining till nextFrame
			var t0 = time
			var t1: Long
			while (nextFrame - t0 > yieldDurations.avg()) {
				Thread.yield()
				yieldDurations.add(time.also { t1 = it } - t0) // update average yield time
				t0 = t1
			}
		} catch (e: InterruptedException) {
		}

		// schedule next frame, drop frame(s) if already too late for next frame
		nextFrame = max(nextFrame + NANOS_IN_SECOND / fps, time)
	}

	/**
	 * This method will initialise the sync method by setting initial
	 * values for sleepDurations/yieldDurations and nextFrame.
	 *
	 * If running on windows it will start the sleep timer fix.
	 */
	private fun initialise() {
		initialised = true
		sleepDurations.init(1000 * 1000.toLong())
		yieldDurations.init(((-(time - time) * 1.333).toInt()).toLong())
		nextFrame = time
		val osName = System.getProperty("os.name")
		if (osName.startsWith("Win")) {
			// On windows the sleep functions can be highly inaccurate by
			// over 10ms making it unusable. However it can be forced to
			// be a bit more accurate by running a separate sleeping daemon
			// thread.
			val timerAccuracyThread = Thread(Runnable {
				try {
					Thread.sleep(Long.MAX_VALUE)
				} catch (e: Exception) {
				}
			})
			timerAccuracyThread.name = "LWJGL3 Timer"
			timerAccuracyThread.isDaemon = true
			timerAccuracyThread.start()
		}
	}

	/**
	 * Get the system time in nano seconds
	 *
	 * @return will return the current time in nano's
	 */
	private val time: Long
		get() = (GLFW.glfwGetTime() * NANOS_IN_SECOND).toLong()

	private class RunningAvg(slotCount: Int) {
		private val slots: LongArray = LongArray(slotCount)
		private var offset: Int = 0
		fun init(value: Long) {
			while (offset < slots.size) {
				slots[offset++] = value
			}
		}

		fun add(value: Long) {
			slots[offset++ % slots.size] = value
			offset %= slots.size
		}

		fun avg(): Long {
			var sum: Long = 0
			for (slot in slots) {
				sum += slot
			}
			return sum / slots.size
		}

		fun dampenForLowResTicker() {
			if (avg() > DAMPEN_THRESHOLD) {
				for (i in slots.indices) {
					slots[i] *= DAMPEN_FACTOR.toLong()
				}
			}
		}

		companion object {
			private const val DAMPEN_THRESHOLD = 10 * 1000L * 1000L // 10ms
			private const val DAMPEN_FACTOR = 0.9f // don't change: 0.9f is exactly right!
		}
	}

	/** number of nano seconds in a second  */
	private const val NANOS_IN_SECOND = 1000L * 1000L * 1000L
}