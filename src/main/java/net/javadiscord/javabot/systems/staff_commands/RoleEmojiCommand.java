package net.javadiscord.javabot.systems.staff_commands;

import org.jetbrains.annotations.NotNull;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;

/**
 * This class represents the /add-role-emoji command.
 * This command allows adding emojis which are usable only be members with certain roles.
 */
public class RoleEmojiCommand extends SlashCommand{

	private final BotConfig botConfig;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public RoleEmojiCommand(BotConfig botConfig) {
		this.botConfig=botConfig;
		SlashCommandData slashCommandData = Commands.slash("add-role-emoji", "Adds an emoji only usable only with certain roles")
				.addOption(OptionType.STRING, "name", "The name of the emoji", true)
				.addOption(OptionType.ATTACHMENT, "emoji", "the emoji", true)
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
				.setGuildOnly(true);
		for (int i = 1; i <= 10; i++) {
			slashCommandData.addOption(OptionType.ROLE, "role-"+i, "A role allowed to use the emoji", i==1);
		}
		setSlashCommandData(slashCommandData);
		requireUsers(botConfig.getSystems().getAdminConfig().getAdminUsers());
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (!Checks.hasAdminRole(botConfig, event.getMember())) {
			Responses.replyAdminOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		Attachment attachment = event.getOption("emoji",null, OptionMapping::getAsAttachment);
		Role[] roles = event
				.getOptions()
				.stream()
				.filter(option -> option.getType() == OptionType.ROLE)
				.map(option -> option.getAsRole())
				.toArray(Role[]::new);
		event.deferReply().queue();
		attachment.getProxy().downloadAsIcon().thenAccept(icon->{
			event
				.getGuild()
				.createEmoji(event.getOption("name",attachment.getFileName(), OptionMapping::getAsString), icon, roles)
				.queue(emoji -> {
					event.getHook().sendMessage("Emoji "+emoji.getName()+" successfully created").queue();
				},e->{
					event.getHook().sendMessage("Cannot create emoji because `"+e.getMessage()+"`").queue();
				});
		}).exceptionally(e -> {
			event.getHook().sendMessage("Cannot create emoji because `"+e.getMessage()+"`").queue();
			return null;
		});
	}
}
