package gjum.minecraft.civ.snitchmod.common;

import gjum.minecraft.civ.snitchmod.common.model.Snitch;
import gjum.minecraft.civ.snitchmod.common.model.WorldPos;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class SnitchSqliteDb {
	public final String server;

	private Connection conn;

	public SnitchSqliteDb(String server) throws ClassNotFoundException, SQLException {
		this.server = server;
		new File("SnitchMod/" + server).mkdirs();
		Class.forName("org.sqlite.JDBC"); // load driver
		conn = DriverManager.getConnection("jdbc:sqlite:SnitchMod/" + server + "/snitches.sqlite");
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

	private static final String pkeySnitches = "world,x,y,z";

	synchronized
	private void createTableSnitch() throws SQLException {
		if (conn == null) return;
		String sql = "CREATE TABLE IF NOT EXISTS snitches" +
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
				" PRIMARY KEY (" + pkeySnitches + "));";
		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sql);
		}
	}

	synchronized
	public Collection<Snitch> selectAllSnitches() {
		final ArrayList<Snitch> snitches = new ArrayList<>();
		try (Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM snitches");
			while (rs.next()) {
				final Snitch snitch = new Snitch(
						server,
						rs.getString("world"),
						rs.getInt("x"),
						rs.getInt("y"),
						rs.getInt("z")
				);
				snitch.setFromDb(
						rs.getString("group_name"),
						rs.getString("type"),
						rs.getString("name"),
						rs.getLong("dormant_ts"),
						rs.getLong("cull_ts"),
						rs.getLong("first_seen_ts"),
						rs.getLong("last_seen_ts"));
				snitches.add(snitch);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return snitches;
	}

	synchronized
	public void upsertSnitch(Snitch snitch) {
		if (conn == null) return;
		String sql = "INSERT INTO snitches (world,x,y,z,group_name,type,name,dormant_ts,cull_ts,first_seen_ts,last_seen_ts)" +
				" VALUES (?,?,?,?,?,?,?,?,?,?,?)" +
				"ON CONFLICT (" + pkeySnitches + ") DO UPDATE SET " +
				"group_name = excluded.group_name," +
				"type = excluded.type," +
				"name = excluded.name," +
				"dormant_ts = excluded.dormant_ts," +
				"cull_ts = excluded.cull_ts," +
				"first_seen_ts = excluded.first_seen_ts," +
				"last_seen_ts = excluded.last_seen_ts";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	synchronized
	public void deleteSnitch(WorldPos pos) {
		if (conn == null) return;
		String sql = "DELETE FROM snitches WHERE world = ? AND x = ? AND y = ? AND z = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			int i = 0;
			pstmt.setString(++i, pos.getWorld());
			pstmt.setInt(++i, pos.getX());
			pstmt.setInt(++i, pos.getY());
			pstmt.setInt(++i, pos.getZ());

			pstmt.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
