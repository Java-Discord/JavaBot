package net.discordjug.javabot.systems.custom_vc.commands;

import net.dv8tion.jda.api.interactions.commands.build.Commands;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * Command for managing custom voice channels.
 */
public class CustomVCControlCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param addMemberSubcommand /vc-control add-member
	 * @param removeMemberSubcommand /vc-control remove-member
	 */
	public CustomVCControlCommand(CustomVCAddMemberSubcommand addMemberSubcommand, CustomVCRemoveMemberSubcommand removeMemberSubcommand) {
		setCommandData(Commands.slash("vc-control", "Manages custom voice channels")
				.setGuildOnly(true)
		);
		addSubcommands(addMemberSubcommand, removeMemberSubcommand);
	}
}
