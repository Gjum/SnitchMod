package gjum.minecraft.civ.snitchmod.common.model;

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

import gjum.minecraft.civ.snitchmod.common.model.Snitch.Type;

public class JalistEntry {
	/**
	 * When the `/jalist` entry was taken.
	 * Milliseconds since UNIX epoch.
	 */
	public final long ts;
	public final @NotNull WorldPos pos;
	public final @NotNull String group;
	/**
	 * Block id: note_block or jukebox
	 */
	public final @NotNull Type type;
	public final @NotNull String name;
	/**
	 * "soft cull" date; ms since UNIX epoch.
	 * 0 means unknown (e.g. from alert).
	 */
	public final long dormantTs;
	/**
	 * "hard cull" date; ms since UNIX epoch.
	 * 0 means unknown (e.g. from alert).
	 */
	public final long cullTs;

	public JalistEntry(
			long ts,
			@NotNull WorldPos pos,
			@NotNull String group,
			@NotNull Type type,
			@NotNull String name,
			long dormantTs, long cullTs
	) {
		this.ts = ts;
		this.pos = pos;
		this.group = group;
		this.type = type;
		this.name = name;
		this.dormantTs = dormantTs;
		this.cullTs = cullTs;
	}

	private static final Pattern locationPattern = Pattern.compile("^Location: (?:([A-Za-z][^ ]+),? )?([-0-9]+),? ([-0-9]+),? ([-0-9]+)");
	private static final Pattern groupPattern = Pattern.compile("^Group: ([^ ]+)");
	private static final Pattern lifetimePattern = Pattern.compile("^Will (cull|go dormant) in (?:([0-9]+) h(?:our)?s? ?)?(?:([0-9]+) min(?:ute)?s? ?)?(?:([0-9]+) sec(?:ond)?s?)?\s*");

	@Nullable
	public static JalistEntry fromStack(ItemStack stack, @NotNull String server) {
		Type type;
		if (stack.getItem() == Items.NOTE_BLOCK) {
			type = Type.NOTEBLOCK;
		} else if (stack.getItem() == Items.JUKEBOX) {
			type = Type.JUKEBOX;
		} else return null;

		CompoundTag displayTag = stack.getTagElement("display");
		if (displayTag == null) return null;
		if (displayTag.getTagType("Name") != TAG_STRING) return null;
		if (displayTag.getTagType("Lore") != TAG_LIST) return null;

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

		long ts = System.currentTimeMillis();

		String lifetimeType = lifetimeMatch.group(1);
		long h = 0;
		if (lifetimeMatch.group(2) != null) {
			h = Integer.parseInt(lifetimeMatch.group(2));
		}
		long m = 0;
		if (lifetimeMatch.group(3) != null) {
			m = Integer.parseInt(lifetimeMatch.group(3));
		}
		long s = 0;
		if (lifetimeMatch.group(4) != null) {
			s = Integer.parseInt(lifetimeMatch.group(4));
		}
		long lifetimeDurationMs = (h * 3600L + m * 60L + s) * 1000L;

		long dormantTs = 0, cullTs = 0;
		if ("go dormant".equals(lifetimeType)) {
			dormantTs = ts + lifetimeDurationMs;
		} else if ("cull".equals(lifetimeType)) {
			cullTs = ts + lifetimeDurationMs;
		} else {
			System.err.println("Ignoring malformed jalist entry with lifetime type: " + lifetimeType);
			return null;
		}

		var pos = new WorldPos(server, world, x, y, z);
		return new JalistEntry(ts, pos, group, type, name, dormantTs, cullTs);
	}

	private static String getStringFromChatJson(String json) {
		return Component.Serializer.fromJson(json).getString();
	}
}
