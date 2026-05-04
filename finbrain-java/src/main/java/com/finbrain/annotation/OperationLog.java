package com.finbrain.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    String module() default "";

    String operation() default "";

    String description() default "";

    boolean saveRequest() default true;

    boolean saveResponse() default false;

    String[] sensitiveFields() default {};
}
