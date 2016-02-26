package de.fatox.meta.ide.persist;

public @interface Persist {
    String key() default "";
    String defaultValue() default "";
}
