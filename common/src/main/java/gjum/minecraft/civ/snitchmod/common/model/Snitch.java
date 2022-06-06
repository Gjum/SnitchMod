package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Snitch extends WorldPos {
	@Nullable
	private String group;
	/**
	 * block id: NOTEBLOCK or JUKEBOX
	 */
	@Nullable
	private String type;
	@Nullable
	private String name;
	/**
	 * "soft cull" date; ms since UNIX epoch.
	 * 0 means unknown (e.g. from alert).
	 */
	private long dormantTs;
	/**
	 * "hard cull" date; ms since UNIX epoch.
	 * 0 means unknown (e.g. from alert).
	 */
	private long cullTs;
	/**
	 * who created the snitch.
	 * null means unknown who created it.
	 */
	private @Nullable UUID creatorUuid;
	/**
	 * when snitch was created; ms since UNIX epoch.
	 * 0 means unknown when it was created.
	 */
	private long createdTs;
	/**
	 * when snitch was first seen; ms since UNIX epoch.
	 * 0 means unknown (e.g. never seen).
	 */
	private long firstSeenTs;
	/**
	 * when snitch was last seen; ms since UNIX epoch.
	 * 0 means unknown (e.g. never seen).
	 */
	private long lastSeenTs;

	public Snitch(
			@NotNull String server,
			@NotNull String world,
			int x, int y, int z) {
		super(server, world, x, y, z);
	}

	public Snitch(WorldPos p) {
		super(p.server, p.world, p.getX(), p.getY(), p.getZ());
	}

	public void setFromDb(
			@Nullable String group,
			@Nullable String type,
			@Nullable String name,
			long dormantTs,
			long cullTs,
			long firstSeenTs,
			long lastSeenTs
	) {
		this.group = group;
		this.type = type;
		this.name = name;
		this.dormantTs = dormantTs;
		this.cullTs = cullTs;
		this.firstSeenTs = firstSeenTs;
		this.lastSeenTs = lastSeenTs;
	}

	public void updateFromJalist(JalistEntry jalist) {
		group = jalist.group;
		type = jalist.type;
		name = jalist.name;
		dormantTs = jalist.dormantTs;
		cullTs = jalist.cullTs;
		if (firstSeenTs == 0 || firstSeenTs > jalist.ts) firstSeenTs = jalist.ts;
		if (lastSeenTs < jalist.ts) lastSeenTs = jalist.ts;
	}

	public void updateFromAlert(SnitchAlert alert) {
		group = alert.group;
		name = alert.snitchName;
		if (dormantTs > 0 && dormantTs < alert.ts) dormantTs = alert.ts;
		if (cullTs > 0 && cullTs < alert.ts) cullTs = alert.ts;
		if (firstSeenTs == 0 || firstSeenTs > alert.ts) firstSeenTs = alert.ts;
		if (lastSeenTs < alert.ts) lastSeenTs = alert.ts;
	}

	public void updateFromCreation(String group, UUID creatorUuid) {
		this.group = group;
		this.creatorUuid = creatorUuid;
		createdTs = System.currentTimeMillis();
		firstSeenTs = createdTs;
		lastSeenTs = createdTs;
		// TODO set dormantTs/cullTs from server config
	}

	public void updateFromBroken(SnitchBroken snitchBroken) {
		this.group = snitchBroken.group;
		lastSeenTs = System.currentTimeMillis();
		dormantTs = 0;
		cullTs = 0;
	}

	public AABB getRangeAABB() {
		return new AABB(this).inflate(11);
	}

	@Nullable
	public String getGroup() {
		return group;
	}

	@Nullable
	public String getType() {
		return type;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public boolean hasDormantTs() {
		return dormantTs != 0;
	}

	/**
	 * "soft cull" date; ms since UNIX epoch.
	 * 0 means unknown (e.g. from alert).
	 */
	public long getDormantTs() {
		return dormantTs;
	}

	public boolean hasCullTs() {
		return cullTs != 0;
	}

	/**
	 * "hard cull" date; ms since UNIX epoch.
	 * 0 means unknown (e.g. from alert).
	 */
	public long getCullTs() {
		return cullTs;
	}

	public @Nullable UUID getCreatorUuid() {
		return creatorUuid;
	}

	public long getCreatedTs() {
		return createdTs;
	}

	public long getFirstSeenTs() {
		return firstSeenTs;
	}

	public long getLastSeenTs() {
		return lastSeenTs;
	}
}
