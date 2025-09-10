package net.discordjug.javabot.systems.staff_commands.forms.commands;

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * The `/form` command.
 */
public class FormCommand extends SlashCommand {

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param createSub            form create subcommand
	 * @param deleteSub            form delete subcommand
	 * @param closeSub             form close subcommand
	 * @param reopenSub            form reopen subcommand
	 * @param detailsSub           form details subcommand
	 * @param modifySub            form modify subcommand
	 * @param addFieldSub          form add-field subcommand
	 * @param removeFieldSub       form remove-field subcommand
	 * @param showSub              form show subcommands
	 * @param attachSub            form attach subcommand
	 * @param detachSub            form detach subcommand
	 * @param submissionsGetSub    form submissions-get subcommand
	 * @param submissionsDeleteSub form submissions-delete subcommand
	 *
	 */
	public FormCommand(CreateFormSubcommand createSub, DeleteFormSubcommand deleteSub, CloseFormSubcommand closeSub,
			ReopenFormSubcommand reopenSub, DetailsFormSubcommand detailsSub, ModifyFormSubcommand modifySub,
			AddFieldFormSubcommand addFieldSub, RemoveFieldFormSubcommand removeFieldSub, ShowFormSubcommand showSub,
			AttachFormSubcommand attachSub, DetachFormSubcommand detachSub,
			SubmissionsExportFormSubcommand submissionsGetSub, SubmissionsDeleteFormSubcommand submissionsDeleteSub) {
		setCommandData(Commands.slash("form", "Commands for managing modal forms")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED).setGuildOnly(true));
		addSubcommands(createSub, deleteSub, closeSub, reopenSub, detailsSub, modifySub, addFieldSub, removeFieldSub,
				showSub, attachSub, detachSub, submissionsGetSub, submissionsDeleteSub);
	}
}
