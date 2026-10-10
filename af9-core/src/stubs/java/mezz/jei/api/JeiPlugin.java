package mezz.jei.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Compile-time stand-in for JEI's annotation (not shipped): JEI finds the plugin by it. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JeiPlugin {}
