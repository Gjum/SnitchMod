package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gjum.minecraft.civ.snitchmod.common.Utils.nonEmptyOrDefault;

public class SnitchRename {
	public final long ts;
	public final @NotNull WorldPos pos;
	public final @NotNull String snitchName;
	private final @Nullable String snitchOldName;
	public final @Nullable String group;
	public final @NotNull UUID clientUuid;

	public SnitchRename(
			long ts,
			@NotNull WorldPos pos,
			@NotNull String snitchName,
			@Nullable String snitchOldName,
			@Nullable String group,
			@NotNull UUID clientUuid) {
		this.ts = ts;
		this.pos = pos;
		this.snitchName = snitchName;
		this.snitchOldName = snitchOldName;
		this.group = group;
		this.clientUuid = clientUuid;
	}

	// Changed snitch name to new_name from old_name
	// Changed snitch name to new_name from
	static Pattern renamePattern = Pattern.compile("^\s*Changed snitch name to +([^ ]+) from *([^ ]*).*");
	// §6Location: §b(world) [123 45 -321]\n§6Name: §bSNITCHNAME\n§6Group: §bGROUPNAME
	static Pattern hoverPattern = Pattern.compile("Location: (?:\\(?([^\\n)]+)\\)? )?\\[([-0-9]+),? ([-0-9]+),? ([-0-9]+)\\] *\\nName: ([^ ]+) *\\nGroup: ([^ ]+).*", Pattern.MULTILINE);

	public static @Nullable SnitchRename fromChat(
			@NotNull Component message,
			@NotNull String server,
			@NotNull String world,
			@NotNull UUID clientUuid
	) {
		String text = message.getString().replaceAll("§.", "");

		Matcher textMatch = renamePattern.matcher(text);
		if (!textMatch.matches()) return null;

		String snitchName = textMatch.group(1);
		String snitchOldName = textMatch.group(2);

		final HoverEvent hoverEvent = message.getSiblings().get(0).getStyle().getHoverEvent();
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) return null;

		@SuppressWarnings("ConstantConditions")
		String hoverText = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT).getString().replaceAll("§.", "");

		Matcher hoverMatch = hoverPattern.matcher(hoverText);
		if (!hoverMatch.matches()) return null;

		world = nonEmptyOrDefault(hoverMatch.group(1), world);
		int x = Integer.parseInt(hoverMatch.group(2));
		int y = Integer.parseInt(hoverMatch.group(3));
		int z = Integer.parseInt(hoverMatch.group(4));
		String group = hoverMatch.group(6);

		var pos = new WorldPos(server, world, x, y, z);

		long now = System.currentTimeMillis();

		return new SnitchRename(now, pos, snitchName, snitchOldName, group, clientUuid);
	}
}
