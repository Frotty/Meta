package de.fatox.meta.injection

import com.badlogic.gdx.utils.GdxRuntimeException
import kotlin.reflect.KClass

class InjectionKey(val kClass: KClass<*>, val name: String?) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as InjectionKey

		if (kClass != other.kClass) return false
		if (name != other.name) return false

		return true
	}

	override fun hashCode(): Int = 31 * kClass.hashCode() + (name?.hashCode() ?: 0)

	override fun toString(): String = "InjectionKey(kClass=${kClass.qualifiedName}, name=$name)"
}

open class MetaInject {
	@PublishedApi
	internal val providers: MutableMap<InjectionKey, () -> Any> = mutableMapOf()

	@PublishedApi
	internal val singletons: MutableMap<InjectionKey, () -> Any> = mutableMapOf()

	@PublishedApi
	internal val singletonCache: MutableMap<InjectionKey, Any> = mutableMapOf()

	@PublishedApi
	internal fun canonicalName(name: String?): String? = if (name == "default") null else name

	inline fun <reified T : Any> inject(name: String? = null): T {
		val key = InjectionKey(T::class, canonicalName(name))
		return singletonCache[key] as T?
			?: singletons[key]?.invoke()?.also { singletonCache[key] = it } as T?
			?: providers[key]?.invoke() as T?
			?: throw GdxRuntimeException("Unknown class: ${T::class.qualifiedName}")
	}

	inline fun <reified T : Any> lazyInject(name: String? = null): Lazy<T> = lazy(lazyType) { inject(name) }

	inline fun <reified T : Any> provider(name: String? = null, noinline provider: () -> T) {
		val key = InjectionKey(T::class, canonicalName(name))
		if (name == "default") providers.putIfAbsent(key, provider)
		else providers[key] = provider
	}

	inline fun <reified T : Any> singleton(name: String? = null, noinline singleton: () -> T) {
		val key = InjectionKey(T::class, canonicalName(name))
		if (name == "default") {
			singletons.putIfAbsent(key, singleton)
			return
		}
		// Can't add a singleton that is already cached
		check(singletonCache[key] == null) { "Can not add singleton for ${T::class.qualifiedName} with name $name" }
		singletons[key] = singleton
	}

	inline fun <reified T : Any> singleton(singleton: T, name: String? = null) {
		if (name == "default") {
			singleton(name) { singleton }
			return
		}
		val key = InjectionKey(T::class, canonicalName(name))
		// Can't add a singleton that is already cached
		check(singletonCache[key] == null) { "Can not add singleton for ${T::class.qualifiedName} with name $name" }
		singletonCache[key] = singleton
	}

	companion object : MetaInject() {
		var lazyType: LazyThreadSafetyMode = LazyThreadSafetyMode.NONE

		@PublishedApi
		internal val scopes: MutableMap<String, MetaInject> = mutableMapOf()

		inline fun global(clear: Boolean = false, context: MetaInject.() -> Unit): MetaInject {
			if (clear) {
				providers.clear()
				singletons.clear()
				singletonCache.clear()
			}
			return this.apply(context)
		}

		inline fun scope(name: String, clear: Boolean = false, context: MetaInject.() -> Unit = {}): MetaInject {
			return scopes.getOrPut(name) { MetaInject() }.apply {
				if (clear) {
					providers.clear()
					singletons.clear()
					singletonCache.clear()
				}
				context()
			}
		}
	}
}
