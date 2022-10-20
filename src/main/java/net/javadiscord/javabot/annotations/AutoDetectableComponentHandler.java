package net.javadiscord.javabot.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * Annotation for component (non-slash command-interactions) handlers which should be registered automatically.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface AutoDetectableComponentHandler {
	/**
	 * The names the handler should be called under.
	 * Each name should be unique.
	 * @return The names of the handler
	 */
	String[] value();
}
