package com.javadiscord.javabot.events.gist;

import com.javadiscord.javabot.external_apis.github.File;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class GistState {
	private List<File> files;
	private int position;
	private Instant created;
	private long channelId;
}
