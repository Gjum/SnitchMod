package gjum.minecraft.civ.snitchmod.common;

import gjum.minecraft.civ.snitchmod.common.model.*;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SnitchesStore {
	public final @NotNull String server;

	private final HashMap<WorldPos, Snitch> snitches = new HashMap<>();

	private @Nullable SnitchSqliteDb db;

	public SnitchesStore(@NotNull String server) {
		this.server = server;
		try {
			db = new SnitchSqliteDb(server);
			for (Snitch snitch : db.selectAllSnitches()) {
				snitches.put(snitch.pos, snitch);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		if (db != null) db.close();
	}

	public @Nullable Snitch getSnitch(@NotNull WorldPos pos) {
		return snitches.get(pos);
	}

	public @Nullable Snitch getSnitch(@NotNull String world, @NotNull BlockPos pos) {
		return snitches.get(new WorldPos(server, world, pos));
	}

	public Collection<Snitch> getAllSnitches() {
		return snitches.values();
	}

	public void updateSnitchesFromJalist(List<JalistEntry> jalist) {
		List<Snitch> jalistSnitches = new ArrayList<>(jalist.size());
		for (JalistEntry entry : jalist) {
			Snitch snitch = snitches.computeIfAbsent(entry.pos, Snitch::new);
			snitch.updateFromJalist(entry);
			jalistSnitches.add(snitch);
		}
		if (db != null) db.upsertSnitches(jalistSnitches);
	}

	public void updateSnitchFromRename(SnitchRename rename) {
		Snitch snitch = snitches.computeIfAbsent(rename.pos, Snitch::new);
		snitch.updateFromRename(rename);
		if (db != null) db.upsertSnitch(snitch);
	}

	public void updateSnitchFromAlert(SnitchAlert alert) {
		Snitch snitch = snitches.computeIfAbsent(alert.pos, Snitch::new);
		snitch.updateFromAlert(alert);
		if (db != null) db.upsertSnitch(snitch);
	}

	public void updateSnitchFromCreation(Snitch snitch) {
		// don't reuse any existing snitch, it no longer exists, only the new snitch does
		snitches.put(snitch.pos, snitch);
		if (db != null) db.upsertSnitch(snitch);
		// TODO remember last created snitch for placement helper
	}

	public void updateSnitchBroken(SnitchBroken snitchBroken) {
		Snitch snitch = snitches.computeIfAbsent(snitchBroken.pos, Snitch::new);
		snitch.updateFromBroken(snitchBroken);
		if (db != null) db.upsertSnitch(snitch);
	}

	/**
	 * There is no snitch at the given WorldPos
	 *
	 * @return The snitch that was at the WorldPos,
	 * or null if no snitch was ever known there.
	 */
/**	public @Nullable Snitch updateSnitchGone(@NotNull WorldPos pos) {
		Snitch snitch = snitches.get(pos);
		if (snitch == null) return null;
		snitch.updateGone();
		if (db != null) db.upsertSnitch(snitch);
		return snitch;
	} */
}
