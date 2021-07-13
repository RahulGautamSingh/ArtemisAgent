package com.walkertribe.ian.util

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.map

fun Arb.Companion.boolState(): Arb<BoolState> = Arb.boolean().map(BoolState.Companion::invoke)

fun BoolState.shouldBeTrue() {
    shouldBeKnown()
    booleanValue.shouldBeTrue()
}

fun BoolState.shouldBeFalse() {
    shouldBeKnown()
    booleanValue.shouldBeFalse()
}

fun BoolState.shouldBeUnknown() {
    isKnown.shouldBeFalse()
    booleanValue.shouldBeFalse()
}

fun BoolState.shouldBeKnown() {
    isKnown.shouldBeTrue()
}
