package me.whereareiam.identica.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an argument parser.
 * The method return type defines the parsed value type.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parser {
	/**
	 * Parser name. When empty, it is registered as the default parser for the return type.
	 */
	String name() default "";

	/**
	 * Named suggestion provider to use.
	 */
	String suggestions() default "";
}
