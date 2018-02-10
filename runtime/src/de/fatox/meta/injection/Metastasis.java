package de.fatox.meta.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class Metastasis {
    private final Map<Key, Provider<?>> providers = new HashMap<>();
    private final Map<Key, Object> singletons = new HashMap<>();
    private final Map<Class, Object[][]> injectFields = new HashMap<>(0);

    public Metastasis() {
        // Add Metastasis to the injectable classes
        providers.put(Key.of(Metastasis.class), () -> null);
    }

    public void loadModule(Object module) {
        // Only allow module instances, not classes
        if (module instanceof Class) {
            throw new MetastasisException(String.format("%s provided as class instead of an instance.", ((Class) module).getName()));
        }
        // Find and Register all providerMethods
        Set<Method> providerMethods = getProviderMethods(module.getClass());
        for (Method providerMethod : providerMethods) {
            registerProviderMethod(module, providerMethod);
        }
    }

    /**
     * @return an instance of type
     */
    public <T> T instance(Class<T> type) {
        return provider(Key.of(type), null).get();
    }

    /**
     * @return instance specified by key (type and qualifier)
     */
    public <T> T instance(Key<T> key) {
        return provider(key, null).get();
    }

    /**
     * @return provider of type
     */
    public <T> Provider<T> provider(Class<T> type) {
        return provider(Key.of(type), null);
    }

    /**
     * @return provider of key (type, qualifier)
     */
    public <T> Provider<T> provider(Key<T> key) {
        return provider(key, null);
    }

    /**
     * Injects getInjectedFields to the target object
     */
    public void injectFields(Object target) {
        if (!injectFields.containsKey(target.getClass())) {
            Object[][] fieldObjects = injectFields(target.getClass());
            injectFields.put(target.getClass(), fieldObjects);
        }
        for (Object[] f : injectFields.get(target.getClass())) {
            Field field = (Field) f[0];
            Key key = (Key) f[2];
            Key key2 = Key.of(field.getType(), field.getName());
            Key key3 = Key.of(field.getType(), "default");
            try {
                if (providers.containsKey(key)) {
                    field.set(target, providers.get(key).get());
                } else if (providers.containsKey(key2)) {
                    field.set(target, providers.get(key2).get());
                } else if (providers.containsKey(key3)) {
                    field.set(target, providers.get(key3).get());
                } else {
                    throw new MetastasisException(String.format("Can't inject field %s in %s because there is no provider defined for the type", field
                            .getName(), target.getClass().getName()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new MetastasisException(String.format("Can't inject field %s in %s", field.getName(), target.getClass().getName()));
            }
        }
    }

    private void registerProviderMethod(final Object module, final Method m) {
        // Build key
        final Key key = Key.of(m.getReturnType(), qualifier(m.getAnnotations()));
        // Forbid double providers without different qualifiers
        if (providers.containsKey(key)) {
            throw new MetastasisException(String.format("%s has multiple providers, module %s", key.toString(), module.getClass()));
        }

        boolean isSingleton = m.getAnnotation(Singleton.class) != null;
        final Provider<?>[] paramProviders = paramProviders(key, m.getParameterTypes(), m.getGenericParameterTypes(), m.getParameterAnnotations(),
                Collections.singleton(key)
        );
        if (isSingleton) {
            singletonProvider(key, () -> {
                try {
                    return m.invoke(module, params(paramProviders));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                return null;
            });
        } else {
            provider(key, null);
        }
    }

    private static Object[] params(Provider<?>[] paramProviders) {
        Object[] params = new Object[paramProviders.length];
        for (int i = 0; i < paramProviders.length; ++i) {
            params[i] = paramProviders[i].get();
        }
        return params;
    }

    private static Set<Key> append(Set<Key> set, Key newKey) {
        if (set != null && !set.isEmpty()) {
            Set<Key> appended = new LinkedHashSet<>(set);
            appended.add(newKey);
            return appended;
        } else {
            return Collections.singleton(newKey);
        }
    }

    private static Object[][] injectFields(Class<?> target) {
        Set<Field> injectedFields = getInjectedFields(target);
        Object[][] fs = new Object[injectedFields.size()][];
        int i = 0;
        for (Field field : injectedFields) {
            Class<?> providerType = field.getType().equals(Provider.class) ? (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()
                    [0] : null;
            Class<?> aClass = (providerType != null) ? providerType : field.getType();
            fs[i++] = new Object[]{field, providerType != null, Key.of(aClass, qualifier(field.getAnnotations()))};
        }
        return fs;
    }

    /**
     * Retrieves all fields with @Inject annotation for the given class
     */
    private static Set<Field> getInjectedFields(Class<?> type) {
        Class<?> current = type;
        Set<Field> fields = new HashSet<>();
        while (!current.equals(Object.class)) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static String chain(Set<Key> chain, Key lastKey) {
        StringBuilder chainString = new StringBuilder();
        for (Key key : chain) {
            chainString.append(key.toString()).append(" -> ");
        }
        return chainString.append(lastKey.toString()).toString();
    }

    private static Constructor constructor(Key key) {
        Constructor inject = null;
        Constructor noarg = null;
        for (Constructor c : key.type.getDeclaredConstructors()) {
            if (c.isAnnotationPresent(Inject.class)) {
                if (inject == null) {
                    inject = c;
                } else {
                    throw new MetastasisException(String.format("%s has multiple @Inject constructors", key.type));
                }
            } else if (c.getParameterTypes().length == 0) {
                noarg = c;
            }
        }
        Constructor constructor = inject != null ? inject : noarg;
        if (constructor != null) {
            constructor.setAccessible(true);
            return constructor;
        } else {
            throw new MetastasisException(String.format("%s doesn't have an @Inject or no-arg constructor, or a module provider", key.type
                    .getName()));
        }
    }

    private static Set<Method> getProviderMethods(Class<?> type) {
        Class<?> current = type;
        Set<Method> providers = new HashSet<>();
        while (!current.equals(Object.class)) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Provides.class) && (type.equals(current) || !providerInSubClass(method, providers))) {
                    method.setAccessible(true);
                    providers.add(method);
                }
            }
            current = current.getSuperclass();
        }
        return providers;
    }

    private static Annotation qualifier(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                return annotation;
            }
        }
        return null;
    }

    private static boolean providerInSubClass(Method method, Set<Method> discoveredMethods) {
        for (Method discovered : discoveredMethods) {
            if (discovered.getName().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), discovered.getParameterTypes()
            )) {
                return true;
            }
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    private <T> Provider<T> provider(final Key<T> key, Set<Key> chain) {
        if (!providers.containsKey(key)) {
            if (key.name == null || key.name.length() <= 0) {
                key.qualifier = Named.class;
                key.name = "default";
                if (providers.containsKey(key)) {
                    return (Provider<T>) providers.get(key);
                }
                key.name = "";
            }
            final Constructor constructor = constructor(key);
            final Provider<?>[] paramProviders = paramProviders(key, constructor.getParameterTypes(), constructor
                    .getGenericParameterTypes(), constructor.getParameterAnnotations(), chain);
            providers.put(key, () -> {
                        try {
                            return constructor.newInstance(params(paramProviders));
                        } catch (Exception e) {
                            throw new MetastasisException(String.format("Can't instantiate %s", key.toString()), e);
                        }
                    }
            );
        }
        return (Provider<T>) providers.get(key);
    }


    @SuppressWarnings("unchecked")
    private <T> Provider<T> singletonProvider(final Key key, final Provider provider) {
        if (!providers.containsKey(key)) {
            Provider singletonProvider = () -> {
                if (!singletons.containsKey(key)) {
                    synchronized (singletons) {
                        if (!singletons.containsKey(key)) {
                            singletons.put(key, provider.get());
                        }
                    }
                }
                return (T) singletons.get(key);
            };
            providers.put(key, singletonProvider);
        }
        return (Provider<T>) providers.get(key);
    }

    private Provider<?>[] paramProviders(
            final Key key,
            Class<?>[] parameterClasses,
            Type[] parameterTypes,
            Annotation[][] annotations,
            final Set<Key> chain
    ) {
        Provider<?>[] providers = new Provider<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i) {
            Class<?> parameterClass = parameterClasses[i];
            Annotation qualifier = qualifier(annotations[i]);
            Class<?> providerType = Provider.class.equals(parameterClass) ?
                    (Class<?>) ((ParameterizedType) parameterTypes[i]).getActualTypeArguments()[0] :
                    null;
            if (providerType == null) {
                final Key newKey = Key.of(parameterClass, qualifier);
                final Set<Key> newChain = append(chain, key);
                if (newChain.contains(newKey)) {
                    throw new MetastasisException(String.format("Circular dependency: %s", chain(newChain, newKey)));
                }
                providers[i] = () -> provider(newKey, newChain).get();
            } else {
                final Key newKey = Key.of(providerType, qualifier);
                providers[i] = () -> provider(newKey, null);
            }
        }
        return providers;
    }
}
