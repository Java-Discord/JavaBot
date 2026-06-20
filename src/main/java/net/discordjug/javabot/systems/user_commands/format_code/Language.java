package net.discordjug.javabot.systems.user_commands.format_code;

public enum Language {
    C("c"),
    CPP("cpp"),
    CSHARP("csharp"),
    CSS("css"),
    D("d"),
    GO("go"),
    HTML("html"),
    JAVA("java"),
    JAVASCRIPT("js"),
    KOTLIN("kotlin"),
    PHP("php"),
    PYTHON("python"),
    RUBY("ruby"),
    RUST("rust"),
    SQL("sql"),
    SWIFT("swift"),
    TYPESCRIPT("typescript"),
    XML("xml"),
    UNKNOWN("txt");

    private final String discordName;

    Language(String discordName) {
        this.discordName = discordName;
    }

    public String getDiscordName() {
        return discordName;
    }

    /**
     * Resolves a language from a string (e.g. the value of the /format-code "format"
     * option) by matching its Discord code-fence name, falling back to {@link #UNKNOWN}.
     *
     * @param name the code-fence name to look up (case-insensitive)
     * @return the matching language, or {@link #UNKNOWN} if none matches
     */
    public static Language fromString(String name) {
        for (Language language : values()) {
            if (language.discordName.equalsIgnoreCase(name)) {
                return language;
            }
        }
        return UNKNOWN;
    }
}