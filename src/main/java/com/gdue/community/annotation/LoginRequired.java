package com.gdue.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) //用来标注该注解是用在哪里的（方法上）
@Retention(RetentionPolicy.RUNTIME) //用来标注该注解编译时保不保留
public @interface LoginRequired {
}
