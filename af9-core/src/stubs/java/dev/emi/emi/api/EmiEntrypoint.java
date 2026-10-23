package dev.emi.emi.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Compile-time stand-in for EMI's plugin marker (not shipped). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EmiEntrypoint {}
