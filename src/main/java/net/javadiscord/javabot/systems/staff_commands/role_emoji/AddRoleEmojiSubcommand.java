package net.javadiscord.javabot.systems.staff_commands.role_emoji;

import org.jetbrains.annotations.NotNull;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand.Subcommand;

import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;

/**
 * This class represents the /emoji-admin add subcommand.
 * This subcommand allows adding emojis which are usable only be members with certain roles.
 */
public class AddRoleEmojiSubcommand extends Subcommand{

	private final BotConfig botConfig;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}s.
	 * @param botConfig The main configuration of the bot
	 */
	public AddRoleEmojiSubcommand(BotConfig botConfig) {
		this.botConfig=botConfig;
		SubcommandData subCommandData = new SubcommandData("add", "Adds an emoji only usable only with certain roles")
				.addOption(OptionType.STRING, "name", "The name of the emoji", true)
				.addOption(OptionType.ATTACHMENT, "emoji", "the emoji", true);
		for (int i = 1; i <= 10; i++) {
			subCommandData.addOption(OptionType.ROLE, "role-"+i, "A role allowed to use the emoji", i==1);
		}
		setSubcommandData(subCommandData );
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
