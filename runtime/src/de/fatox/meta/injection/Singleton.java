package de.fatox.meta.injection;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Identifies a type that the injector only instantiates once. Not inherited.
 */
@Scope
@Documented
@Retention(RUNTIME)
public @interface Singleton {
}