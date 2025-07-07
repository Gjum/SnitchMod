package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Snitch {
	private final static String JUKEBOX_DB_REPRESENTATION = "jukebox";
	private final static String NOTEBLOCK_DB_REPRESENTATION = "note_block";
	public enum Type {
		JUKEBOX (1000L * 60L * 60L * 24L * 42L, JUKEBOX_DB_REPRESENTATION),
		NOTEBLOCK (1000L * 60L * 60L * 24L * 28L, NOTEBLOCK_DB_REPRESENTATION);

		public final long timer;
		public final String dbRepresentation;
		Type(long timer, String dbRepresentation) {
			this.timer = timer;
			this.dbRepresentation = dbRepresentation;
		}

		@Override
		public String toString() {
			return this.dbRepresentation;
		}
	}

	public boolean maybeRefreshed;
	public final @NotNull WorldPos pos;
	private @Nullable String group;
	private @Nullable Type type;
	private @Nullable String name;
	private long dormantTs;
	private long cullTs;
	private long firstSeenTs;
	private long lastSeenTs;
	private long createdTs;
	private @Nullable UUID createdByUuid;
	private long renamedTs;
	private @Nullable UUID renamedByUuid;
	private long lostJalistAccessTs;
	private long brokenTs;
	private long goneTs;
	private @NotNull HashSet<String> tags = new HashSet<>();
	private @Nullable String notes;

	public Snitch(@NotNull WorldPos pos) {
		this.pos = pos;
	}

	public Snitch(
		@NotNull String server,
		@NotNull String world,
		int x, int y, int z,
		@Nullable String group,
		@Nullable String rawType,
		@Nullable String name,
		long dormantTs,
		long cullTs,
		long firstSeenTs,
		long lastSeenTs,
		long createdTs,
		@Nullable String createdByUuid,
		long renamedTs,
		@Nullable String renamedByUuid,
		long lostJalistAccessTs,
		long brokenTs,
		long goneTs,
		@Nullable String tags,
		@Nullable String notes
	) {
		this.pos = new WorldPos(server, world, x, y, z);
		this.group = group;
		if (rawType != null) {
			String type = rawType.trim().toLowerCase();
			switch (type) {
			case JUKEBOX_DB_REPRESENTATION, "juke_box", "juke box", "logsnitch":
				this.type = Type.JUKEBOX;
				break;
			case NOTEBLOCK_DB_REPRESENTATION, "noteblock", "note block", "snitch":
				this.type = Type.NOTEBLOCK;
				break;
			}
		}
		this.name = name;
		this.dormantTs = dormantTs;
		this.cullTs = cullTs;
		this.firstSeenTs = firstSeenTs;
		this.lastSeenTs = lastSeenTs;
		this.createdTs = createdTs;
		if (createdByUuid != null) this.createdByUuid = UUID.fromString(createdByUuid);
		this.renamedTs = renamedTs;
		if (renamedByUuid != null) this.renamedByUuid = UUID.fromString(renamedByUuid);
		this.lostJalistAccessTs = lostJalistAccessTs;
		this.brokenTs = brokenTs;
		this.goneTs = goneTs;
		// TODO move deserialization logic to database
		this.tags.clear();
		if (tags != null && !tags.isEmpty()) {
			this.tags.addAll(Arrays.asList(tags.split("\n")));
		}
		this.notes = notes;
	}

	public void updateFromCreation(String group, @Nullable Type type, UUID createdByUuid) {
		this.group = group;
		this.createdByUuid = createdByUuid;
		this.type = type;
		createdTs = System.currentTimeMillis();
		firstSeenTs = createdTs;
		lastSeenTs = createdTs;
		if (type != null) {
			this.dormantTs = createdTs + type.timer;
		}
	}

	public void updateFromJalist(JalistEntry jalist) {
		group = jalist.group;
		type = jalist.type;
		name = jalist.name;
		dormantTs = jalist.dormantTs;
		cullTs = jalist.cullTs;
		updateSeen(jalist.ts);
		lostJalistAccessTs = 0;
		this.maybeRefreshed = false;
	}

	public void updateFromRename(SnitchRename rename) {
		this.group = rename.group;
		this.name = rename.snitchName;
		this.renamedTs = rename.ts;
		this.renamedByUuid = rename.clientUuid;
		updateSeen(rename.ts);
	}

	public void updateFromAlert(SnitchAlert alert) {
		group = alert.group;
		name = alert.snitchName;
		// if dormant/cull disagrees, we clearly don't know their true values
		if (dormantTs != 0 && dormantTs < alert.ts) dormantTs = 0;
		if (cullTs != 0 && cullTs < alert.ts) cullTs = 0;
		updateSeen(alert.ts);
	}

	public void updateFromBroken(SnitchBroken snitchBroken) {
		this.group = snitchBroken.group;
		dormantTs = 0;
		cullTs = 0;
		updateSeen(snitchBroken.ts); // must have been alive to be broken like this
		brokenTs = snitchBroken.ts;
		goneTs = snitchBroken.ts;
	}

	public void updateGone() {
		goneTs = System.currentTimeMillis();
	}

	public void updateNoLongerGone() {
		goneTs = 0;
	}

	private void updateSeen(long ts) {
		if (firstSeenTs == 0 || firstSeenTs > ts) firstSeenTs = ts;
		if (lastSeenTs < ts) lastSeenTs = ts;
	}

	public WorldPos getPos() {
		return pos;
	}

	public AABB getRangeAABB() {
		return new AABB(pos).inflate(11);
	}

	@Nullable
	public String getGroup() {
		return group;
	}

	@NotNull
	public Optional<Type> getType() {
		return Optional.ofNullable(type);
	}

	@Nullable
	public String getName() {
		return name;
	}

	public boolean isAlive() {
		if (brokenTs != 0 || goneTs != 0) return false;
		if (dormantTs == 0 && cullTs == 0) return true;
		long now = System.currentTimeMillis();
		return (dormantTs == 0 || now < dormantTs) && (cullTs == 0 || now < cullTs);
	}

	/**
	 * "soft cull" date.
	 * Milliseconds since the UNIX epoch.
	 * 0 means unknown (e.g. from alert).
	 */
	public long getDormantTs() {
		return dormantTs;
	}

	public boolean hasDormantTs() {
		return dormantTs != 0;
	}

	/**
	 * "hard cull" date.
	 * Milliseconds since the UNIX epoch.
	 * 0 means unknown (e.g. from alert).
	 */
	public long getCullTs() {
		return cullTs;
	}

	public boolean hasCullTs() {
		return cullTs != 0;
	}

	/**
	 * When the snitch was first seen.
	 * Milliseconds since the UNIX epoch.
	 * 0 means unknown (e.g. never seen).
	 */
	public long getFirstSeenTs() {
		return firstSeenTs;
	}

	/**
	 * When the snitch was last seen.
	 * Milliseconds since the UNIX epoch.
	 * 0 means unknown (e.g. never seen).
	 */
	public long getLastSeenTs() {
		return lastSeenTs;
	}

	/**
	 * When the snitch was created.
	 * Milliseconds since the UNIX epoch.
	 * 0 means unknown when it was created.
	 */
	public long getCreatedTs() {
		return createdTs;
	}

	public boolean hasCreatedTs() {
		return createdTs != 0;
	}

	/**
	 * Who created the snitch.
	 * null means unknown who created it.
	 */
	public @Nullable UUID getCreatedByUuid() {
		return createdByUuid;
	}

	/**
	 * When the snitch was last renamed by the client player.
	 * Milliseconds since the UNIX epoch.
	 * 0 means it was not renamed by the client player.
	 */
	public long getRenamedTs() {
		return renamedTs;
	}

	public boolean hasRenamedTs() {
		return renamedTs != 0;
	}

	/**
	 * UUID of the client player when she last renamed the snitch.
	 * null means it was not renamed by the client player.
	 */
	public @Nullable UUID getRenamedByUuid() {
		return renamedByUuid;
	}

	/**
	 * When the snitch was first missing from `/jalist`.
	 * Milliseconds since the UNIX epoch.
	 * 0 means unknown or still have access.
	 * This is only set if the jalist has no snitches for the snitch's `group` at all,
	 * indicating missing permissions on that group instead of a broken snitch.
	 * Otherwise, `goneTs` is set instead.
	 */
	// TODO can this be different for different snitches in a given group?
	public long getLostJalistAccessTs() {
		return lostJalistAccessTs;
	}

	public boolean haveLostJalistAccess() {
		return lostJalistAccessTs != 0;
	}

	/**
	 * Precise time when the snitch was broken.
	 * Milliseconds since the UNIX epoch.
	 * 0 means still alive, or unknown when exactly it was broken.
	 * If a snitch is gone but it's not known precisely when, `goneTs` is set instead.
	 * Always less than or equal to `goneTs`.
	 */
	public long getBrokenTs() {
		return brokenTs;
	}

	public boolean wasBroken() {
		return brokenTs != 0;
	}

	/**
	 * When the snitch was first missing from `/jalist`.
	 * Milliseconds since the UNIX epoch.
	 * 0 means alive or missing `/jalist` permissions.
	 * This is not set if the jalist has no snitches for the snitch's `group` at all,
	 * indicating missing permissions on that group instead of a broken snitch.
	 * In that case, `lostJalistAccessTs` is set instead.
	 * Always greater than or equal to `brokenTs`.
	 */
	public long getGoneTs() {
		return goneTs;
	}

	public boolean isGone() {
		return goneTs != 0;
	}

	public @NotNull HashSet<String> getTags() {
		return tags;
	}

	public @Nullable String getNotes() {
		return notes;
	}
}
