package net.discordjug.javabot;

import club.minnced.discord.webhook.send.WebhookEmbed;
import com.zaxxer.hikari.HikariConfig;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.GuildConfig;
import net.discordjug.javabot.data.config.GuildConfigItem;
import net.discordjug.javabot.data.config.SystemsConfig;
import net.discordjug.javabot.data.config.SystemsConfig.ApiConfig;
import net.discordjug.javabot.data.config.guild.HelpConfig;
import net.discordjug.javabot.data.config.guild.MessageCacheConfig;
import net.discordjug.javabot.data.config.guild.MetricsConfig;
import net.discordjug.javabot.data.config.guild.ModerationConfig;
import net.discordjug.javabot.data.config.guild.QOTWConfig;
import net.discordjug.javabot.data.config.guild.ServerLockConfig;
import net.discordjug.javabot.data.config.guild.StarboardConfig;
//import net.dv8tion.jda.api.entities.Guild;
//import net.dv8tion.jda.api.entities.Member;
//import net.dv8tion.jda.api.entities.Role;
//import net.dv8tion.jda.api.entities.ScheduledEvent;
//import net.dv8tion.jda.api.entities.ThreadMember;
//import net.dv8tion.jda.api.entities.User;
//import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
//import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
//import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
//import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.internal.entities.GuildVoiceStateImpl;
//import net.dv8tion.jda.internal.entities.MemberPresenceImpl;
import net.dv8tion.jda.internal.requests.restaction.PermOverrideData;
import org.h2.server.TcpServer;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;

/**
 * Configure classes and resources to be accessible from native-image.
 */
@Configuration
@ImportRuntimeHints(RuntimeHintsConfiguration.class)
@RegisterReflectionForBinding({
		//register config classes for reflection
		BotConfig.class, GuildConfig.class, GuildConfigItem.class, SystemsConfig.class, ApiConfig.class,
		HelpConfig.class, MessageCacheConfig.class, MetricsConfig.class, ModerationConfig.class, QOTWConfig.class, ServerLockConfig.class, StarboardConfig.class,
		
		//ensure JDA can create necessary caches
//		User[].class, Guild[].class, Member[].class, Role[].class, Channel[].class, AudioManager[].class, ScheduledEvent[].class, ThreadMember[].class, ForumTag[].class, RichCustomEmoji[].class, GuildSticker[].class, MemberPresenceImpl[].class,
		//needs to be serialized for channel managers etc
		PermOverrideData.class,
		
		HikariConfig.class
	})
public class RuntimeHintsConfiguration implements RuntimeHintsRegistrar {
	
	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		
		//ensure resources are available in native-image
		hints.resources().registerPattern("assets/**");
		hints.resources().registerPattern("database/**");
		hints.resources().registerPattern("help_guidelines/**");
		hints.resources().registerPattern("help_overview/**");
		hints.resources().registerResource(new ClassPathResource("quartz.properties"));
		
		//allow H2 to create the TCP server (necessary for starting the DB)
		hints.reflection().registerType(TcpServer.class, MemberCategory.INVOKE_PUBLIC_METHODS);
		
		// JDA needs to be able to access listener methods
		hints.reflection().registerType(ListenerAdapter.class, MemberCategory.INVOKE_PUBLIC_METHODS);
		
		// caffeine
		hints.reflection().registerTypeIfPresent(getClass().getClassLoader(), "com.github.benmanes.caffeine.cache.SSW", MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		
		for (Class<?> cl : WebhookEmbed.class.getClasses()) {
			hints.reflection().registerType(cl,  MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_PUBLIC_METHODS);
		}
		
		hints.reflection().registerType(GuildVoiceStateImpl[].class, MemberCategory.UNSAFE_ALLOCATED);
	}
}
