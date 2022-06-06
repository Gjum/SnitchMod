package gjum.minecraft.civ.snitchmod.common;

import gjum.minecraft.civ.snitchmod.common.model.JalistEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.nbt.Tag.TAG_LIST;
import static net.minecraft.nbt.Tag.TAG_STRING;

public class JalistStackParser {
	static Pattern locationPattern = Pattern.compile("^Location: (?:([A-Za-z][^ ]+),? )?([-0-9]+),? ([-0-9]+),? ([-0-9]+)");
	static Pattern groupPattern = Pattern.compile("^Group: ([^ ]+)");
	static Pattern lifetimePattern = Pattern.compile("^Will (cull|go dormant) in ([0-9]+) h(?:our)?s? ([0-9]+) min(?:ute)?s? ([0-9]+) sec(?:ond)?s?");

	@Nullable
	public static JalistEntry getJalistEntryFromStack(ItemStack stack, @NotNull String server, long cullDuration) {
		String type;
		if (stack.getItem() == Items.NOTE_BLOCK) {
			type = "noteblock";
		} else if (stack.getItem() == Items.JUKEBOX) {
			type = "jukebox";
		} else return null;

		CompoundTag displayTag = stack.getTagElement("display");
		if (displayTag == null) return null;
		if (displayTag.getTagType("Name") != TAG_STRING) return null;
		if (displayTag.getTagType("Lore") !=TAG_LIST) return null;

		String name = getStringFromChatJson(displayTag.getString("Name"));

		ListTag lores = displayTag.getList("Lore", TAG_STRING);
		if (lores.size() < 3) return null;

		Matcher locationMatch = locationPattern.matcher(
				getStringFromChatJson(
						lores.getString(0)));
		if (!locationMatch.matches()) return null;
		Matcher groupMatch = groupPattern.matcher(
				getStringFromChatJson(
						lores.getString(1)));
		if (!groupMatch.matches()) return null;
		Matcher lifetimeMatch = lifetimePattern.matcher(
				getStringFromChatJson(
						lores.getString(2)));
		if (!lifetimeMatch.matches()) return null;

		String world = locationMatch.group(1);
		int x = Integer.parseInt(locationMatch.group(2));
		int y = Integer.parseInt(locationMatch.group(3));
		int z = Integer.parseInt(locationMatch.group(4));

		String group = groupMatch.group(1);

		String lifetimeType = lifetimeMatch.group(1);
		long h = Integer.parseInt(lifetimeMatch.group(2));
		long m = Integer.parseInt(lifetimeMatch.group(3));
		long s = Integer.parseInt(lifetimeMatch.group(4));

		long dtMs = (h * 3600L + m * 60L + s) * 1000L;

		long dormant_ts, cull_ts;
		long now = System.currentTimeMillis();

		if ("go dormant".equals(lifetimeType)) {
			dormant_ts = now + dtMs;
			cull_ts = dormant_ts + cullDuration;
		} else if ("cull".equals(lifetimeType)) {
			cull_ts = now + dtMs;
			dormant_ts = cull_ts - cullDuration;
		} else {
			System.err.println("Ignoring malformed jalist entry with lifetime type: " + lifetimeType);
			return null;
		}

		return new JalistEntry(System.currentTimeMillis(), server, world, x, y, z, group, type, name, dormant_ts, cull_ts);
	}

	public static String getStringFromChatJson(String json) {
		return Component.Serializer.fromJson(json).getString();
	}
}
