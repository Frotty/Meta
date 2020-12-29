package de.fatox.meta.injection

import de.fatox.meta.ui.windows.MetaWindow.contentTable
import de.fatox.meta.ui.windows.MetaWindow.close
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.graphics.FontProvider.getFont
import de.fatox.meta.api.ui.UIRenderer.addActor
import de.fatox.meta.api.PosModifier.modify
import de.fatox.meta.api.ui.UIRenderer.resize
import de.fatox.meta.api.model.MetaWindowData.displayed
import de.fatox.meta.api.model.MetaWindowData.set
import de.fatox.meta.api.model.MetaWindowData.setFrom
import de.fatox.meta.api.lang.LanguageBundle.format
import de.fatox.meta.api.model.MetaAudioVideoData.masterVolume
import de.fatox.meta.api.model.MetaAudioVideoData.musicVolume
import de.fatox.meta.api.AssetProvider.get
import de.fatox.meta.api.model.MetaAudioVideoData.soundVolume
import de.fatox.meta.assets.MetaAssetProvider.get
import de.fatox.meta.api.ui.UIRenderer.getCamera
import de.fatox.meta.ui.windows.MetaWindow
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisLabel
import de.fatox.meta.ui.windows.MetaDialog.DialogListener
import de.fatox.meta.ui.components.MetaClickListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.widget.VisImageButton
import com.badlogic.gdx.utils.Align
import de.fatox.meta.ui.components.MetaTextButton
import kotlin.jvm.JvmOverloads
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import de.fatox.meta.injection.Inject
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.ui.components.MetaLabel
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.Meta
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle
import de.fatox.meta.util.GoldenRatio
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.util.InputValidator
import de.fatox.meta.error.MetaErrorHandler
import com.kotcrab.vis.ui.widget.VisValidatableTextField
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.injection.Singleton
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.input.MetaInput
import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.DummyPosModifier
import de.fatox.meta.api.model.MetaWindowData
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.reflect.ClassReflection
import de.fatox.meta.ui.MetaUiManager
import de.fatox.meta.ui.windows.MetaDialog
import java.lang.InstantiationException
import java.lang.IllegalAccessException
import com.badlogic.gdx.utils.TimeUtils
import kotlin.jvm.Synchronized
import de.fatox.meta.task.TaskListener
import de.fatox.meta.task.MetaTask
import com.badlogic.gdx.Game
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.error.MetaError
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.IntMap
import de.fatox.meta.camera.ArcCamControl
import com.badlogic.gdx.controllers.Controllers
import de.fatox.meta.input.MetaControllerListener
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.PovDirection
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.api.AssetProvider
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.sound.MetaSoundDefinition
import com.badlogic.gdx.math.MathUtils
import de.fatox.meta.sound.MetaSoundHandle
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.sound.MetaSoundPlayer
import java.lang.NoSuchMethodException
import de.fatox.meta.assets.MetaData.CacheObj
import com.badlogic.gdx.utils.Json
import java.nio.channels.ReadableByteChannel
import de.fatox.meta.assets.HashUtils
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.lang.RuntimeException
import java.io.IOException
import java.math.BigInteger
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Throws
import de.fatox.meta.assets.XPKByteChannel
import java.util.Arrays
import java.nio.channels.ClosedChannelException
import de.fatox.meta.entity.Meta3DEntity
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.Input.Buttons
import de.fatox.meta.api.entity.EntityManager
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import de.fatox.meta.entity.LightEntity
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import de.fatox.meta.graphics.buffer.MultisampleFBO
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import java.nio.IntBuffer
import java.lang.Exception
import com.badlogic.gdx.Application.ApplicationType
import java.nio.ByteOrder
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import java.lang.IllegalStateException
import com.badlogic.gdx.graphics.Pixmap
import java.util.HashMap
import com.badlogic.gdx.graphics.VertexAttribute
import de.fatox.meta.injection.Qualifier
import de.fatox.meta.injection.MetastasisException
import de.fatox.meta.injection.Metastasis
import java.util.LinkedHashSet
import java.util.HashSet
import de.fatox.meta.injection.Provides
import java.lang.StringBuilder
import java.lang.reflect.*

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