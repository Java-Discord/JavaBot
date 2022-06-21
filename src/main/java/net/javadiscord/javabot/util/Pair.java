package net.javadiscord.javabot.util;

/**
 * A simple pair that contains to values.
 *
 * @param first  The first value.
 * @param second The second value.
 * @param <F>    The type of the first value.
 * @param <S>    The type of the second value.
 */
public record Pair<F, S>(F first, S second) {
}
