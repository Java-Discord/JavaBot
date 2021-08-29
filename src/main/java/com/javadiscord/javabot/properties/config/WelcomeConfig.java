package com.javadiscord.javabot.properties.config;

import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

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

	public TextChannel getChannel(Guild guild) {
		return guild.getTextChannelById(this.channelId);
	}

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
