package me.whereareiam.identica.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
	String[] value();

	Mode mode() default Mode.ANY_OF;

	enum Mode {
		ANY_OF,
		ALL_OF
	}
}
