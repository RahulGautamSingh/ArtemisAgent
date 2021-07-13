package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.iface.ListenerRegistry
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.protocol.PacketTestListenerModule
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.prepare
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFloatLittleEndian
import io.ktor.utils.io.core.writeIntLittleEndian

class VersionPacketTest : PacketTestSpec.Server<VersionPacket>(
    specName = "VersionPacket",
    fixtures = listOf(VersionPacketFixture()),
    failures = listOf(
        object : Failure(TestPacketTypes.CONNECTED, "Fails to parse legacy version") {
            override val payloadGen: Gen<ByteReadPacket> = Arb.bind(
                Arb.int(),
                Arb.float(),
            ) { unknown, legacy ->
                buildPacket {
                    writeIntLittleEndian(unknown)
                    writeFloatLittleEndian(legacy)
                }
            }
        }
    ),
    isRequired = true,
) {
    override suspend fun DescribeSpecContainerScope.describeMore(
        readChannel: ByteReadChannel,
    ) {
        it("Sets version of PacketReader") {
            val listenerRegistry = ListenerRegistry().apply {
                register(PacketTestListenerModule)
            }
            val reader = PacketReader(readChannel, listenerRegistry)

            val fixture = fixtures[0]
            fixture.generator.checkAll { data ->
                readChannel.prepare(fixture.packetType, data.buildPayload())

                val result = reader.readPacket()
                result.shouldBeInstanceOf<ParseResult.Success>()

                val packet = result.packet
                packet.shouldBeInstanceOf<VersionPacket>()
                reader.version shouldBeEqual packet.version
            }

            reader.close()
        }
    }
}
