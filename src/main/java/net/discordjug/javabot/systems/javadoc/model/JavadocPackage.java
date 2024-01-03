package net.discordjug.javabot.systems.javadoc.model;

public record JavadocPackage(net.discordjug.javabot.systems.javadoc.model.JavadocPackage.Metadata metadata) {
	public record Metadata(String url, String version, String groupId, String artifactId) {}
}
