package gjum.minecraft.civ.snitchmod.common;

import gjum.minecraft.civ.snitchmod.common.model.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SnitchesStore {
	public final String server;

	private final HashMap<String, Snitch> snitches = new HashMap<>();
	@Nullable
	private SnitchSqliteDb db;

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
		List<Snitch> jalistSnitches = new ArrayList<Snitch>(jalist.size());
		for (JalistEntry entry : jalist) {
			Snitch snitch = snitches.getOrDefault(getId(entry), new Snitch(entry));
			snitch.updateFromJalist(entry);
			snitches.put(getId(snitch), snitch);
			jalistSnitches.add(snitch);
		}
		if (db != null) db.upsertSnitches(jalistSnitches);
	}

	public void updateSnitchFromRename(SnitchRename rename) {
		Snitch snitch = snitches.getOrDefault(getId(rename), new Snitch(rename));
		snitch.updateFromRename(rename);
		snitches.put(getId(snitch), snitch);
		if (db != null) db.upsertSnitch(snitch);
	}

	public void updateSnitchFromAlert(SnitchAlert alert) {
		Snitch snitch = snitches.getOrDefault(getId(alert), new Snitch(alert));
		snitch.updateFromAlert(alert);
		snitches.put(getId(snitch), snitch);
		if (db != null) db.upsertSnitch(snitch);
	}

	public void updateSnitchFromCreation(Snitch snitch) {
		// don't reuse any existing snitch, it no longer exists, only the new snitch does
		snitches.put(getId(snitch), snitch);
		if (db != null) db.upsertSnitch(snitch);
		// TODO remember last created snitch for placement helper
	}

	public void updateSnitchBroken(SnitchBroken snitchBroken) {
		Snitch snitch = snitches.getOrDefault(getId(snitchBroken), new Snitch(snitchBroken));
		snitch.updateFromBroken(snitchBroken);
		if (db != null) db.upsertSnitch(snitch);
	}

	/**
	 * There is no snitch at the given WorldPos
	 */
	public void updateSnitchGone(WorldPos pos) {
		Snitch snitch = snitches.get(getId(pos));
		if (snitch == null) return;
		snitch.updateGone();
		if (db != null) db.upsertSnitch(snitch);
	}

	@Nullable
	public Snitch deleteSnitch(WorldPos pos) {
		Snitch snitch = snitches.remove(getId(pos));
		if (snitch == null) return null;
		if (db != null) db.deleteSnitch(pos);
		return snitch;
	}

	private static String getId(WorldPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + pos.getWorld();
	}
}
