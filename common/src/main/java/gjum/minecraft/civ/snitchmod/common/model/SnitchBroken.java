package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnitchBroken extends WorldPos {
	public final String group;

	public SnitchBroken(@NotNull String server, @NotNull String world, int x, int y, int z, String group) {
		super(server, world, x, y, z);
		this.group = group;
	}

	// Snitch was reinforced on GROUP owned by OWNER
	private static final Pattern brokenPattern = Pattern.compile("^(\\S+) was reinforced on (\\S+) owned by ([A-Za-z0-9_]{3,17}).*");

	public static SnitchBroken fromChat(
			Component message,
			@NotNull BlockPos lastBrokenBlockPos,
			String server,
			String world
	) {
		String text = message.getString().replaceAll("§.", "");

		Matcher textMatch = brokenPattern.matcher(text);
		if (!textMatch.matches()) return null;

		String group = textMatch.group(2);

		// XXX coords from chat should override block pos heuristic
		int x = lastBrokenBlockPos.getX();
		int y = lastBrokenBlockPos.getY();
		int z = lastBrokenBlockPos.getZ();

		return new SnitchBroken(server, world, x, y, z, group);
	}
}
