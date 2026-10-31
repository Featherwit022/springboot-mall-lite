package com.example.mall.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)   // 忘了 RUNTIME，切点永远匹配不到
public @interface CostTime {
    String value() default "";
}