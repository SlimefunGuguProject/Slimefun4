package io.github.thebusybiscuit.slimefun4.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or member as part of Slimefun's supported addon-facing API.
 *
 * <p>Elements carrying this annotation should remain source and binary compatible whenever practical.
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
public @interface SlimefunAPI {}
