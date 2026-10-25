package com.antimatter.applocker.ui.setup

/** PIN, monitoring and at least one protected app: the three steps protection needs. */
const val REQUIRED_STEPS = 3

fun requiredStepsDone(pinSet: Boolean, monitoringActive: Boolean, protectedCount: Int): Int =
    listOf(pinSet, monitoringActive, protectedCount > 0).count { it }
