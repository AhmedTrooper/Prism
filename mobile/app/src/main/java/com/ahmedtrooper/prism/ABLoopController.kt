package com.ahmedtrooper.prism

/**
 * A-B loop helper: manages mpv's `ab-loop-a` and `ab-loop-b`.
 * Values are seconds; null/"no" means unset.
 * Production: use via PrismLib get/set; testable via injected IO lambda.
 */
class ABLoopController(
    private val getProp: (String) -> String? = { PrismLib.getPropertyString(it) },
    private val setProp: (String, String) -> Unit = { k, v -> PrismLib.setPropertyString(k, v) }
) {
    data class State(val a: Double?, val b: Double?)

    fun getState(): State {
        fun parse(k: String): Double? = getProp(k)?.let { v ->
            if (v == "no" || v.isBlank()) null else v.toDoubleOrNull()
        }
        return State(parse("ab-loop-a"), parse("ab-loop-b"))
    }

    fun labelFor(state: State, fmt: (Double) -> String = { Utils.prettyTime(it.toInt()) }): String = when {
        state.a == null && state.b == null -> "A-B: off"
        state.a != null && state.b == null -> "A: ${fmt(state.a)}"
        else -> "A-B: ${fmt(state.a!!)} → ${fmt(state.b!!)}"
    }

    /** MX-style: tap cycles: set A -> set B -> clear */
    fun toggle(currentPosSec: Double): State {
        val s = getState()
        return when {
            s.a == null -> {
                setProp("ab-loop-a", currentPosSec.toString())
                State(currentPosSec, null)
            }
            s.b == null -> {
                // Ensure B > A; if not, swap
                val a = s.a
                val (na, nb) = if (currentPosSec <= a) a to (a + 0.5) else a to currentPosSec
                if (nb == a + 0.5) setProp("ab-loop-a", na.toString())
                setProp("ab-loop-b", nb.toString())
                State(na, nb)
            }
            else -> {
                clear()
                State(null, null)
            }
        }
    }

    fun clear() {
        setProp("ab-loop-a", "no")
        setProp("ab-loop-b", "no")
    }

    fun toastText(state: State): String = labelFor(state)
}
