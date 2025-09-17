package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Optional;

import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * The `/form delete` command.
 */
public class DeleteFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public DeleteFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("delete", "Delete an existing form")
				.addOptions(new OptionData(OptionType.INTEGER, "form-id", "The ID of a form to delete", true, true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {

		long id = event.getOption("form-id", OptionMapping::getAsLong);
		Optional<FormData> formOpt = formsRepo.getForm(id);
		if (formOpt.isEmpty()) {
			event.reply("A form with this ID was not found.").setEphemeral(true).queue();
			return;
		}

		event.deferReply(true).queue();

		FormData form = formOpt.get();
		formsRepo.deleteForm(form);

		if (form.isAttached()) {
			DetachFormSubcommand.detachFromMessage(form, event.getGuild());
			// TODO send a warning
		}

		event.getHook().sendMessage("Form deleted!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		event.replyChoices(
				formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.getId())).toList())
				.queue();
	}
}
