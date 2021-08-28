package com.javadiscord.javabot.properties.config;

import lombok.Data;

import javax.annotation.Nullable;

@Data
public class WelcomeConfig {
	@Nullable
	private String joinMessageTemplate;
	@Nullable
	private String leaveMessageTemplate;
	private long channelId;
	private boolean enabled;

	@Nullable
	private ImageConfig imageConfig;

	@Data
	public static class ImageConfig {
		private int width;
		private int height;
		@Nullable
		private String overlayImageUrl;
		@Nullable
		private String backgroundImageUrl;
		private int primaryColor;
		private int secondaryColor;

		@Nullable
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
