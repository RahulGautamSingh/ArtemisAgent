package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.Packet
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.property.Gen
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.core.writeIntLittleEndian
import io.ktor.utils.io.core.writeShort
import io.ktor.utils.io.errors.EOFException
import io.ktor.utils.io.writeIntLittleEndian
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

sealed class PacketTestFixture<T : Packet>(val packetType: Int) {
    abstract class Client<T : Packet.Client>(
        packetType: Int,
        val expectedPayloadSize: Int,
    ) : PacketTestFixture<T>(packetType) {
        abstract val generator: Gen<PacketTestData.Client<T>>

        private val expectedHeader: List<Int> by lazy {
            listOf(
                Packet.HEADER,
                expectedPayloadSize + Packet.PREAMBLE_SIZE,
                Origin.CLIENT.value,
                0,
                expectedPayloadSize + Int.SIZE_BYTES,
            )
        }

        fun validateHeader(header: List<Int>) {
            header shouldContainExactly expectedHeader
        }
    }

    abstract class Server<T : Packet.Server>(
        packetType: Int,
        val recognizeObjectListeners: Boolean = false,
    ) : PacketTestFixture<T>(packetType) {
        abstract val generator: Gen<PacketTestData.Server<T>>

        abstract suspend fun testType(packet: Packet.Server): T

        open fun afterTest(data: PacketTestData.Server<T>) { }
    }

    open val specName: String = ""
    open val groupName: String = ""

    open suspend fun describeMore(scope: DescribeSpecContainerScope) { }

    companion object {
        suspend fun ByteReadChannel.prepare(
            packetType: Int,
            payload: ByteReadPacket,
        ) {
            val ints = mutableListOf<Int>()
            val payloadSlot = slot<ByteReadPacket>()

            val sendChannel = mockk<ByteWriteChannel> {
                coJustRun { writeInt(capture(ints)) }
                coJustRun { writePacket(capture(payloadSlot)) }
                every { flush() } answers {
                    clearMocks(this@prepare)
                    val packet = payloadSlot.captured

                    coEvery { readInt() } returnsMany ints andThenThrows EOFException()
                    coEvery { readPacket(packet.remaining.toInt()) } returns packet
                    every { cancel(any()) } returns true
                }
            }

            sendChannel.writePacketWithHeader(packetType, payload)
            clearMocks(sendChannel)
        }

        suspend fun ByteWriteChannel.writePacketWithHeader(
            packetType: Int,
            payload: ByteReadPacket,
        ) {
            val payloadSize = payload.remaining.toInt()

            writeIntLittleEndian(Packet.HEADER)
            writeIntLittleEndian(payloadSize + Packet.PREAMBLE_SIZE)
            writeIntLittleEndian(Origin.SERVER.value)
            writeIntLittleEndian(0)
            writeIntLittleEndian(payloadSize + Int.SIZE_BYTES)
            writeIntLittleEndian(packetType)

            writePacket(payload)
            flush()
        }

        suspend fun <F : PacketTestFixture<*>> DescribeSpecContainerScope.organizeTests(
            fixtures: List<F>,
            describeTests: suspend DescribeSpecContainerScope.(F) -> Unit,
        ) {
            fixtures.groupBy { it.groupName }.forEach { (groupName, list) ->
                if (groupName.isBlank()) {
                    listTests(list, describeTests)
                } else {
                    describe(groupName) {
                        listTests(list, describeTests)
                    }
                }
            }
        }

        private suspend fun <F : PacketTestFixture<*>> DescribeSpecContainerScope.listTests(
            fixtures: List<F>,
            describeTests: suspend DescribeSpecContainerScope.(F) -> Unit,
        ) {
            if (fixtures.size == 1) {
                val fixture = fixtures[0]
                describeTests(fixture)
            } else {
                fixtures.forEach { fixture ->
                    describe(fixture.specName) {
                        describeTests(fixture)
                    }
                }
            }
        }

        fun BytePacketBuilder.writeString(str: String) {
            writeIntLittleEndian(str.length + 1)
            writeFully(Charsets.UTF_16LE.encode(str))
            writeShort(0)
        }
    }
}
