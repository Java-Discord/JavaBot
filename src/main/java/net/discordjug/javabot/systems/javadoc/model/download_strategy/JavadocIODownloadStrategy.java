package net.discordjug.javabot.systems.javadoc.model.download_strategy;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URL;

public class JavadocIODownloadStrategy implements JavadocDownloadStrategy {
	@Override
	public @NotNull URL buildDownloadUrl(@Nonnull String groupId, @Nonnull String artifactId, @Nonnull String version) throws MalformedURLException {
		return new URL("https://javadoc.io/jar/%s/%s/%s/%s-%s-javadoc.jar".formatted(groupId, artifactId, version, artifactId, version));
	}
}
