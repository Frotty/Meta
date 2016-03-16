package de.fatox.meta.ide.persist;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface Persist {
    String key() default "";
    String defaultValue() default "";
}
