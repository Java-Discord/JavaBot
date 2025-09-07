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
 * The `/form submissions-delete` command.
 */
public class SubmissionsDeleteFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public SubmissionsDeleteFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(
				new SubcommandData("submissions-delete", "Deletes submissions of an user in the form").addOptions(
						new OptionData(OptionType.INTEGER, "form-id", "The ID of a form to get submissions for", true,
								true),
						new OptionData(OptionType.STRING, "user-id", "User to delete submissions of", true, true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		event.deferReply().setEphemeral(true).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption("form-id", OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("Couldn't find a form with this id").queue();
			return;
		}

		String user = event.getOption("user-id", OptionMapping::getAsString);
		FormData form = formOpt.get();

		int count = formsRepo.deleteSubmissions(form, user);
		event.getHook().sendMessage("Deleted " + count + " of this user's submissions!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		switch (target.getName()) {
			case "user-id" -> {
				Long formId = event.getOption("form-id", OptionMapping::getAsLong);
				if (formId != null) {
					Optional<FormData> form = formsRepo.getForm(formId);
					if (form.isPresent()) {
						event.replyChoices(formsRepo.getAllSubmissions(form.get()).keySet().stream()
								.map(user -> new Choice(user.getUsername(), Long.toString(user.getId()))).toList())
								.queue();
						return;
					}
				}
				event.replyChoices().queue();
			}
			case "form-id" -> event.replyChoices(
					formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.getId())).toList())
					.queue();
			default -> {}
		}
	}
}
