package gjum.minecraft.civ.snitchmod.common;

import gjum.minecraft.civ.snitchmod.common.model.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SnitchesStore {
	public final String server;

	private final HashMap<String, Snitch> snitches = new HashMap<>();

	private @Nullable SnitchSqliteDb db;

	public SnitchesStore(String server) {
		this.server = server;
		try {
			db = new SnitchSqliteDb(server);
			for (Snitch snitch : db.selectAllSnitches()) {
				snitches.put(getId(snitch), snitch);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		if (db != null) db.close();
	}

	public Collection<Snitch> getAllSnitches() {
		return snitches.values();
	}

	public void updateSnitchesFromJalist(List<JalistEntry> jalist) {
		List<Snitch> jalistSnitches = new ArrayList<>(jalist.size());
		for (JalistEntry entry : jalist) {
			Snitch snitch = snitches.computeIfAbsent(getId(entry.pos), id -> new Snitch(entry.pos));
			snitch.updateFromJalist(entry);
			jalistSnitches.add(snitch);
		}
		if (db != null) db.upsertSnitches(jalistSnitches);
	}

	public void updateSnitchFromRename(SnitchRename rename) {
		Snitch snitch = snitches.computeIfAbsent(getId(rename.pos), id -> new Snitch(rename.pos));
		snitch.updateFromRename(rename);
		if (db != null) db.upsertSnitch(snitch);
	}

	public void updateSnitchFromAlert(SnitchAlert alert) {
		Snitch snitch = snitches.computeIfAbsent(getId(alert.pos), id -> new Snitch(alert.pos));
		snitch.updateFromAlert(alert);
		if (db != null) db.upsertSnitch(snitch);
	}

	public void updateSnitchFromCreation(Snitch snitch) {
		// don't reuse any existing snitch, it no longer exists, only the new snitch does
		snitches.put(getId(snitch), snitch);
		if (db != null) db.upsertSnitch(snitch);
		// TODO remember last created snitch for placement helper
	}

	public void updateSnitchBroken(SnitchBroken snitchBroken) {
		Snitch snitch = snitches.computeIfAbsent(getId(snitchBroken.pos), id -> new Snitch(snitchBroken.pos));
		snitch.updateFromBroken(snitchBroken);
		if (db != null) db.upsertSnitch(snitch);
	}

	/**
	 * There is no snitch at the given WorldPos
	 *
	 * @return The snitch that was at the WorldPos,
	 * or null if no snitch was ever known there.
	 */
	public @Nullable Snitch updateSnitchGone(WorldPos pos) {
		Snitch snitch = snitches.get(getId(pos));
		if (snitch == null) return null;
		snitch.updateGone();
		if (db != null) db.upsertSnitch(snitch);
		return snitch;
	}

	private static String getId(Snitch snitch) {
		return getId(snitch.pos);
	}

	private static String getId(WorldPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + pos.getWorld();
	}
}
