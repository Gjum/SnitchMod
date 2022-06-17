package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gjum.minecraft.civ.snitchmod.common.Utils.nonEmptyOrDefault;

public class SnitchAlert {
	/**
	 * ms since UNIX epoch when alert happened
	 */
	public final long ts;
	public final @NotNull WorldPos pos;
	public final @NotNull String action;
	public final @NotNull String accountName;
	public final @NotNull String snitchName;
	public final @Nullable String group;

	public SnitchAlert(
			long ts,
			@NotNull WorldPos pos,
			@NotNull String action,
			@NotNull String accountName,
			@NotNull String snitchName,
			@Nullable String group) {
		this.ts = ts;
		this.pos = pos;
		this.action = action;
		this.accountName = accountName;
		this.snitchName = snitchName;
		this.group = group;
	}

	// Enter  PLAYER  SNITCHNAME  [123 45 -321]  [12m North West]
	static Pattern alertPattern = Pattern.compile("^(Enter|Login|Logout) +([A-Za-z0-9_]{3,17}) +(.+) +\\[(?:([A-Za-z][^ ]+),? )?([-0-9]+),? ([-0-9]+),? ([-0-9]+)\\].*");
	// §6Location: §b(world) [123 45 -321]\n§6Name: §bSNITCHNAME\n§6Group: §bGROUPNAME
	static Pattern hoverPattern = Pattern.compile("Location: (?:\\(?([^\\n)]+)\\)? )?\\[([-0-9]+),? ([-0-9]+),? ([-0-9]+)\\] *\\n(?:Name: ([^\\n]+)\\n)?Group: ([^ ]+).*", Pattern.MULTILINE);

	@Nullable
	public static SnitchAlert fromChat(
			@NotNull Component message,
			@NotNull String server,
			@NotNull String world
	) {
		String text = message.getString().replaceAll("§.", "");

		Matcher textMatch = alertPattern.matcher(text);
		if (!textMatch.matches()) return null;

		String action = textMatch.group(1);
		String accountName = textMatch.group(2);
		String snitchName = textMatch.group(3);
		world = nonEmptyOrDefault(textMatch.group(4), world);
		int x = Integer.parseInt(textMatch.group(5));
		int y = Integer.parseInt(textMatch.group(6));
		int z = Integer.parseInt(textMatch.group(7));

		String group = null;
		final HoverEvent hoverEvent = message.getSiblings().get(0).getStyle().getHoverEvent();
		if (hoverEvent != null && hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT) {
			@SuppressWarnings("ConstantConditions")
			String hoverText = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT).getString().replaceAll("§.", "");

			Matcher hoverMatch = hoverPattern.matcher(hoverText);
			if (hoverMatch.matches()) {
				world = nonEmptyOrDefault(hoverMatch.group(1), world);
				group = hoverMatch.group(6);
			}
		}

		long now = System.currentTimeMillis();

		var pos = new WorldPos(server, world, x, y, z);

		return new SnitchAlert(now, pos, action, accountName, snitchName, group);
	}
}
