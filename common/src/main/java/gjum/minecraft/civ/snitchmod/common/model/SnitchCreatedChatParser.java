package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gjum.minecraft.civ.snitchmod.common.Utils.nonEmptyOrDefault;

public class SnitchCreatedChatParser {
	// Created Snitch on group GROUPNAME at [123 45 -321]
	static Pattern createdPattern = Pattern.compile("^Created (\\S+) on group (\\S+) at \\[(?:\\(?([^\\n)]+)\\)? )?([-0-9.]+) ([-0-9.]+) ([-0-9.]+)\\].*");

	public static Snitch fromChat(Component message, String server, String world, @NotNull UUID clientUuid) {
		String text = message.getString().replaceAll("§.", "");

		Matcher textMatch = createdPattern.matcher(text);
		if (!textMatch.matches()) return null;

		String rawType = textMatch.group(1).trim().toLowerCase();
		String type = null;
		if (rawType.equals("logsnitch")) {
			type = "jukebox";
		} else if (rawType.equals("snitch")) {
			type = "note_block";
		}

		String group = textMatch.group(2);
		world = nonEmptyOrDefault(textMatch.group(3), world);
		int x = Integer.parseInt(textMatch.group(4));
		int y = Integer.parseInt(textMatch.group(5));
		int z = Integer.parseInt(textMatch.group(6));

		Snitch snitch = new Snitch(new WorldPos(server, world, x, y, z));
		snitch.updateFromCreation(group, type, clientUuid);
		return snitch;
	}
}
