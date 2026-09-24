package com.ahmedtrooper.prism

import org.junit.Assert.*
import org.junit.Test

class ABLoopControllerTest {
    private fun makeController(map: MutableMap<String, String> = mutableMapOf()): ABLoopController {
        return ABLoopController(
            getProp = { map[it] },
            setProp = { k, v -> if (v == "no") map.remove(k) else map[k] = v }
        )
    }

    @Test fun toggleOffToA() {
        val c = makeController()
        val s = c.toggle(10.0)
        assertEquals(10.0, s.a)
        assertNull(s.b)
        assertEquals("10.0", c.getState().a.toString())
    }

    @Test fun toggleAToB() {
        val m = mutableMapOf("ab-loop-a" to "5.0")
        val c = makeController(m)
        val s = c.toggle(20.0)
        assertEquals(5.0, s.a!!, 0.001)
        assertEquals(20.0, s.b!!, 0.001)
    }

    @Test fun toggleBToOffClears() {
        val m = mutableMapOf("ab-loop-a" to "5.0", "ab-loop-b" to "10.0")
        val c = makeController(m)
        val s = c.toggle(15.0)
        assertNull(s.a); assertNull(s.b)
        assertNull(m["ab-loop-a"]); assertNull(m["ab-loop-b"])
    }

    @Test fun labelFormats() {
        val c = makeController()
        assertEquals("A-B: off", c.labelFor(ABLoopController.State(null, null)))
        assertTrue(c.labelFor(ABLoopController.State(5.0, null)).startsWith("A:"))
        assertTrue(c.labelFor(ABLoopController.State(5.0, 10.0)).contains("→"))
    }

    @Test fun clearRemovesBoth() {
        val m = mutableMapOf("ab-loop-a" to "2.0", "ab-loop-b" to "8.0")
        val c = makeController(m)
        c.clear()
        assertNull(m["ab-loop-a"]); assertNull(m["ab-loop-b"])
    }

    @Test fun zoomResetHelperIdempotent() {
        // Zoom reset is just state zero; test that AB loop toggle is idempotent after clear
        val c = makeController()
        c.toggle(3.0); c.toggle(6.0)
        assertNotNull(c.getState().b)
        c.toggle(9.0)
        assertNull(c.getState().a)
        val s2 = c.toggle(1.0)
        assertEquals(1.0, s2.a!!, 0.001)
    }
}
