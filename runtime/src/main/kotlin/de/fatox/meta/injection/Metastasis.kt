package de.fatox.meta.injection

import de.fatox.meta.injection.Metastasis
import java.lang.reflect.*
import java.util.*

class Metastasis {
    private val providers: MutableMap<Key<*>, Provider<*>> = HashMap()
    private val singletons: MutableMap<Key<*>, Any?> = HashMap()
    private val injectFields: MutableMap<Class<*>, Array<Array<Any>>> = HashMap(0)
    fun loadModule(module: Any) {
        // Only allow module instances, not classes
        if (module is Class<*>) {
            throw MetastasisException(String.format("%s provided as class instead of an instance.", module.name))
        }
        // Find and Register all providerMethods
        val providerMethods = getProviderMethods(module.javaClass)
        for (providerMethod in providerMethods) {
            registerProviderMethod(module, providerMethod)
        }
    }

    /**
     * @return an instance of type
     */
    fun <T> instance(type: Class<T>): T {
        return provider(Key.Companion.of(type), null).get()
    }

    /**
     * @return instance specified by key (type and qualifier)
     */
    fun <T> instance(key: Key<T>): T? {
        return provider(key, null).get()
    }

    /**
     * @return provider of type
     */
    fun <T> provider(type: Class<T>): Provider<T> {
        return provider(Key.Companion.of(type), null)
    }

    /**
     * @return provider of key (type, qualifier)
     */
    fun <T> provider(key: Key<T>): Provider<T> {
        return provider(key, null)
    }

    /**
     * Injects getInjectedFields to the target object
     */
    fun injectFields(target: Any) {
        if (!injectFields.containsKey(target.javaClass)) {
            val fieldObjects = Companion.injectFields(target.javaClass)
            injectFields[target.javaClass] = fieldObjects
        }
        for (f in injectFields[target.javaClass]!!) {
            val field = f[0] as Field
            val key = f[2] as Key<*>
            val annotation = field.getAnnotation(Named::class.java)
            var key2: Key<*> = Key.Companion.of(field.type, field.name)
            if (annotation != null) {
                key2 = Key.Companion.of(field.type, annotation)
            }
            val key3: Key<*> = Key.Companion.of(field.type, "default")
            try {
                if (providers.containsKey(key)) {
                    field[target] = providers[key]!!.get()
                } else if (providers.containsKey(key2)) {
                    field[target] = providers[key2]!!.get()
                } else if (providers.containsKey(key3)) {
                    field[target] = providers[key3]!!.get()
                } else {
                    throw MetastasisException(
                        String.format(
                            "Can't inject field %s in %s because there is no provider defined for the type <%s>", field
                                .name, target.javaClass.name, field.type.name
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw MetastasisException(
                    String.format(
                        "Can't inject field %s in %s",
                        field.name,
                        target.javaClass.name
                    )
                )
            }
        }
    }

    private fun registerProviderMethod(module: Any, m: Method) {
        // Build key
        val key: Key<*> = Key.Companion.of(m.returnType, qualifier(m.annotations))
        // Forbid double providers without different qualifiers
        if (providers.containsKey(key)) {
            throw MetastasisException(
                String.format(
                    "%s has multiple providers, module %s",
                    key.toString(),
                    module.javaClass
                )
            )
        }
        val isSingleton = m.getAnnotation(Singleton::class.java) != null
        val paramProviders =
            paramProviders(key, m.parameterTypes, m.genericParameterTypes, m.parameterAnnotations, setOf(key))
        if (isSingleton) {
            singletonProvider<Any>(key) {
                try {
                    return@singletonProvider m.invoke(module, *params(paramProviders))
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
                null
            }
        } else {
            provider<Any>(key, null)
        }
    }

    private fun <T> provider(key: Key<T>, chain: Set<Key<*>>?): Provider<T> {
        if (!providers.containsKey(key)) {
            if (key.name == null || key.name!!.length <= 0) {
                key.qualifier = Named::class.java
                key.name = "default"
                if (providers.containsKey(key)) {
                    return providers[key] as Provider<T>
                }
                key.name = ""
            }
            val constructor = constructor(key)
            val paramProviders = paramProviders(
                key, constructor.parameterTypes, constructor
                    .genericParameterTypes, constructor.parameterAnnotations, chain
            )
            providers[key] = Provider<Any> {
                try {
                    return@put constructor.newInstance(*params(paramProviders))
                } catch (e: Exception) {
                    throw MetastasisException(String.format("Can't instantiate %s", key.toString()), e)
                }
            }
        }
        return providers[key] as Provider<T>
    }

    private fun <T> singletonProvider(key: Key<*>, provider: Provider<*>): Provider<T> {
        if (!providers.containsKey(key)) {
            val singletonProvider: Provider<*> = Provider {
                if (!singletons.containsKey(key)) {
                    synchronized(singletons) {
                        if (!singletons.containsKey(key)) {
                            singletons[key] = provider.get()
                        }
                    }
                }
                singletons[key] as T?
            }
            providers[key] = singletonProvider
        }
        return providers[key] as Provider<T>
    }

    private fun paramProviders(
        key: Key<*>,
        parameterClasses: Array<Class<*>>,
        parameterTypes: Array<Type>,
        annotations: Array<Array<Annotation>>,
        chain: Set<Key<*>>?
    ): Array<Provider<*>> {
        val providers: Array<Provider<*>> = arrayOfNulls(parameterTypes.size)
        for (i in parameterTypes.indices) {
            val parameterClass = parameterClasses[i]
            val qualifier = qualifier(annotations[i])
            val providerType =
                if (Provider::class.java == parameterClass) (parameterTypes[i] as ParameterizedType).actualTypeArguments[0] as Class<*> else null
            if (providerType == null) {
                val newKey: Key<*> = Key.Companion.of(parameterClass, qualifier)
                val newChain = append(chain, key)
                if (newChain.contains(newKey)) {
                    throw MetastasisException(String.format("Circular dependency: %s", chain(newChain, newKey)))
                }
                providers[i] = Provider { provider<Any>(newKey, newChain).get() }
            } else {
                val newKey: Key<*> = Key.Companion.of(providerType, qualifier)
                providers[i] = Provider<Any> { provider<Any?>(newKey, null) }
            }
        }
        return providers
    }

    companion object {
        private fun params(paramProviders: Array<Provider<*>>): Array<Any?> {
            val params = arrayOfNulls<Any>(paramProviders.size)
            for (i in paramProviders.indices) {
                params[i] = paramProviders[i].get()
            }
            return params
        }

        private fun append(set: Set<Key<*>>?, newKey: Key<*>): Set<Key<*>> {
            return if (set != null && !set.isEmpty()) {
                val appended: MutableSet<Key<*>> =
                    LinkedHashSet(set)
                appended.add(newKey)
                appended
            } else {
                setOf(newKey)
            }
        }

        private fun injectFields(target: Class<*>): Array<Array<Any>> {
            val injectedFields = getInjectedFields(target)
            val fs: Array<Array<Any>> = arrayOfNulls(injectedFields.size)
            var i = 0
            for (field in injectedFields) {
                val providerType =
                    if (field.type == Provider::class.java) (field.genericType as ParameterizedType).actualTypeArguments[0] as Class<*> else null
                val aClass = providerType ?: field.type
                fs[i++] = arrayOf(field, providerType != null, Key.Companion.of(aClass, qualifier(field.annotations)))
            }
            return fs
        }

        /**
         * Retrieves all fields with @Inject annotation for the given class
         */
        private fun getInjectedFields(type: Class<*>): Set<Field> {
            var current = type
            val fields: MutableSet<Field> = HashSet()
            while (current != Any::class.java) {
                for (field in current.declaredFields) {
                    if (field.isAnnotationPresent(Inject::class.java)) {
                        field.isAccessible = true
                        fields.add(field)
                    }
                }
                current = current.superclass
            }
            return fields
        }

        private fun chain(chain: Set<Key<*>>, lastKey: Key<*>): String {
            val chainString = StringBuilder()
            for (key in chain) {
                chainString.append(key.toString()).append(" -> ")
            }
            return chainString.append(lastKey.toString()).toString()
        }

        private fun constructor(key: Key<*>): Constructor<*> {
            var inject: Constructor<*>? = null
            var noarg: Constructor<*>? = null
            for (c in key.type.declaredConstructors) {
                if (c.isAnnotationPresent(Inject::class.java)) {
                    inject = if (inject == null) {
                        c
                    } else {
                        throw MetastasisException(String.format("%s has multiple @Inject constructors", key.type))
                    }
                } else if (c.parameterTypes.size == 0) {
                    noarg = c
                }
            }
            val constructor = inject ?: noarg
            return if (constructor != null) {
                constructor.isAccessible = true
                constructor
            } else {
                throw MetastasisException(
                    String.format(
                        "%s doesn't have an @Inject or no-arg constructor, or a module provider", key.type
                            .name
                    )
                )
            }
        }

        private fun getProviderMethods(type: Class<*>): Set<Method> {
            var current = type
            val providers: MutableSet<Method> = HashSet()
            while (current != Any::class.java) {
                for (method in current.declaredMethods) {
                    if (method.isAnnotationPresent(Provides::class.java) && (type == current || !providerInSubClass(
                            method,
                            providers
                        ))
                    ) {
                        method.isAccessible = true
                        providers.add(method)
                    }
                }
                current = current.superclass
            }
            return providers
        }

        private fun qualifier(annotations: Array<Annotation>): Annotation? {
            for (annotation in annotations) {
                if (annotation.annotationType().isAnnotationPresent(Qualifier::class.java)) {
                    return annotation
                }
            }
            return null
        }

        private fun providerInSubClass(method: Method, discoveredMethods: Set<Method>): Boolean {
            for (discovered in discoveredMethods) {
                if (discovered.name == method.name && Arrays.equals(
                        method.parameterTypes, discovered.parameterTypes
                    )
                ) {
                    return true
                }
            }
            return false
        }
    }

    init {
        // Add Metastasis to the injectable classes
        providers[Key.Companion.of<Metastasis>(Metastasis::class.java)] =
            Provider<Any> { null }
    }
}