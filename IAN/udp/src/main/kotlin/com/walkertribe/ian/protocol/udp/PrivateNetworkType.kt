package com.walkertribe.ian.protocol.udp

/**
 * IPv4 private network types. Used to determine whether an address belongs to a private network.
 * @author rjwut
 */
enum class PrivateNetworkType {
    TWENTY_FOUR_BIT_BLOCK {
        // 10.x.x.x
        override val constraints: Array<ByteConstraint> get() = TWENTY_FOUR_BIT_CONSTRAINTS
    },
    TWENTY_BIT_BLOCK {
        // 172.16.x.x - 172.31.x.x
        override val constraints: Array<ByteConstraint> get() = TWENTY_BIT_CONSTRAINTS
    },
    SIXTEEN_BIT_BLOCK {
        // 192.168.x.x
        override val constraints: Array<ByteConstraint> get() = SIXTEEN_BIT_CONSTRAINTS
    };

    /**
     * Returns true if the given address matches this private network type.
     */
    internal abstract val constraints: Array<ByteConstraint>

    internal fun match(address: ByteArray): Boolean = address.run {
        size == Int.SIZE_BYTES && zip(constraints).all { (byte, cons) -> cons.check(byte) }
    }

    companion object {
        private val TWENTY_FOUR_BIT_CONSTRAINTS = arrayOf<ByteConstraint>(
            ByteConstraint.Equals(10),
        )

        private val TWENTY_BIT_CONSTRAINTS = arrayOf(
            ByteConstraint.Equals(-44),
            ByteConstraint.Range(16 ..31),
        )

        private val SIXTEEN_BIT_CONSTRAINTS = arrayOf<ByteConstraint>(
            ByteConstraint.Equals(-64),
            ByteConstraint.Equals(-88),
        )

        /**
         * Returns the private network address type that matches the given address, or null if it's
         * not a private network address.
         */
        operator fun invoke(address: ByteArray): PrivateNetworkType? =
            entries.find { it.match(address) }
    }
}
