package gjum.minecraft.civ.snitchmod.common;

import gjum.minecraft.civ.snitchmod.common.model.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;

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

	public void updateSnitchFromJalist(JalistEntry jalist) {
		Snitch snitch = snitches.getOrDefault(getId(jalist), new Snitch(jalist));
		snitch.updateFromJalist(jalist);
		snitches.put(getId(snitch), snitch);
		if (db != null) db.upsertSnitch(snitch);
	}

	public void updateSnitchFromAlert(SnitchAlert alert) {
		Snitch snitch = snitches.getOrDefault(getId(alert), new Snitch(alert));
		snitch.updateFromAlert(alert);
		snitches.put(getId(snitch), snitch);
		if (db != null) db.upsertSnitch(snitch);
	}

	@Nullable
	public Snitch deleteSnitch(WorldPos pos) {
		Snitch snitch = snitches.remove(getId(pos));
		if (db != null) db.deleteSnitch(pos);
		return snitch;
	}

	private static String getId(WorldPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + pos.getWorld();
	}
}
