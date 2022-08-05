package net.javadiscord.javabot.api;

import net.javadiscord.javabot.Bot;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AjpNioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Holds all configuration for the {@link org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat}
 * web service.
 */
@Configuration
public class TomcatConfig {

	private final int ajpPort;

	private final boolean tomcatAjpEnabled;

	public TomcatConfig(@Value("${tomcat.ajp.port}") int ajpPort, @Value("${tomcat.ajp.enabled}") boolean tomcatAjpEnabled) {
		this.ajpPort = ajpPort;
		this.tomcatAjpEnabled = tomcatAjpEnabled;
	}

	/**
	 * Sets up the {@link TomcatServletWebServerFactory} using the {@link Value}s defined in the
	 * application.properties file.
	 *
	 * @return The {@link TomcatServletWebServerFactory}.
	 */
	@Bean
	public TomcatServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
		if (tomcatAjpEnabled) {
			Connector ajpConnector = new Connector("org.apache.coyote.ajp.AjpNioProtocol");
			AjpNioProtocol protocol= (AjpNioProtocol) ajpConnector.getProtocolHandler();
			protocol.setSecret(Bot.getConfig().getSystems().getApiConfig().getAjpSecret());
			ajpConnector.setPort(ajpPort);
			ajpConnector.setSecure(true);
			tomcat.addAdditionalTomcatConnectors(ajpConnector);
		}
		return tomcat;
	}
}
