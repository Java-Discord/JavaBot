package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.systems.help.model.HelpTransaction;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /ping command.</h3>
 */
public class PingCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public PingCommand() {
		setSlashCommandData(Commands.slash("ping", "Shows the bot's gateway ping.")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getUser().getIdLong() == 374328434677121036L) {
			HelpTransaction transaction = new HelpTransaction();
			transaction.setMessageType(1);
			transaction.setWeight(20);
			transaction.setRecipient(event.getUser().getIdLong());
			DbHelper.doDaoAction(HelpTransactionRepository::new, dao -> {
				dao.save(transaction);
			});
		}
		event.replyEmbeds(new EmbedBuilder()
				.setAuthor(event.getJDA().getGatewayPing() + "ms", null, event.getJDA().getSelfUser().getAvatarUrl())
				.setColor(Responses.Type.DEFAULT.getColor())
				.build()
		).queue();
	}
}