package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormUser;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;

/**
 * The `/form submissions-export` command. Export a list of users who have
 * submitted the specified form from the database in JSON format.
 * 
 * @see FormData
 */
public class SubmissionsExportFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public SubmissionsExportFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("submissions-export", "Export all of the form's submissions")
				.addOptions(new OptionData(OptionType.INTEGER, FORM_ID_FIELD, "The ID of a form to get submissions for",
						true, true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			Responses.error(event, "Couldn't find a form with this id").queue();
			return;
		}

		event.deferReply().setEphemeral(false).queue();
		FormData form = formOpt.get();
		Map<FormUser, Integer> submissions = formsRepo.getSubmissionsCountPerUser(form);
		JsonObject root = new JsonObject();
		JsonObject details = new JsonObject();
		JsonArray users = new JsonArray();
		submissions.forEach((formUser, value) -> {
			JsonObject uobj = new JsonObject();
			uobj.addProperty("username", formUser.username());
			uobj.addProperty("submissions", value);
			details.add(Long.toString(formUser.id()), uobj);
			users.add(formUser.username());
		});
		root.add("users", users);
		root.add("details", details);
		event.getHook().sendFiles(FileUpload.fromData(gson.toJson(root).getBytes(StandardCharsets.UTF_8),
				"submissions_" + form.id() + ".json")).queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}
}
