package com.nighthawkapps.wallet.android.ext

import java.util.concurrent.CopyOnWriteArraySet

import kotlin.math.roundToLong

internal typealias Leaf = String

/**
 * A tiny log.
 */
interface Twig {

    /**
     * Log the message. Simple.
     */
    fun twig(logMessage: String = "", priority: Int = 0)

    /**
     * Bundles twigs together.
     */
    operator fun

            plus(twig: Twig):

            Twig {
        // if the other twig is a composite twig, let it handle the addition
        return if (twig is CompositeTwig) twig.plus(this) else
            CompositeTwig(mutableListOf(this, twig))
    }

    companion object {

        /**
         * Access the trunk corresponding to this twig.
         */
        val trunk get() = Bush.trunk

        /**
         * Convenience function to just turn this thing on. Twigs are silent by default so this is
         * most useful to enable developer logging at the right time.
         */
        fun enabled(isEnabled: Boolean) {
            if (isEnabled) plant(TroubleshootingTwig())
            else plant(SilentTwig())
        }

        /**
         * Plants the twig, making it the one and only bush. Twigs can be bundled together to create
         * the appearance of multiple bushes (i.e `Twig.plant(twigA + twigB + twigC)`) even though
         * there's only ever one bush.
         */
        fun plant(rootTwig: Twig) {
            Bush.trunk = rootTwig
        }

        /**
         * Generate a leaf on the bush. Leaves show up in every log message as tags until they are
         * clipped.
         */
        fun sprout(leaf: Leaf) = Bush.leaves.add(leaf)

        /**
         * Clip a leaf from the bush. Clipped leaves no longer appear in logs.
         */
        fun clip(leaf: Leaf) = Bush.leaves.remove(leaf)

        /**
         * Clip all leaves from the bush.
         */
        fun prune() = Bush.leaves.clear()
    }
}

/**
 * A collection of tiny logs (twigs) consisting of one trunk and maybe some leaves. There can only
 * ever be one trunk. Trunks are created by planting a twig. Whenever a leaf sprouts, it will appear
 * as a tag on every log message until clipped.
 *
 * @see [Twig.plant]
 * @see [Twig.sprout]
 * @see [Twig.clip]
 */
object Bush {
    var trunk: Twig = SilentTwig()
    val leaves: MutableSet<Leaf> = CopyOnWriteArraySet<Leaf>()
}

/**
 * Makes a tiny log.
 */
inline fun twig(message: String, priority: Int = 0) = Bush.trunk.twig(message, priority)

/**
 * Makes an exception.
 */
inline fun twig(t: Throwable) = t.stackTraceToString().lines().forEach {
    twig(it)
}

/**
 * Times a tiny log.
 */
inline fun <R> twig(logMessage: String, priority: Int = 0, block: () -> R): R =
    Bush.trunk.twig(logMessage, priority, block)

/**
 * Meticulously times a tiny task.
 */
inline fun <R> twigTask(logMessage: String, priority: Int = 0, block: () -> R): R =
    Bush.trunk.twigTask(logMessage, priority, block)

/**
 * A tiny log that does nothing. No one hears this twig fall in the woods.
 */
class SilentTwig : Twig {

    /**
     * Shh.
     */
    override fun twig(logMessage: String, priority: Int) {
        // shh
    }
}

/**
 * A tiny log for detecting troubles. Aim at your troubles and pull the twigger.
 *
 * @param formatter a formatter for the twigs. The default one is pretty spiffy.
 * @param printer a printer for the twigs. The default is System.err.println.
 */
open class TroubleshootingTwig(
    val formatter: (String) -> String = spiffy(6),
    val printer: (String) -> Any = System.err::println,
    val minPriority: Int = 0
) : Twig {

    /**
     * Actually print and format the log message, unlike the SilentTwig, which does nothing.
     */
    override fun twig(logMessage: String, priority: Int) {
        if (priority >= minPriority) printer(formatter(logMessage))
    }

    companion object {

        /**
         * A tiny log formatter that makes twigs pretty spiffy.
         *
         * @param stackFrame the stack frame from which we try to derive the class. This can vary depending
         * on how the code is called so we expose it for flexibility. Jiggle the handle on this whenever the
         * line numbers appear incorrect.
         */
        fun spiffy(stackFrame: Int = 4, tag: String = "@TWIG"): (String) -> String =
            { logMessage: String ->
                val stack = Thread.currentThread().stackTrace[stackFrame]
                val time = String.format(
                    "$tag %1\$tD %1\$tI:%1\$tM:%1\$tS.%1\$tN",
                    System.currentTimeMillis()
                )
                val className = stack.className.split(".").lastOrNull()?.split("\$")?.firstOrNull()
                val tags = Bush.leaves.joinToString(" #", "#")
                "$time[$className:${stack.lineNumber}]($tags)    $logMessage"
            }
    }
}

/**
 * Since there can only ever be one trunk on the bush of twigs, this class lets
 * you cheat and make that trunk be a bundle of twigs.
 */
open class CompositeTwig(open val twigBundle: MutableList<Twig>) :
    Twig {
    override operator fun plus(twig: Twig): Twig {
        if (twig is CompositeTwig) twigBundle.addAll(twig.twigBundle) else twigBundle.add(twig)
        return this
    }

    override fun twig(logMessage: String, priority: Int) {
        for (twig in twigBundle) {
            twig.twig(logMessage, priority)
        }
    }
}

/**
 * Times a tiny log. Execute the block of code on the clock.
 */
inline fun <R> Twig.twig(logMessage: String, priority: Int = 0, block: () -> R): R {
    val start = System.currentTimeMillis()
    val result = block()
    val elapsed = (System.currentTimeMillis() - start)
    twig("$logMessage | ${elapsed}ms", priority)
    return result
}

/**
 * A tiny log task. Execute the block of code with some twigging around the outside. For silent
 * twigs, this adds a small amount of overhead at the call site but still avoids logging.
 *
 * note: being an extension function (i.e. static rather than a member of the Twig interface) allows
 *       this function to be inlined and simplifies its use with suspend functions
 *       (otherwise the function and its "block" param would have to suspend)
 */
inline fun <R> Twig.twigTask(logMessage: String, priority: Int = 0, block: () -> R): R {
    twig("$logMessage - started   | on thread ${Thread.currentThread().name}", priority)
    val start = System.nanoTime()
    val result = block()
    val elapsed = ((System.nanoTime() - start) / 1e5).roundToLong() / 10L
    twig(
        "$logMessage - completed | in $elapsed ms" + " on thread ${Thread.currentThread().name}",
        priority
    )
    return result
}
