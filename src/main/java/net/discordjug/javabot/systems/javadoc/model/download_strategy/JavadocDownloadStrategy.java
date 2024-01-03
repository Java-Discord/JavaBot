package net.discordjug.javabot.systems.javadoc.model.download_strategy;

import net.discordjug.javabot.systems.javadoc.model.JavadocPackage;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface JavadocDownloadStrategy {
	static final Path JAVADOC_DIR = Path.of("javadocs/");

	@Nonnull
	URL buildDownloadUrl(@Nonnull String groupId, @Nonnull String artifactId, @Nonnull String version) throws MalformedURLException;

	@Nonnull
	default CompletableFuture<Optional<JavadocPackage>> download(@Nonnull URL url) {
		return CompletableFuture.supplyAsync(() -> {
			try (BufferedInputStream in = new BufferedInputStream(url.openStream());
				 FileOutputStream fileOutputStream = new FileOutputStream(JAVADOC_DIR + "test.jar.zip")) {
				byte[] dataBuffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
					fileOutputStream.write(dataBuffer, 0, bytesRead);
				}
				return Optional.empty();
			} catch (IOException ignored) {
				return Optional.empty();
			}
		});
	}
}
