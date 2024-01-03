package net.discordjug.javabot.systems.javadoc.model;

import lombok.Getter;
import net.discordjug.javabot.systems.javadoc.model.download_strategy.JavadocDownloadStrategy;
import net.discordjug.javabot.systems.javadoc.model.download_strategy.JavadocIODownloadStrategy;

@Getter
public enum JavadocPackageHost {
	JAVADOC_IO("javadoc.io", new JavadocIODownloadStrategy());

	private final String host;
	private final JavadocDownloadStrategy downloadStrategy;

	JavadocPackageHost(String baseUrl, JavadocDownloadStrategy downloadStrategy) {
		this.host = baseUrl;
		this.downloadStrategy = downloadStrategy;
	}
}
