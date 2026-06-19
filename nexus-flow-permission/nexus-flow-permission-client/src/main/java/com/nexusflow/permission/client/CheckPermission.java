package com.nexusflow.permission.client;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckPermission {
    String value();
    String scopeType() default "MERCHANT";
}
