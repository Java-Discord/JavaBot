package net.discordjug.javabot.systems.custom_vc.commands;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.custom_vc.CustomVCRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

/**
 * Adds a member to a custom voice channel.
 */
public class CustomVCAddMemberSubcommand extends CustomVCChangeMembersSubcommand {

	public CustomVCAddMemberSubcommand(CustomVCRepository dataStorage,
			BotConfig botConfig) {
		super(new SubcommandData("add-member", "adds a member to the voice channel"), dataStorage, botConfig);
	}

	@Override
	protected void apply(VoiceChannel vc, Member member, SlashCommandInteractionEvent event) {
		vc.upsertPermissionOverride(member)
			.setAllowed(Permission.VIEW_CHANNEL)
			.queue();
		event
			.reply("Successfully added " + member.getAsMention() + " to " + vc.getAsMention() + ". They can now join the voice channel.")
			.setEphemeral(true)
			.queue();
	}
}
