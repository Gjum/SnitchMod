package gjum.minecraft.civ.snitchmod.common.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SnitchAlert extends WorldPos {
	/**
	 * ms since UNIX epoch when alert happened
	 */
	public final long ts;
	@NotNull
	public final String action;
	@NotNull
	public final String accountName;
	@NotNull
	public final String snitchName;
	@Nullable
	public final String group;

	public SnitchAlert(
			long ts,
			@NotNull String server,
			@NotNull String action,
			@NotNull String accountName,
			@NotNull String snitchName,
			@NotNull String world,
			int x, int y, int z,
			@Nullable String group) {
		super(server, world, x, y, z);
		this.ts = ts;
		this.action = action;
		this.accountName = accountName;
		this.snitchName = snitchName;
		this.group = group;
	}
}
