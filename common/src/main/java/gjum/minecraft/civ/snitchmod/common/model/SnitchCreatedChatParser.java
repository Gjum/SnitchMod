package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnitchCreatedChatParser {
	// Created Snitch on group GROUPNAME at [123 45 -321]
	static Pattern createdPattern = Pattern.compile("^Created (\\S+) on group (\\S+) at \\[(?:\\(?([^\\n)]+)\\)? )?([-0-9.]+) ([-0-9.]+) ([-0-9.]+)\\].*");

	public static Snitch getSnitchCreationFromChat(Component message, String server, String world, @NotNull UUID clientUuid) {
		String text = message.getString().replaceAll("§.", "");

		Matcher textMatch = createdPattern.matcher(text);
		if (!textMatch.matches()) return null;

		String group = textMatch.group(2);
		int x = Integer.parseInt(textMatch.group(5));
		int y = Integer.parseInt(textMatch.group(6));
		int z = Integer.parseInt(textMatch.group(7));

		Snitch snitch = new Snitch(server, world, x, y, z);
		snitch.updateFromCreation(group, clientUuid);
		return snitch;
	}
}
