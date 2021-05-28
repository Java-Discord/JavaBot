package Other;

import Commands.Other.Version;

import java.awt.*;

public class Constants {

        public static final String CHECK = "✅";
        public static final String CROSS = "❎";
        public static final String TRASH = "\uD83D\uDDD1";
        public static final String FAILURE = "<abort:759740784882089995>";
        public static final String SUCCESS = "<check:759740404941455440>";
        public static final String REACTION_FAILURE = "abort:759740784882089995";
        public static final String REACTION_SUCCESS = "check:759740404941455440";
        public static final Color GRAY  = Color.decode("#2F3136");
        public static final Color GREEN  = Color.decode("#2ECC71");
        public static final Color RED  = Color.decode("#FF212D");
        public static final String WEBSITE  = "https://javadiscord.net";
        public static final String HELP_IMAGE  = "https://cdn.discordapp.com/attachments/711245550271594556/847517860749508629/unknown.png";

        public static final String HELP_MAIN = "** - currently running version ``" + new Version().getVersion() + "``\n``() - optional``, ``<> - required``\n\nCommands marked with a ``*`` require special permissions.";
        public static final String HELP_USER  = "!avatar\n!botinfo\n!chmm\n!idcalc\n!lmgtfy\n!ping\n!profile\n!serverinfo\n!uptime\n!leaderboard";
        public static final String HELP_MOD  = "*!ban\n*!clearwarns\n*!editembed\n*!embed\n*!kick\n*!mute\n*!mutelist\n*!purge\n*!report\n*!unban\n*!unmute\n*!warn\n*!warns\n*!clearqotw\n*!rr\n*!cc";
        public static final String HELP_OTHER  = "*!question\n*!config\n*!welcomeimage\n*!accept\n*!decline\n*!clear\n*!response\n*!correct\n!guild";
}
