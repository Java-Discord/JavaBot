package net.javadiscord.javabot.tasks;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Constants;
import net.javadiscord.javabot.events.StartupListener;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Periodically updates the bot's Discord presence by cycling through a list of
 * activities. Also sets the bot to {@link OnlineStatus#DO_NOT_DISTURB} if it's
 * not set like that already.
 * <p>
 * This updater should be added as an event listener to the bot, so that it
 * will automatically begin operation when the bot gives the ready event.
 * </p>
 */
public class PresenceUpdater extends ListenerAdapter {
	/**
	 * The executor that is responsible for the scheduled updates of the bot's
	 * presence data.
	 */
	private final ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

	/**
	 * A list of functions that take a reference to the bot's JDA client, and
	 * produce an {@link Activity} that should be displayed.
	 */
	private final List<Function<JDA, Activity>> activities;
	/**
	 * The amount of time the updater should wait before updating the activity.
	 */
	private final long delay;
	/**
	 * The unit of time that {@link PresenceUpdater#delay} is counted in.
	 */
	private final TimeUnit delayUnit;
	/**
	 * The index of the currently active activity.
	 */
	private int currentActivityIndex = 0;
	/**
	 * A reference to the bot's JDA client object. This is set as soon as the
	 * bot is ready, and so it will never be null when passed to a function.
	 */
	private JDA jda;

	/**
	 * Constructs an updater using a list of activities, and a delay and time
	 * unit to describe how frequently to cycle through them.
	 *
	 * @param activities The list of activity-producing functions.
	 * @param delay      The amount of time the updater should wait before updating the activity.
	 * @param delayUnit  The unit of time that {@link PresenceUpdater#delay} is counted in.
	 */
	public PresenceUpdater(List<Function<JDA, Activity>> activities, long delay, TimeUnit delayUnit) {
		this.activities = new CopyOnWriteArrayList<>(activities);
		this.delay = delay;
		this.delayUnit = delayUnit;
	}

	/**
	 * A list of standard Activities.
	 *
	 * @return A pre-built implementation of the {@link PresenceUpdater} that
	 * has all the necessary properties defined to reasonable defaults.
	 */
	public static PresenceUpdater standardActivities() {
		var format = "%s | %s members";
		return new PresenceUpdater(List.of(
				jda -> Activity.watching(String.format(format, Constants.WEBSITE_LINK, StartupListener.defaultGuild.getMemberCount())),
				jda -> Activity.watching(String.format(format, Constants.JAM_LINK, StartupListener.defaultGuild.getMemberCount())),
				jda -> Activity.watching(String.format(format, Constants.QOTW_LINK, StartupListener.defaultGuild.getMemberCount())),
				jda -> Activity.watching(String.format(format, Constants.GITHUB_LINK, StartupListener.defaultGuild.getMemberCount()))
		), 35, TimeUnit.SECONDS);
	}

	/**
	 * Called when the Discord bot is ready. This triggers the start of the
	 * scheduled updating of the bot's activities.
	 *
	 * @param event The ready event that's sent. Notably, contains a reference
	 *              to the bot's {@link JDA} client.
	 */
	@Override
	public void onReady(@Nonnull ReadyEvent event) {
		this.jda = event.getJDA();
		threadPool.scheduleWithFixedDelay(() -> {
			if (this.jda.getPresence().getStatus() != OnlineStatus.DO_NOT_DISTURB) {
				this.jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
			}
			if (currentActivityIndex >= this.activities.size()) currentActivityIndex = 0;
			if (!this.activities.isEmpty()) {
				this.jda.getPresence().setActivity(this.activities.get(currentActivityIndex++).apply(this.jda));
			}
		}, 0, this.delay, this.delayUnit);
	}
}
