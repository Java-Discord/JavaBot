import Commands.Configuation.Config;
import Commands.Configuation.WelcomeImage;
import Commands.CustomCommands.CustomCommands;
import Commands.Moderation.*;
import Commands.Other.GuildConfig;
import Commands.Other.QOTW.ClearQOTW;
import Commands.Other.QOTW.Correct;
import Commands.Other.QOTW.Leaderboard;
import Commands.Other.Question;
import Commands.Other.Shutdown;
import Commands.Other.Suggestions.Accept;
import Commands.Other.Suggestions.Clear;
import Commands.Other.Suggestions.Decline;
import Commands.Other.Suggestions.Response;
import Commands.Other.Testing.*;
import Commands.Other.Version;
import Commands.ReactionRoles.ReactionRoles;
import Commands.UserCommands.*;
import Events.*;
import Properties.ConfigString;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;


public class Bot {

    public static JDA jda;
    public static EventWaiter waiter;


    public static void main(String[] args) throws Exception {

            ConfigString token = new ConfigString("token", "null");
            waiter = new EventWaiter();

            CommandClientBuilder client = new CommandClientBuilder()
                    .setOwnerId("374328434677121036")
                    .setCoOwnerIds("299555811804315648", "620615131256061972")
                    .setPrefix("!")
                    .setEmojis("✅", "⚠️", "❌")
                    .useHelpBuilder(false)
                    .addCommands(

                            //UserCommands
                            new Avatar(),
                            new BotInfo(),
                            new ChangeMyMind(),
                            new Help(),
                            new IDCalc(),
                            new Lmgtfy(),
                            new Ping(),
                            new Profile(),
                            new ServerInfo(),
                            new Uptime(),

                            //ReactionRoles
                            new ReactionRoles(),

                            //CustomCommands
                            new CustomCommands(),

                            //Other
                            new Question(),
                            new Shutdown(),
                            new Version(),
                            new GuildConfig(),

                            //Other.Testing
                            new SampleSuggestion(),
                            new RefreshCategory(),
                            new MongoDBAddUser(),
                            new Image(),
                            new AddConfigFile(),
                            new UpdateUserFiles(),

                            //Other.Suggestions
                            new Accept(),
                            new Clear(),
                            new Decline(),
                            new Response(),

                            //Other.QOTW
                            new ClearQOTW(),
                            new Correct(),
                            new Leaderboard(),

                            //Commands.Moderation
                            new Ban(),
                            new ClearWarns(),
                            new EditEmbed(),
                            new Embed(),
                            new Kick(),
                            new Mute(),
                            new Mutelist(),
                            new Purge(),
                            new Report(),
                            new Unban(),
                            new Unmute(),
                            new Warn(),
                            new Warns(),

                            //Commands.Configuration
                            new Config(),
                            new WelcomeImage()
                    );


            jda = JDABuilder.createDefault(token.getValue())
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.ACTIVITY)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                    .build();

            jda.addEventListener(waiter, client.build());



            //EVENTS
            jda.addEventListener(new GuildJoin());
            jda.addEventListener(new UserJoin());
            jda.addEventListener(new UserLeave());
            jda.addEventListener(new Startup());
            jda.addEventListener(new StatusUpdate());
            jda.addEventListener(new ReactionListener());
            jda.addEventListener(new SuggestionListener());
            jda.addEventListener(new CstmCmdListener());
            jda.addEventListener(new AutoMod());
            jda.addEventListener(new SubmissionListener());
            jda.addEventListener(new SlashCommands());
            //jda.addEventListener(new StarboardListener());

    }
}

