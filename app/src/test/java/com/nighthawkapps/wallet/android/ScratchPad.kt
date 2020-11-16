package com.nighthawkapps.wallet.android

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scanReduce
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ScratchPad {

    val t get() = System.currentTimeMillis()
    var t0 = 0L
    val Δt get() = t - t0

    @Test
    fun testMarblesCombine() = runBlocking {
        var started = false
        val flow = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9).onEach {
            delay(100)
            if (!started) {
                t0 = t
                started = true
            }
            println("$Δt\temitting $it")
        }
        val flow2 = flowOf("a", "b", "c", "d", "e", "f").onEach { delay(150); println("$Δt\temitting $it") }
        val flow3 = flowOf("A", "B").onEach { delay(450); println("$Δt\temitting $it") }
        combine(flow, flow2, flow3) { i, s, t -> "$i$s$t" }.onStart {
            t0 = t
        }.collect {
//            if (!started) {
//                println("$Δt until first emission")
//                t0 = t
//                started = true
//            }
            println("$Δt\t$it") // Will print "1a 2a 2b 2c"
        }
    }

    @Test
    fun testMarblesScan() = runBlocking {
        val flow = flowOf(1, 2, 3, 4, 5)

        flow.scanReduce { accumulator, value ->
            println("was: $accumulator  now: $value")
            value
        }.collect {
            println("got $it")
        }
    }
}
