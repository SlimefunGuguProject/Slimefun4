package io.github.thebusybiscuit.slimefun4.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or member as an implementation detail that is not a supported addon API.
 *
 * <p>Internal elements may change between maintenance releases without compatibility guarantees.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
    ElementType.TYPE,
    ElementType.METHOD,
    ElementType.CONSTRUCTOR,
    ElementType.FIELD,
    ElementType.PACKAGE
})
public @interface SlimefunInternal {}
