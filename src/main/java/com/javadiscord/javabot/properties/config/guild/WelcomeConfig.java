package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.annotation.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
public class WelcomeConfig extends GuildConfigItem {
	@Nullable
	private String joinMessageTemplate;
	@Nullable
	private String leaveMessageTemplate;
	private long channelId;
	private boolean enabled;

	@Nullable
	private ImageConfig imageConfig;

	public TextChannel getChannel() {
		return this.getGuild().getTextChannelById(this.channelId);
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
