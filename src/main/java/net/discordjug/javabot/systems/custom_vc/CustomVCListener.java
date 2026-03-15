package net.discordjug.javabot.systems.custom_vc;

import java.util.List;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.ExceptionLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

/**
 * Automatically creates a temporary voice channel when a user joins the {@link ModerationConfig#getCustomVoiceChannel() custom voice channel template} and deletes that voice channel when there are no users left.
 */
@RequiredArgsConstructor
public class CustomVCListener extends ListenerAdapter {
	private final BotConfig botConfig;
	private final CustomVCRepository repository;
	private final CustomVCButtonHandler buttonHandler;
	
	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
		AudioChannelUnion vcJoined = event.getChannelJoined();
		long customVoiceChannelId = botConfig.get(event.getGuild()).getModerationConfig().getCustomVoiceChannelId();
		if (vcJoined != null && vcJoined.getIdLong() == customVoiceChannelId) {
			createCustomVC(event, vcJoined);
		}
		AudioChannelUnion vcLeft = event.getChannelLeft();
		if (vcLeft != null && repository.isCustomVoiceChannel(vcLeft.getIdLong()) && vcLeft.getMembers().isEmpty()) {
			vcLeft.delete().queue();
		}
	}

	private void createCustomVC(GuildVoiceUpdateEvent event, AudioChannelUnion vcJoined) {
		ChannelAction<? extends StandardGuildChannel> copy = vcJoined.createCopy();
		copy
			.setName("custom-" + event.getMember().getId())
			.addMemberPermissionOverride(
					event.getMember().getIdLong(),
					List.of(Permission.MANAGE_CHANNEL, Permission.VIEW_CHANNEL),
					List.of())
			.queue(newChannel -> {
				repository.addCustomVoiceChannel(newChannel.getIdLong(), event.getMember().getIdLong());
				if (!(newChannel instanceof VoiceChannel newVC)) {
					ExceptionLogger.capture(new IllegalStateException("expected VoiceChannel to be created, got " + newChannel.getClass().getCanonicalName()));
					newChannel.delete().queue();
					return;
				}
				event.getGuild().moveVoiceMember(event.getMember(), newVC).queue();
				newVC.sendMessageEmbeds(new EmbedBuilder()
						.setTitle("Your personal Voice Channel")
						.setDescription("""
								This is your personal, temporary voice channel.
								You can configure this channel using the button below, the `/vc-control` command or the Discord settings.
								This channel will be deleted as soon as all people leave this channel.
								""")
						.build())
				.addContent(event.getMember().getAsMention())
				.addComponents(ActionRow.of(buttonHandler.createMakePrivateButton()))
				.queue();
			});
	}
	
	@Override
	public void onReady(ReadyEvent event) {
		for (long channelId : repository.getAllCustomVoiceChannels()) {
			VoiceChannel vc = event.getJDA().getVoiceChannelById(channelId);
			if (vc == null) {
				repository.removeCustomVoiceChannel(channelId);
			} else if (vc.getMembers().isEmpty()) {
				vc.delete().queue();
				repository.removeCustomVoiceChannel(channelId);
			}
		}
	}
}
