package de.fatox.meta.injection

interface Provider<T> {
    fun get(): T
}