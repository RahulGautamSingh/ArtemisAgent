package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.filterNot
import io.kotest.property.exhaustive.map
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class ObjectUpdatePacketTest : PacketTestSpec.Server<ObjectUpdatePacket>(
    specName = "ObjectUpdatePacket",
    fixtures = ObjectUpdatePacketFixture.ALL,
    failures = listOf(
        object : Failure(TestPacketTypes.OBJECT_BIT_STREAM, "Fails to parse invalid object type") {
            private val validObjectTypeIDs = ObjectType.entries.map { it.id }.toSet() + setOf(0)

            override val payloadGen: Gen<ByteReadPacket> = Exhaustive.bytes().filterNot {
                validObjectTypeIDs.contains(it)
            }.map {
                buildPacket {
                    writeIntLittleEndian(it.toInt())
                }
            }
        }
    ),
)
