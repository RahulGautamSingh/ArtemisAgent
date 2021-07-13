package com.walkertribe.ian.protocol.udp

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.util.Enumeration

class PrivateNetworkAddressTest : DescribeSpec({
    finalizeSpec { unmockkAll() }

    describe("PrivateNetworkAddress") {
        it("Default") {
            PrivateNetworkAddress.DEFAULT.hostAddress shouldBeEqual "255.255.255.255"
        }

        lateinit var validAddress: PrivateNetworkAddress

        it("At least one can be found with a valid broadcast address") {
            validAddress = PrivateNetworkAddress.findAll().shouldNotBeEmpty().first()
        }

        it("Valid broadcast address is guessed") {
            val address = PrivateNetworkAddress.guessBest().shouldNotBeNull()
            address.hostAddress shouldBeEqual validAddress.hostAddress
        }

        mockkStatic(NetworkInterface::getNetworkInterfaces)

        it("Empty list of addresses if there are no network interfaces") {
            every { NetworkInterface.getNetworkInterfaces() } returns null

            PrivateNetworkAddress.findAll().shouldBeEmpty()
        }

        it("Does not return loopback address") {
            every {
                NetworkInterface.getNetworkInterfaces()
            } returns mockk<Enumeration<NetworkInterface>> {
                every { hasMoreElements() } returnsMany listOf(true, false)
                every { nextElement() } returns mockk<NetworkInterface> {
                    every { isLoopback } returns true
                }
            }

            PrivateNetworkAddress.findAll().shouldBeEmpty()
        }

        it("Does not return address of interface that is down") {
            every {
                NetworkInterface.getNetworkInterfaces()
            } returns mockk<Enumeration<NetworkInterface>> {
                every { hasMoreElements() } returnsMany listOf(true, false)
                every { nextElement() } returns mockk<NetworkInterface> {
                    every { isLoopback } returns false
                    every { isUp } returns false
                }
            }

            PrivateNetworkAddress.findAll().shouldBeEmpty()
        }

        it("Does not return null addresses") {
            every {
                NetworkInterface.getNetworkInterfaces()
            } returns mockk<Enumeration<NetworkInterface>> {
                every { hasMoreElements() } returnsMany listOf(true, true, false)
                every { nextElement() } returnsMany listOf(
                    mockk<NetworkInterface> {
                        every { isLoopback } returns false
                        every { isUp } returns true
                        every { interfaceAddresses } returns listOf(null)
                    },
                    mockk<NetworkInterface> {
                        every { isLoopback } returns false
                        every { isUp } returns true
                        every { interfaceAddresses } returns listOf(
                            mockk<InterfaceAddress> {
                                every { address } returns null
                            }
                        )
                    }
                )
            }

            PrivateNetworkAddress.findAll().shouldBeEmpty()
        }

        it("Does not return non-private addresses") {
            val validFirstBytes = setOf(10.toByte(), (-44).toByte(), (-64).toByte())

            Arb.byteArray(Arb.of(4), Arb.byte()).filter {
                !validFirstBytes.contains(it[0])
            }.checkAll { bytes ->
                every {
                    NetworkInterface.getNetworkInterfaces()
                } returns mockk<Enumeration<NetworkInterface>> {
                    every { hasMoreElements() } returnsMany listOf(true, false)
                    every { nextElement() } returns mockk<NetworkInterface> {
                        every { isLoopback } returns false
                        every { isUp } returns true
                        every { interfaceAddresses } returns listOf(
                            mockk<InterfaceAddress> {
                                every { address } returns mockk<InetAddress> {
                                    every { address } returns bytes
                                }
                            }
                        )
                    }
                }

                PrivateNetworkAddress.findAll().shouldBeEmpty()
            }
        }

        it("Does not return non-broadcast addresses") {
            every {
                NetworkInterface.getNetworkInterfaces()
            } returns mockk<Enumeration<NetworkInterface>> {
                every { hasMoreElements() } returnsMany listOf(true, false)
                every { nextElement() } returns mockk<NetworkInterface> {
                    every { isLoopback } returns false
                    every { isUp } returns true
                    every { interfaceAddresses } returns listOf(
                        mockk<InterfaceAddress> {
                            every { address } returns mockk<InetAddress> {
                                every { address } returns byteArrayOf(10, 0, 0, 0)
                            }
                            every { broadcast } returns null
                        }
                    )
                }
            }

            PrivateNetworkAddress.findAll().shouldBeEmpty()
        }
    }
})
