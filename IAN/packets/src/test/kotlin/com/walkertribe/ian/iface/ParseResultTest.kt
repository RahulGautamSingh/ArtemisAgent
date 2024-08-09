package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.protocol.PacketTestListenerModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class ParseResultTest : DescribeSpec({
    val mockListener = mockk<ListenerModule>()

    afterSpec { clearAllMocks() }

    describe("ParseResult") {
        val unparsedPackets = mutableListOf<Packet.Raw>()

        describe("Processing") {
            val processing = mutableListOf<ParseResult.Processing>()

            it("Packet is unparsed") {
                checkAll(
                    Arb.int(),
                    Arb.byteArray(Arb.nonNegativeInt(UShort.MAX_VALUE.toInt()), Arb.byte()),
                ) { expectedPacketType, expectedPayload ->
                    val result = ParseResult.Processing(
                        expectedPacketType,
                        expectedPayload,
                    )

                    val packet = result.packet.shouldBeInstanceOf<Packet.Raw>()
                    packet.type shouldBeEqual expectedPacketType
                    packet.payload.toList() shouldContainExactly expectedPayload.toList()

                    processing.add(result)
                    unparsedPackets.add(packet)
                }
            }

            it("Cannot fire listeners") {
                processing.forEach {
                    shouldThrow<UnsupportedOperationException> { it.fireListeners() }
                }
            }
        }

        describe("Fail") {
            val fails = mutableListOf<ParseResult.Fail>()

            it("Constructor") {
                fails.addAll(
                    unparsedPackets.map {
                        ParseResult.Fail(
                            PacketException(
                                RuntimeException(),
                                it.type,
                                it.payload,
                            )
                        )
                    }.onEach {
                        shouldThrow<PacketException> { throw it.exception }
                    }
                )
            }

            it("Packet is generated from PacketException") {
                unparsedPackets.zip(fails).forEach { (unparsed, result) ->
                    val expectedPacketType = unparsed.type
                    val expectedPayload = unparsed.payload

                    val packet = result.packet.shouldBeInstanceOf<Packet.Raw>()
                    packet.type shouldBeEqual expectedPacketType
                    packet.payload.toList() shouldContainExactly expectedPayload.toList()
                }
            }

            it("Cannot add listeners") {
                fails.forEach {
                    shouldThrow<IllegalStateException> {
                        it.addListeners(listOf(mockListener, PacketTestListenerModule))
                    }
                }
            }

            it("Cannot fire listeners") {
                fails.forEach {
                    shouldThrow<UnsupportedOperationException> { it.fireListeners() }
                }
            }
        }

        describe("Interesting") {
            it("With listeners: true") {
                val result = ParseResult.Processing(0, byteArrayOf())
                result.addListeners(listOf(mockListener))
                result.isInteresting.shouldBeTrue()
            }

            it("Without listeners: false") {
                val result = ParseResult.Fail(PacketException())
                result.isInteresting.shouldBeFalse()
            }

            it("Copied from previous result") {
                val previous = ParseResult.Processing(0, byteArrayOf())
                previous.addListeners(listOf(mockListener))
                val success = ParseResult.Success(mockk(), previous)
                success.isInteresting.shouldBeTrue()
            }

            it("Can fire listeners") {
                val mockPacket = mockk<Packet.Server> {
                    every { offerTo(any()) } answers { callOriginal() }
                }
                val success = ParseResult.Success(mockPacket, ParseResult.Fail(PacketException()))
                success.addListeners(listOf(PacketTestListenerModule))
                success.fireListeners()
                PacketTestListenerModule.packets shouldContain mockPacket
            }

            PacketTestListenerModule.packets.clear()
        }
    }
})
