package org.springframework.cloud.openfeign;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface SpringQueryMap {
    @AliasFor("encoded")
    boolean value() default false;

    @AliasFor("value")
    boolean encoded() default false;
}
