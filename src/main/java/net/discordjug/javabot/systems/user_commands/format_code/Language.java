package net.discordjug.javabot.systems.user_commands.format_code;

/**
 * The programming languages supported by the code-formatting commands. Each constant maps a
 * human-readable {@link #displayName} to the {@link #discordName} tag Discord uses for
 * syntax highlighting inside code blocks.
 */
public enum Language {
    C("C", "c"),
    CPP("C++", "cpp"),
    CSHARP("C#", "csharp"),
    CSS("CSS", "css"),
    D("D", "d"),
    GO("Go", "go"),
    HTML("HTML", "html"),
    JAVA("Java", "java"),
    JAVASCRIPT("JavaScript", "javascript"),
    KOTLIN("Kotlin", "kotlin"),
    PHP("PHP", "php"),
    PYTHON("Python", "python"),
    RUBY("Ruby", "ruby"),
    RUST("Rust", "rust"),
    SQL("SQL", "sql"),
    SWIFT("Swift", "swift"),
    TYPESCRIPT("TypeScript", "typescript"),
    XML("XML", "xml"),
    UNKNOWN("Unknown", "txt");

    /**
     * @return the human-readable language name shown to users (e.g. {@code "C#"})
     */
    private final String displayName;

    /**
     * @return the tag Discord uses to syntax-highlight this language in a code block (e.g. {@code "csharp"})
     */
    private final String discordName;

    Language(String displayName, String discordName) {
        this.displayName = displayName;
        this.discordName = discordName;
    }

    public String getDisplayName() {
        return displayName;
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
        try {
            return Language.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}