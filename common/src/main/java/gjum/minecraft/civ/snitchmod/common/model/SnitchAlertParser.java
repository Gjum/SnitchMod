package gjum.minecraft.civ.snitchmod.common.model;

import gjum.minecraft.civ.snitchmod.common.model.SnitchAlert;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnitchAlertParser {
	// Enter  PLAYER  GROUPNAME  [123 45 -321]  [12m North West]
	static Pattern alertPattern = Pattern.compile("^(Enter|Login|Logout) +([A-Za-z0-9_]{3,17}) +([^ ]+) +\\[(?:([A-Za-z][^ ]+),? )?([-0-9]+),? ([-0-9]+),? ([-0-9]+)\\].*");
	// §6Location: §b(world) [123 45 -321]\n§6Name: §bSNITCHNAME\n§6Group: §bGROUPNAME
	static Pattern hoverPattern = Pattern.compile("Location: (?:\\(?([^\\n)]+)\\)? )?\\[([-0-9]+),? ([-0-9]+),? ([-0-9]+)\\] *\\nName: ([^ ]+) *\\nGroup: ([^ ]+).*", Pattern.MULTILINE);

	@Nullable
	public static SnitchAlert getSnitchAlertFromChat(Component message, String server) {
		String text = message.getString().replaceAll("§.", "");

		Matcher textMatch = alertPattern.matcher(text);
		if (!textMatch.matches()) return null;

		String action = textMatch.group(1);
		String accountName = textMatch.group(2);
		String snitchName = textMatch.group(3);
		String world = textMatch.group(4) == null ? "world" : textMatch.group(4);
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
				world = hoverMatch.group(1) == null ? world : hoverMatch.group(1);
				group = hoverMatch.group(6);
			}
		}

		long now = System.currentTimeMillis();

		return new SnitchAlert(now, server, action, accountName, snitchName, world, x, y, z, group);
	}
}
