package com.walkertribe.ian.world

import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.isKnown
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.floats.shouldBeZero
import io.kotest.matchers.floats.shouldNotBeNaN
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

const val EPSILON = 0.00000001

fun Property.FloatProperty.shouldBeUnspecified() {
    hasValue.shouldBeFalse()
    value.shouldBeNaN()
    valueOrZero.shouldBeZero()
}

fun Property.FloatProperty.shouldBeSpecified() {
    hasValue.shouldBeTrue()
    value.shouldNotBeNaN()
}

infix fun Property.FloatProperty.shouldContainValue(expectedValue: Float) {
    shouldBeSpecified()
    value.shouldBeWithinPercentageOf(expectedValue, EPSILON)
}

infix fun Property.FloatProperty.shouldMatch(property: Property.FloatProperty) {
    hasValue shouldBeEqual property.hasValue
    shouldContainValue(property.value)
}

fun Property.ByteProperty.shouldBeUnspecified(unknownValue: Byte = -1) {
    hasValue.shouldBeFalse()
    value shouldBeEqual unknownValue
}

fun Property.ByteProperty.shouldBeSpecified(unknownValue: Byte = -1) {
    hasValue.shouldBeTrue()
    value shouldNotBeEqual unknownValue
}

infix fun Property.ByteProperty.shouldContainValue(expectedValue: Byte) {
    hasValue.shouldBeTrue()
    value shouldBeEqual expectedValue
}

infix fun Property.ByteProperty.shouldMatch(property: Property.ByteProperty) {
    hasValue shouldBeEqual property.hasValue
    shouldContainValue(property.value)
}

fun Property.IntProperty.shouldBeUnspecified(unknownValue: Int = -1) {
    hasValue.shouldBeFalse()
    value shouldBeEqual unknownValue
}

fun Property.IntProperty.shouldBeSpecified(unknownValue: Int = -1) {
    hasValue.shouldBeTrue()
    value shouldNotBeEqual unknownValue
}

infix fun Property.IntProperty.shouldContainValue(expectedValue: Int) {
    hasValue.shouldBeTrue()
    value shouldBeEqual expectedValue
}

infix fun Property.IntProperty.shouldMatch(property: Property.IntProperty) {
    hasValue shouldBeEqual property.hasValue
    shouldContainValue(property.value)
}

fun Property.BoolProperty.shouldBeUnspecified() {
    hasValue.shouldBeFalse()
    value.isKnown.shouldBeFalse()
    value.booleanValue.shouldBeFalse()
}

fun Property.BoolProperty.shouldBeSpecified() {
    hasValue.shouldBeTrue()
    value.isKnown.shouldBeTrue()
}

fun Property.BoolProperty.shouldBeTrue() {
    shouldContainValue(BoolState.True)
}

fun Property.BoolProperty.shouldBeFalse() {
    shouldContainValue(BoolState.False)
}

infix fun Property.BoolProperty.shouldContainValue(expectedValue: BoolState) {
    hasValue shouldBeEqual expectedValue.isKnown
    shouldContainValue(expectedValue.booleanValue)
    value shouldBeEqual expectedValue
}

infix fun Property.BoolProperty.shouldContainValue(expectedValue: Boolean) {
    shouldBeSpecified()
    value.booleanValue shouldBeEqual expectedValue
}

infix fun Property.BoolProperty.shouldMatch(property: Property.BoolProperty) {
    hasValue shouldBeEqual property.hasValue
    shouldContainValue(property.value)
}

fun Property.ObjectProperty<*>.shouldBeUnspecified() {
    hasValue.shouldBeFalse()
    value.shouldBeNull()
}

fun Property.ObjectProperty<*>.shouldBeSpecified() {
    hasValue.shouldBeTrue()
    value.shouldNotBeNull()
}

infix fun <V : Any> Property.ObjectProperty<V>.shouldContainValue(expectedValue: V) {
    hasValue.shouldBeTrue()

    val value = this.value
    value.shouldNotBeNull()
    value shouldBeEqual expectedValue
}

infix fun <V : Any> Property.ObjectProperty<V>.shouldMatch(
    property: Property.ObjectProperty<V>
) {
    hasValue shouldBeEqual property.hasValue
    value shouldBe property.value
}
