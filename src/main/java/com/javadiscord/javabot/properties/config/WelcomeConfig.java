package com.javadiscord.javabot.properties.config;

import lombok.Data;

@Data
public class WelcomeConfig {
	private String joinMessageTemplate;
	private String leaveMessageTemplate;
	private long channelId;
	private boolean enabled;

	private ImageConfig imageConfig;

	@Data
	public static class ImageConfig {
		private int width;
		private int height;
		private String overlayImageUrl;
		private String backgroundImageUrl;
		private int primaryColor;
		private int secondaryColor;

		private AvatarConfig avatarConfig;

		@Data
		public static class AvatarConfig {
			private int x;
			private int y;
			private int width;
			private int height;
		}
	}
}
