package com.walkertribe.ian.iface

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.safeCast

data class ListenerFunction<A : ListenerArgument>(
    val argumentClass: KClass<A>,
    private val fn: (A) -> Unit,
) {
    fun <T : ListenerArgument> checkIfAccepting(argClass: KClass<T>): ListenerFunction<out T>? {
        return if (argumentClass.isSubclassOf(argClass)) {
            @Suppress("UNCHECKED_CAST")
            this as ListenerFunction<out T>
        } else {
            null
        }
    }

    fun offer(arg: ListenerArgument) {
        argumentClass.safeCast(arg)?.also(fn)
    }
}
