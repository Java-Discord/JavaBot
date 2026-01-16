package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
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
 * The `/form reopen` command.
 */
public class ReopenFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;
	private final FormInteractionManager interactionManager;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo          the forms repository
	 * @param interactionManager form interaction manager
	 * @param botConfig          main bot configuration
	 */
	public ReopenFormSubcommand(FormsRepository formsRepo, FormInteractionManager interactionManager,
			BotConfig botConfig) {
		this.formsRepo = formsRepo;
		this.interactionManager = interactionManager;
		setCommandData(new SubcommandData("reopen", "Reopen a closed form").addOptions(
				new OptionData(OptionType.INTEGER, "form-id", "The ID of a closed form to reopen", true, true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		long id = event.getOption("form-id", OptionMapping::getAsLong);
		Optional<FormData> formOpt = formsRepo.getForm(id);
		if (formOpt.isEmpty()) {
			event.reply("A form with this ID was not found.").setEphemeral(true).queue();
			return;
		}
		FormData form = formOpt.get();

		if (!form.closed()) {
			event.reply("This form is already opened").setEphemeral(true).queue();
			return;
		}

		event.deferReply(true).queue();

		interactionManager.reopenForm(event.getGuild(), form);

		event.getHook().sendMessage("Form reopened!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		event.replyChoices(
				formsRepo.getAllForms(true).stream().map(form -> new Choice(form.toString(), form.id())).toList())
				.queue();
	}
}
