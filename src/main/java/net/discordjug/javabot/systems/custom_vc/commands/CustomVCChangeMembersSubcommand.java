package net.discordjug.javabot.systems.custom_vc.commands;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.custom_vc.CustomVCRepository;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * Abstract/base subcommand for changing the allowed members in custom voice channels.
 */
abstract class CustomVCChangeMembersSubcommand extends SlashCommand.Subcommand {
	
	protected final BotConfig botConfig;
	private final CustomVCRepository repository;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SubcommandData}.
	 * @param subcommandData the configuration (name and description) of the subcommand
	 * @param repository The repository storing information about custom voice channels
	 * @param botConfig the main configuration of the bot
	 */
	protected CustomVCChangeMembersSubcommand(SubcommandData subcommandData, CustomVCRepository repository, BotConfig botConfig) {
		this.botConfig = botConfig;
		this.repository = repository;
		setCommandData(
				subcommandData
					.addOption(OptionType.USER, "member", "The member in question")
		);
	}
	
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (botConfig.get(event.getGuild()).getModerationConfig().getCustomVoiceChannelId() == 0) {
			Responses.error(event, "This feature is disabled.").queue();
			return;
		}
		VoiceChannel vc = getCustomVoiceChannel(event);
		if (vc == null) {
			Responses.error(event, "This command be used in custom voice channels only. You can create a custom voice channel by joining " + botConfig.get(event.getGuild()).getModerationConfig().getCustomVoiceChannel().getAsMention() + ".")
				.queue();
			return;
		}
		if (repository.getOwnerId(vc.getIdLong()) != event.getMember().getIdLong()) {
			Responses.error(event, "Only the owner of the custom voice channel can use this command. You can create your own custom voice channel by joining " + botConfig.get(event.getGuild()).getModerationConfig().getCustomVoiceChannel().getAsMention() + ".")
			.queue();
			return;
		}
		Member member = event.getOption("member", null, OptionMapping::getAsMember);
		if (member == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		
		apply(vc, member, event);
	}
	
	protected abstract void apply(VoiceChannel vc, Member member, SlashCommandInteractionEvent event);

	private VoiceChannel getCustomVoiceChannel(SlashCommandInteractionEvent event) {
		if (repository.isCustomVoiceChannel(event.getChannelIdLong()) && event.getChannel().getType() == ChannelType.VOICE) {
			return event.getChannel().asVoiceChannel();
		}
		GuildVoiceState voiceState = event.getMember().getVoiceState();
		if (voiceState == null) {
			return null;
		}
		AudioChannelUnion channel = voiceState.getChannel();
		if (channel == null || channel.getType() != ChannelType.VOICE) {
			return null;
		}
		if (repository.isCustomVoiceChannel(voiceState.getChannel().getIdLong())) {
			return voiceState.getChannel().asVoiceChannel();
		}
		return null;
	}

}
