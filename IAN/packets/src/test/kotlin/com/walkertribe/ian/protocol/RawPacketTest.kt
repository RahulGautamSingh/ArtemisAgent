package com.walkertribe.ian.protocol

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll
import kotlin.reflect.full.primaryConstructor

class RawPacketTest : DescribeSpec({
    describe("RawPacket") {
        arrayOf(
            Packet.Raw.Unknown::class,
            Packet.Raw.Unparsed::class,
        ).forEach { packetClass ->
            describe(packetClass.java.simpleName) {
                val packets = mutableListOf<Packet.Raw>()

                it("Constructor") {
                    val constructor = packetClass.primaryConstructor.shouldNotBeNull()

                    checkAll(
                        Arb.int(),
                        Arb.byteArray(Arb.nonNegativeInt(UShort.MAX_VALUE.toInt()), Arb.byte()),
                    ) { packetType, payload ->
                        packets.add(constructor.call(packetType, payload))
                    }
                }
            }
        }
    }
})
