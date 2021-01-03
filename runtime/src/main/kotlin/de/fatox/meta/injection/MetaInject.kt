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

	override fun hashCode(): Int {
		return 31 * kClass.hashCode() + (name?.hashCode() ?: 0)
	}

	override fun toString(): String {
		return "InjectionKey(kClass=${kClass.qualifiedName}, name=$name)"
	}
}

open class MetaInject {
	@PublishedApi
	internal val providers: MutableMap<InjectionKey, () -> Any> = mutableMapOf()

	@PublishedApi
	internal val singletons: MutableMap<InjectionKey, () -> Any> = mutableMapOf()

	@PublishedApi
	internal val singletonCache: MutableMap<InjectionKey, Any> = mutableMapOf()

	inline fun <reified T : Any> inject(name: String? = null): T {
		val key = InjectionKey(T::class, name)
		return singletonCache[key] as T?
			?: singletons[key]?.invoke()?.also { singletonCache[key] = it } as T?
			?: providers[key]?.invoke() as T?
			?: throw GdxRuntimeException("Unknown class: ${T::class.qualifiedName}")
	}

	inline fun <reified T : Any> lazyInject(name: String? = null): Lazy<T> = lazy(lazyType) { inject(name) }

	inline fun <reified T : Any> provider(name: String? = null, noinline provider: () -> T) {
		if (name == "default") providers[InjectionKey(T::class, null)] = provider
		providers[InjectionKey(T::class, name)] = provider
	}

	inline fun <reified T : Any> singleton(name: String? = null, noinline singleton: () -> T) {
		// Can't add a singleton that is already cached
		check(singletonCache[InjectionKey(T::class, name)] == null)
		{ "Can not add singleton for ${T::class.qualifiedName} with name $name" }

		if (name == "default" && singletons[InjectionKey(T::class, null)] == null)
			singletons[InjectionKey(T::class, null)] = singleton
		singletons[InjectionKey(T::class, name)] = singleton
	}

	inline fun <reified T : Any> singleton(singleton: T, name: String? = null) {
		// Can't add a singleton that is already cached
		check(singletonCache[InjectionKey(T::class, name)] == null)
		{ "Can not add singleton for ${T::class.qualifiedName} with name $name" }

		if (name == "default") // Don't save default values in the cache directly
			singleton(name) { singleton }
		else
			singletonCache[InjectionKey(T::class, name)] = singleton
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

		inline fun scope(clear: Boolean = false, name: String, context: MetaInject.() -> Unit = {}): MetaInject {
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
