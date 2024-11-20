package org.gbm.filter.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** This annotation is used to bound methods to filtering expressions */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterMethod {}
