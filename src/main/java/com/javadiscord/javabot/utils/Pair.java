package com.javadiscord.javabot.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Pair <F, S> {
	private final F first;
	private final S second;
}
