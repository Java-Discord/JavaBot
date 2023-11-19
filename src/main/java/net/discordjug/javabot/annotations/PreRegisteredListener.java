package net.discordjug.javabot.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks JDA listeners to be registered before JDA is initialized.
 *
 * Only {@link net.dv8tion.jda.api.hooks.EventListener EventListener}s should be annoted with this annotation.
 * All listeners overriding {@link net.dv8tion.jda.api.hooks.EventListener#onReady(net.dv8tion.jda.api.events.session.ReadyEvent) onReady} should be annotated with this annotation.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface PreRegisteredListener {

}
