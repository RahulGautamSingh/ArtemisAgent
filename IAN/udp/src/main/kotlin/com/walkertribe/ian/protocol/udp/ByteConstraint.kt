package com.walkertribe.ian.protocol.udp

internal sealed interface ByteConstraint {
    @JvmInline
    value class Equals(val value: Byte) : ByteConstraint {
        override fun check(byte: Byte): Boolean = byte == value
    }

    class Range(val range: IntRange) : ByteConstraint {
        override fun check(byte: Byte): Boolean = byte in range
    }

    fun check(byte: Byte): Boolean
}
