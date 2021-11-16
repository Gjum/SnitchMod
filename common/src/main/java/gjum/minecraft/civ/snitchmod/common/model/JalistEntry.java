package gjum.minecraft.civ.snitchmod.common.model;

import org.jetbrains.annotations.NotNull;

public class JalistEntry extends WorldPos {
	/**
	 * ms since UNIX epoch.
	 */
	public final long ts;
	@NotNull
	public final String group;
	/**
	 * block id: NOTEBLOCK or JUKEBOX
	 */
	@NotNull
	public final String type;
	@NotNull
	public final String name;
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
			@NotNull String server,
			@NotNull String world,
			int x, int y, int z,
			@NotNull String group,
			@NotNull String type,
			@NotNull String name,
			long dormantTs, long cullTs
	) {
		super(server, world, x, y, z);
		this.ts = ts;
		this.group = group;
		this.type = type;
		this.name = name;
		this.dormantTs = dormantTs;
		this.cullTs = cullTs;
	}
}
