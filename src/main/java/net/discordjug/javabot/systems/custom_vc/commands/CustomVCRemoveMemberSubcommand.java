package net.discordjug.javabot.systems.custom_vc.commands;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.custom_vc.CustomVCRepository;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

/**
 * Command for removing a member from a custom voice channel.
 * Only the owner of the custom voice channel can use this command.
 * This command ensures that the member in question is no longer in the custom voice channel and can no longer join that channel.
 */
public class CustomVCRemoveMemberSubcommand extends CustomVCChangeMembersSubcommand {

	public CustomVCRemoveMemberSubcommand(CustomVCRepository dataStorage,
			BotConfig botConfig) {
		super(new SubcommandData("remove-member", "removes a member to the voice channel"), dataStorage, botConfig);
	}

	@Override
	protected void apply(VoiceChannel vc, Member member, SlashCommandInteractionEvent event) {
		if (member.getRoles().contains(botConfig.get(event.getGuild()).getModerationConfig().getStaffRole())) {
			Responses.error(event, "Cannot remove staff members from custom voice channels.").queue();
			return;
		}
		
		if (event.getMember().getIdLong() == member.getIdLong()) {
			Responses.error(event, "You cannot perform this action on yourself.").queue();
			return;
		}
		
		if (member.getVoiceState().getChannel() != null && member.getVoiceState().getChannel().getIdLong() == vc.getIdLong()) {
			VoiceChannel afkChannel = event.getGuild().getAfkChannel();
			if (afkChannel == null) {
				event.getGuild().kickVoiceMember(member).queue();
			} else {
				event.getGuild().moveVoiceMember(member, afkChannel).queue();
			}
		}
		
		vc.upsertPermissionOverride(member)
			.setDenied(Permission.VIEW_CHANNEL)
			.queue();
		event
			.reply("Successfully removed " + member.getAsMention() + " from " + vc.getAsMention() + ". They can no longer join the voice channel.")
			.setEphemeral(true)
			.queue();
	}
}
