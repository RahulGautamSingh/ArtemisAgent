package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.core.writeIntLittleEndian

data object WelcomePacketFixture : PacketTestFixture.Server<WelcomePacket>(
    TestPacketTypes.PLAIN_TEXT_GREETING,
) {
    data class Data(val message: String) : PacketTestData.Server<WelcomePacket> {
        override val version: Version get() = Version.LATEST

        override fun buildPayload(): ByteReadPacket = buildPacket {
            writeIntLittleEndian(message.length)
            writeFully(Charsets.US_ASCII.encode(message))
        }

        override fun validate(packet: WelcomePacket) {
            packet.message shouldBeEqual message
        }
    }

    override val generator: Gen<Data> = Arb.bind()

    override suspend fun testType(packet: Packet.Server): WelcomePacket =
        packet.shouldBeInstanceOf()
}
