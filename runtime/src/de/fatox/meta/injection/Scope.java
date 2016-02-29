package de.fatox.meta.injection;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;


@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface Scope {}