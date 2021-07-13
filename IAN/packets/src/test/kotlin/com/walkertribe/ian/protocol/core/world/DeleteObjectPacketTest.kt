package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.int
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class DeleteObjectPacketTest : PacketTestSpec.Server<DeleteObjectPacket>(
    specName = "DeleteObjectPacket",
    fixtures = DeleteObjectPacketFixture.allFixtures(),
    failures = listOf(
        object : Failure(TestPacketTypes.OBJECT_DELETE, "Fails to parse invalid object types") {
            private val validObjectTypeIDs = ObjectType.entries.map { it.id }.toSet()

            override val payloadGen: Gen<ByteReadPacket> = Arb.bind(
                Arb.byte().filterNot(validObjectTypeIDs::contains),
                Arb.int(),
            ) { type, id ->
                buildPacket {
                    writeByte(type)
                    writeIntLittleEndian(id)
                }
            }
        }
    ),
)
