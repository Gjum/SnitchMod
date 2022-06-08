package gjum.minecraft.civ.snitchmod.common;

import gjum.minecraft.civ.snitchmod.common.model.Snitch;
import gjum.minecraft.civ.snitchmod.common.model.WorldPos;

import java.io.File;
import java.sql.*;
import java.util.*;

public class SnitchSqliteDb {
	public final String server;

	private Connection conn;

	public SnitchSqliteDb(String server) throws ClassNotFoundException, SQLException {
		this.server = server;
		String serverDir = server.replaceAll(":", "~");
		new File("SnitchMod/" + serverDir).mkdirs();
		Class.forName("org.sqlite.JDBC"); // load driver
		conn = DriverManager.getConnection("jdbc:sqlite:SnitchMod/" + serverDir + "/snitches.sqlite");
		createTableSnitch();
	}

	synchronized
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static final String tableSnitches = "snitches_v2";
	private static final String pkeySnitches = "world,x,y,z";

	synchronized
	private void createTableSnitch() throws SQLException {
		if (conn == null) return;
		String sql = "CREATE TABLE IF NOT EXISTS " + tableSnitches + " " +
				"( world TEXT" +
				", x INT" +
				", y INT" +
				", z INT" +
				", group_name TEXT" +
				", type TEXT" +
				", name TEXT" +
				", dormant_ts BIGINT" +
				", cull_ts BIGINT" +
				", first_seen_ts BIGINT" +
				", last_seen_ts BIGINT" +
				", created_ts BIGINT" +
				", created_by_uuid TEXT" +
				", renamed_ts BIGINT" +
				", renamed_by_uuid TEXT" +
				", lost_jalist_access_ts BIGINT" +
				", broken_ts BIGINT" +
				", gone_ts BIGINT" +
				", tags TEXT" +
				", notes TEXT" +
				", PRIMARY KEY (" + pkeySnitches + ")" +
				");";
		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sql);
		}
	}

	synchronized
	public Collection<Snitch> selectAllSnitches() {
		final ArrayList<Snitch> snitches = new ArrayList<>();
		try (Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableSnitches);
			while (rs.next()) {
				final Snitch snitch = new Snitch(
						server,
						rs.getString("world"),
						rs.getInt("x"),
						rs.getInt("y"),
						rs.getInt("z"),
						rs.getString("group_name"),
						rs.getString("type"),
						rs.getString("name"),
						rs.getLong("dormant_ts"),
						rs.getLong("cull_ts"),
						rs.getLong("first_seen_ts"),
						rs.getLong("last_seen_ts"),
						rs.getLong("created_ts"),
						rs.getString("created_by_uuid"),
						rs.getLong("renamed_ts"),
						rs.getString("renamed_by_uuid"),
						rs.getLong("lost_jalist_access_ts"),
						rs.getLong("broken_ts"),
						rs.getLong("gone_ts"),
						rs.getString("tags"),
						rs.getString("notes"));
				snitches.add(snitch);
			}
		} catch (SQLException e) {
			System.err.println("Failed loading all snitches");
			e.printStackTrace();
		}
		return snitches;
	}

	synchronized
	public void upsertSnitch(Snitch snitch) {
		upsertSnitches(Collections.singletonList(snitch));
	}

	synchronized
	public void upsertSnitches(List<Snitch> snitches) {
		if (conn == null) return;
		String sql = "INSERT INTO " + tableSnitches + " (world,x,y,z,group_name,type,name,dormant_ts,cull_ts,first_seen_ts,last_seen_ts,created_ts,created_by_uuid,renamed_ts,renamed_by_uuid,lost_jalist_access_ts,broken_ts,gone_ts,tags,notes)" +
				" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)" +
				"ON CONFLICT (" + pkeySnitches + ") DO UPDATE SET " +
				"group_name = excluded.group_name," +
				"type = excluded.type," +
				"name = excluded.name," +
				"dormant_ts = excluded.dormant_ts," +
				"cull_ts = excluded.cull_ts," +
				"first_seen_ts = excluded.first_seen_ts," +
				"last_seen_ts = excluded.last_seen_ts," +
				"created_ts = excluded.created_ts," +
				"created_by_uuid = excluded.created_by_uuid," +
				"renamed_ts = excluded.renamed_ts," +
				"renamed_by_uuid = excluded.renamed_by_uuid," +
				"lost_jalist_access_ts = excluded.lost_jalist_access_ts," +
				"broken_ts = excluded.broken_ts," +
				"gone_ts = excluded.gone_ts," +
				"tags = excluded.tags," +
				"notes = excluded.notes";

		PreparedStatement pstmt;
		try {
			pstmt = conn.prepareStatement(sql);
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			System.err.printf("Failed updating %d snitches\n", snitches.size());
			e.printStackTrace();
			return;
		}

		for (Snitch snitch : snitches) {
			try {
				int i = 0;
				pstmt.setString(++i, snitch.getWorld());
				pstmt.setInt(++i, snitch.getX());
				pstmt.setInt(++i, snitch.getY());
				pstmt.setInt(++i, snitch.getZ());
				pstmt.setString(++i, snitch.getGroup());
				pstmt.setString(++i, snitch.getType());
				pstmt.setString(++i, snitch.getName());
				pstmt.setLong(++i, snitch.getDormantTs());
				pstmt.setLong(++i, snitch.getCullTs());
				pstmt.setLong(++i, snitch.getFirstSeenTs());
				pstmt.setLong(++i, snitch.getLastSeenTs());
				pstmt.setLong(++i, snitch.getCreatedTs());
				pstmt.setString(++i, uuidStringOrNull(snitch.getCreatedByUuid()));
				pstmt.setLong(++i, snitch.getRenamedTs());
				pstmt.setString(++i, uuidStringOrNull(snitch.getRenamedByUuid()));
				pstmt.setLong(++i, snitch.getLostJalistAccessTs());
				pstmt.setLong(++i, snitch.getBrokenTs());
				pstmt.setLong(++i, snitch.getGoneTs());
				pstmt.setString(++i, String.join("\n", snitch.getTags()));
				pstmt.setString(++i, snitch.getNotes());

				pstmt.addBatch();
			} catch (SQLException e) {
				System.err.printf(
						"Failed updating %d snitches: Failed updating snitch %s\n", snitches.size(), snitch);
				e.printStackTrace();
				return;
			}
		}

		try {
			pstmt.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			System.err.printf("Failed updating %d snitches\n", snitches.size());
			e.printStackTrace();
			return;
		}
	}

	private static String uuidStringOrNull(UUID uuid) {
		if (uuid == null) return null;
		return uuid.toString();
	}

	synchronized
	public void deleteSnitch(WorldPos pos) {
		if (conn == null) return;
		String sql = "DELETE FROM " + tableSnitches + " WHERE world = ? AND x = ? AND y = ? AND z = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			int i = 0;
			pstmt.setString(++i, pos.getWorld());
			pstmt.setInt(++i, pos.getX());
			pstmt.setInt(++i, pos.getY());
			pstmt.setInt(++i, pos.getZ());

			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("Failed deleting snitch at " + pos);
			e.printStackTrace();
		}
	}
}
