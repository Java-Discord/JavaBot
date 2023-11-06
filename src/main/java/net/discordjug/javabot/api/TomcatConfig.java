package net.discordjug.javabot.api;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AjpNioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.discordjug.javabot.data.config.SystemsConfig;


/**
 * Holds all configuration for the {@link org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat}
 * web service.
 */
@Configuration
public class TomcatConfig {

	private final int ajpPort;
	private final boolean tomcatAjpEnabled;
	private final SystemsConfig systemsConfig;

	/**
	 * Initializes this object.
	 * @param ajpPort The port to run AJP under
	 * @param tomcatAjpEnabled <code>true</code> if AJP is enabled, else <code>false</code>
	 * @param systemsConfig an object representing the configuration of various systems
	 */
	public TomcatConfig(@Value("${tomcat.ajp.port}") int ajpPort, @Value("${tomcat.ajp.enabled}") boolean tomcatAjpEnabled, SystemsConfig systemsConfig) {
		this.ajpPort = ajpPort;
		this.tomcatAjpEnabled = tomcatAjpEnabled;
		this.systemsConfig = systemsConfig;
	}

	/**
	 * Sets up the {@link TomcatServletWebServerFactory} using the {@link Value}s defined in the
	 * application.properties file.
	 *
	 * @return The {@link TomcatServletWebServerFactory}.
	 */
	@Bean
	TomcatServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
		if (tomcatAjpEnabled) {
			Connector ajpConnector = new Connector("org.apache.coyote.ajp.AjpNioProtocol");
			AjpNioProtocol protocol= (AjpNioProtocol) ajpConnector.getProtocolHandler();
			protocol.setSecret(systemsConfig.getApiConfig().getAjpSecret());
			ajpConnector.setPort(ajpPort);
			ajpConnector.setSecure(true);
			tomcat.addAdditionalTomcatConnectors(ajpConnector);
		}
		return tomcat;
	}
}
