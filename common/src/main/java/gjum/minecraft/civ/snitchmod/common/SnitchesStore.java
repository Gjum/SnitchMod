package gjum.minecraft.civ.snitchmod.common;

import gjum.minecraft.civ.snitchmod.common.model.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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

	public void updateSnitchFromAlert(SnitchAlert alert) {
		Snitch snitch = snitches.getOrDefault(getId(alert), new Snitch(alert));
		snitch.updateFromAlert(alert);
		snitches.put(getId(snitch), snitch);
		List<Snitch> jalistSnitches = new ArrayList<Snitch>(1);
		jalistSnitches.add(snitch);
		if (db != null) db.upsertSnitches(jalistSnitches);
	}

	public void updateSnitchFromCreation(Snitch snitch) {
		// don't reuse any existing snitch, it no longer exists, only the new snitch does
		snitches.put(getId(snitch), snitch);
		List<Snitch> jalistSnitches = new ArrayList<Snitch>(1);
		jalistSnitches.add(snitch);
		if (db != null) db.upsertSnitches(jalistSnitches);
		// TODO remember last created snitch for placement helper
	}

	public void updateSnitchBroken(SnitchBroken snitchBroken) {
		Snitch snitch = snitches.getOrDefault(getId(snitchBroken), new Snitch(snitchBroken));
		snitch.updateFromBroken(snitchBroken);
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
