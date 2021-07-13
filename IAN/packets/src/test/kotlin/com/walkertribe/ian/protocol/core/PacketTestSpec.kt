package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.ListenerRegistry
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.iface.TestListener
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketTestListenerModule
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.organizeTests
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.prepare
import com.walkertribe.ian.world.ArtemisObjectTestModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Ignored
import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.describeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Gen
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.EOFException
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.core.writeIntLittleEndian
import io.ktor.utils.io.core.writeShort
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll

@Ignored
sealed class PacketTestSpec<T : Packet>(
    val specName: String,
    open val fixtures: List<PacketTestFixture<T>>,
    autoIncludeTests: Boolean = true,
) : DescribeSpec() {
    init {
        if (autoIncludeTests) {
            @Suppress("LeakingThis")
            include(tests())
        }

        finalizeSpec {
            clearAllMocks()
            unmockkAll()
        }
    }

    abstract class Client<T : Packet.Client>(
        specName: String,
        final override val fixtures: List<PacketTestFixture.Client<T>>,
        autoIncludeTests: Boolean = true,
    ) : PacketTestSpec<T>(specName, fixtures, autoIncludeTests) {
        open suspend fun DescribeSpecContainerScope.describeMore() { }

        override fun tests(): TestFactory = describeSpec {
            val sendChannel = mockk<ByteWriteChannel>()
            val writer = PacketWriter(sendChannel)

            val ints = mutableListOf<Int>()
            val payloadSlot = slot<ByteReadPacket>()

            coEvery { sendChannel.writeInt(capture(ints)) } just runs
            coEvery { sendChannel.writePacket(capture(payloadSlot)) } just runs
            every { sendChannel.flush() } just runs

            finalizeSpec {
                every { sendChannel.close(any()) } returns true

                writer.close()
            }

            describe(specName) {
                organizeTests(fixtures) { fixture ->
                    it("Can write to PacketWriter") {
                        fixture.generator.checkAll { data ->
                            data.packet.writeTo(writer)
                            writer.flush()

                            fixture.validateHeader(ints.map(Int::reverseByteOrder))

                            val payload = payloadSlot.captured
                            data.validate(payload, fixture.expectedPayloadSize, fixture.packetType)

                            ints.clear()
                            packets.add(data.packet)
                        }
                    }

                    it("Packet type matches") {
                        packets.forEach { it.type shouldBeEqual fixture.packetType }
                    }

                    packets.clear()
                    fixture.describeMore(this)
                }

                describeMore()
            }
        }
    }

    abstract class Server<T : Packet.Server>(
        specName: String,
        final override val fixtures: List<PacketTestFixture.Server<T>>,
        private val failures: List<Failure> = listOf(),
        private val isRequired: Boolean = false,
        autoIncludeTests: Boolean = true,
    ) : PacketTestSpec<T>(specName, fixtures, autoIncludeTests) {
        abstract class Failure(val packetType: Int, val testName: String) {
            abstract val payloadGen: Gen<ByteReadPacket>
        }

        open suspend fun DescribeSpecContainerScope.describeMore(
            readChannel: ByteReadChannel,
        ) { }

        override fun tests(): TestFactory = describeSpec {
            val readChannel = mockk<ByteReadChannel>()

            describe(specName) {
                val expectedBehaviour = if (isRequired) "parse even" else "skip"
                val emptyListenerRegistry = ListenerRegistry()
                val testListenerRegistry = ListenerRegistry().apply {
                    register(PacketTestListenerModule)
                }
                val objectListenerRegistry = ListenerRegistry().apply {
                    register(ArtemisObjectTestModule)
                }

                organizeTests(fixtures) { fixture ->
                    PacketTestListenerModule.packets.clear()

                    val objectListenerBehaviour =
                        if (fixture.recognizeObjectListeners) "parse with" else "ignore"

                    val testCases = mutableListOf(
                        Triple(
                            "Can read from PacketReader",
                            testListenerRegistry,
                            true,
                        ),
                        Triple(
                            "Will $expectedBehaviour without listeners",
                            emptyListenerRegistry,
                            isRequired,
                        ),
                    )
                    if (!isRequired) {
                        testCases.add(
                            Triple(
                                "Will $objectListenerBehaviour object listeners",
                                objectListenerRegistry,
                                fixture.recognizeObjectListeners,
                            ),
                        )
                    }

                    withData(
                        nameFn = { it.first },
                        testCases,
                    ) { (_, listenerRegistry, expectPacket) ->
                        val reader = PacketReader(readChannel, listenerRegistry)

                        fixture.generator.checkAll { data ->
                            reader.version = data.version
                            readChannel.prepare(fixture.packetType, data.buildPayload())

                            if (expectPacket) {
                                val result = reader.readPacket()
                                result.shouldBeInstanceOf<ParseResult.Success>()

                                val packet = fixture.testType(result.packet)
                                data.validate(packet)
                                packets.add(packet)
                                fixture.afterTest(data)
                            } else {
                                shouldThrow<EOFException> { reader.readPacket() }
                            }
                        }

                        reader.close()
                    }

                    it("Can offer to listener modules") {
                        packets.forEach(testListenerRegistry::offer)
                        PacketTestListenerModule.packets shouldContainExactly packets
                    }

                    packets.clear()

                    fixture.describeMore(this)
                }

                PacketTestListenerModule.packets.clear()

                describeMore(readChannel)
                describeFailures(readChannel)
            }
        }

        private suspend fun DescribeSpecContainerScope.describeFailures(
            readChannel: ByteReadChannel,
        ) {
            if (failures.isNotEmpty()) {
                val reader = PacketReader(
                    readChannel,
                    ListenerRegistry().apply { register(TestListener.module) }
                )

                withData(nameFn = { it.testName }, failures) {
                    it.payloadGen.checkAll { payload ->
                        readChannel.prepare(it.packetType, payload)

                        val result = reader.readPacket()
                        result.shouldBeInstanceOf<ParseResult.Fail>()
                    }
                }

                reader.close()
            }
        }
    }

    protected val packets = mutableListOf<T>()

    abstract fun tests(): TestFactory

    companion object {
        fun BytePacketBuilder.writeString(str: String) {
            writeIntLittleEndian(str.length + 1)
            writeFully(Charsets.UTF_16LE.encode(str))
            writeShort(0)
        }
    }
}
