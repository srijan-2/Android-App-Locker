package com.antimatter.applocker.security

object PinRules {
    /** True for PINs made of one repeated digit or a straight ascending/descending run. */
    fun isWeak(pin: String): Boolean {
        if (pin.length < 2) return true
        if (pin.all { it == pin[0] }) return true
        val steps = pin.zipWithNext { a, b -> b - a }
        return steps.all { it == 1 } || steps.all { it == -1 }
    }
}
