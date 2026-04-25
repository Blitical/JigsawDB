package dev.blitical.jigsawDB.exceptions.exceptionHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JigsawDBException {
    Severity severity() default Severity.MEDIUM;

    String documentationURL() default "";

    String[] fixes() default {};

    String correct() default "";

    String incorrect() default "";

    enum Severity {
        LOW, MEDIUM, HIGH, SEVERE;
    }
}
