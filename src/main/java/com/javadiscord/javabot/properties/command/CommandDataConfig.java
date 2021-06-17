package com.javadiscord.javabot.properties.command;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class CommandDataConfig {
	public static CommandConfig[] load() {
		Yaml yaml = new Yaml();
		InputStream is = CommandDataConfig.class.getClassLoader().getResourceAsStream("commands.yaml");
		return yaml.loadAs(is, CommandConfig[].class);
	}
}
