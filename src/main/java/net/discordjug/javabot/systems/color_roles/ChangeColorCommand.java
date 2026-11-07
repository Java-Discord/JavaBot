package net.discordjug.javabot.systems.color_roles;

import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.util.AutoCompleteUtils;

public class ChangeColorCommand extends SlashCommand implements AutoCompletable {
	
	private static final Pattern COLOR_PATTERN = Pattern.compile("#[A-F0-9]{6}", Pattern.CASE_INSENSITIVE);
	
	private final BotConfig config;
	private final ColorRoleService service;
	
	public ChangeColorCommand(BotConfig botConfig, ColorRoleService service) {
		this.config = botConfig;
		this.service = service;
		setCommandData(Commands.slash("color", "Sets the color of your roles")
				.addOption(OptionType.STRING, "color", "The hex code of the role color", false, true));
	}
	
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		Role baseRole = config.get(event.getGuild()).getModerationConfig().getColorRoleChangeRole();
		if (baseRole == null) {
			Responses.error(event, "This feature is not available.").queue();
			return;
		}
		if (!event.getMember().getRoles().contains(baseRole)) {
			Responses.error(event, "Only users with the " + baseRole.getAsMention() + " role can change their color.")
				.setAllowedMentions(List.of())
				.queue();
			return;
		}
		
		String colorArgument = event.getOption("color", "", OptionMapping::getAsString);
		
		Color color;
		if (colorArgument.isEmpty()) {
			color = null;
		} else {
			if (!colorArgument.startsWith("#")) {
				colorArgument = "#" + colorArgument;
			}
			if (!COLOR_PATTERN.matcher(colorArgument).matches()) {
				Responses.error(event, "'%s' is not a valid color", colorArgument).queue();
				return;
			}
			try {
				color = Color.decode(colorArgument);
			} catch (NumberFormatException e) {
				Responses.error(event, "'%s' is not a valid color", colorArgument).queue();
				return;
			}
		}
		
		boolean removed = service.removeColor(event.getMember());
		if (color == null) {
			if (removed) {
				Responses.success(event, "Color removed", "Your color role has been removed.").queue();
			} else {
				Responses.error(event, "You don't have a color role.").queue();
			}
			return;
		}
		service.addColor(event.getMember(), color);
		Responses.success(event, "Color set", "Your color has been set to `%s`.", service.encodeColor(color)).queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		Role baseRole = config.get(event.getGuild()).getModerationConfig().getColorRoleChangeRole();
		if (baseRole == null) {
			event.replyChoice("Feature unavailable", "This feature is not available.").queue();
			return;
		}
		if (!event.getMember().getRoles().contains(baseRole)) {
			event.replyChoice("Not permitted", "You cannot use this feature.").queue();
			return;
		}
		List<Choice> choices = service.getColorRoles(event.getGuild())
				.stream()
				.map(rc -> new Choice(service.encodeColor(rc.color()), service.encodeColor(rc.color())))
				.toList();
		event.replyChoices(AutoCompleteUtils.filterChoices(event, choices)).queue();
	}
}
