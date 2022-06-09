package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnitchRename extends WorldPos {
	public final long ts;
	@NotNull
	public final String snitchName;
	@Nullable
	private final String snitchOldName;
	@Nullable
	public final String group;
	@NotNull
	public final UUID clientUuid;

	public SnitchRename(
			long ts,
			@NotNull String server,
			@NotNull String snitchName,
			@Nullable String snitchOldName,
			@NotNull String world,
			int x, int y, int z,
			@Nullable String group,
			@NotNull UUID clientUuid) {
		super(server, world, x, y, z);
		this.ts = ts;
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

	@Nullable
	public static SnitchRename fromChat(Component message, String server, @NotNull UUID clientUuid) {
		String text = message.getString().replaceAll("§.", "");

		Matcher textMatch = renamePattern.matcher(text);
		if (!textMatch.matches()) return null;

		String snitchName = textMatch.group(1);
		String snitchOldName = textMatch.group(2);

		String world = null;
		int x = 0;
		int y = 0;
		int z = 0;
		String group = null;
		final HoverEvent hoverEvent = message.getSiblings().get(0).getStyle().getHoverEvent();
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) return null;

		@SuppressWarnings("ConstantConditions")
		String hoverText = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT).getString().replaceAll("§.", "");

		Matcher hoverMatch = hoverPattern.matcher(hoverText);
		if (!hoverMatch.matches()) return null;

		world = hoverMatch.group(1) == null ? world : hoverMatch.group(1);
		x = Integer.parseInt(hoverMatch.group(2));
		y = Integer.parseInt(hoverMatch.group(3));
		z = Integer.parseInt(hoverMatch.group(4));
		group = hoverMatch.group(6);

		long now = System.currentTimeMillis();

		return new SnitchRename(now, server, snitchName, snitchOldName, world, x, y, z, group, clientUuid);
	}
}
