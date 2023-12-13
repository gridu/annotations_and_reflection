package com.example.demo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MyCustomAnnotation {
    String value() default "default value"; // String element with a default value
    int count() default 0;                  // Integer element with a default value
    boolean enabled() default true;         // Boolean element with a default value

    // You can add more elements as needed
    // ReturnType elementName() default DefaultValue;
}
