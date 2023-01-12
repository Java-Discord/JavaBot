package net.javadiscord.javabot.systems.help.commands;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.systems.help.HelpManager;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * A simple command that can be used inside reserved help channels to
 * immediately unreserve them, instead of waiting for a timeout.
 */
public class UnreserveCommand extends SlashCommand {
	private final BotConfig botConfig;
	private final DbActions dbActions;
	private final HelpAccountRepository helpAccountRepository;
	private final HelpTransactionRepository helpTransactionRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param dbActions A utility object providing various operations on the main database
	 * @param helpTransactionRepository Dao object that represents the HELP_TRANSACTION SQL Table.
	 * @param helpAccountRepository Dao object that represents the HELP_ACCOUNT SQL Table.
	 */
	public UnreserveCommand(BotConfig botConfig, DbActions dbActions, HelpTransactionRepository helpTransactionRepository, HelpAccountRepository helpAccountRepository) {
		this.botConfig = botConfig;
		this.dbActions = dbActions;
		this.helpAccountRepository = helpAccountRepository;
		this.helpTransactionRepository = helpTransactionRepository;
		setCommandData(Commands.slash("unreserve", "Unreserves this post marking your question/issue as resolved.")
				.setGuildOnly(true)
				.addOption(OptionType.STRING, "reason", "The reason why you're unreserving this channel", false)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		// check whether the channel type is either text or thread (possible forum post?)
		if (event.getChannelType() != ChannelType.TEXT && event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD) {
			replyInvalidChannel(event);
			return;
		}
		ThreadChannel postThread = event.getChannel().asThreadChannel();
		if (postThread.getParentChannel().getType() != ChannelType.FORUM) {
			replyInvalidChannel(event);
		}
		HelpManager manager = new HelpManager(postThread, dbActions, botConfig, helpAccountRepository, helpTransactionRepository);
		if (manager.isForumEligibleToBeUnreserved(event.getInteraction())) {
			manager.close(event, event.getUser().getIdLong() == postThread.getOwnerIdLong(),
					event.getOption("reason", null, OptionMapping::getAsString)
			);
		} else {
			Responses.warning(event, "Could not close this post", "You're not allowed to close this post.").queue();
		}
	}

	private void replyInvalidChannel(CommandInteraction interaction) {
		Responses.warning(interaction, "Invalid Channel",
						"This command may only be used in either the text-channel-based help system, or in our new forum help system.")
				.queue();
	}
}
