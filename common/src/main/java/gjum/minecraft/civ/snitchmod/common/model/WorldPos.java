package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class WorldPos extends BlockPos {
	@NotNull
	public final String server;
	@NotNull
	public final String world;

	public WorldPos(
			@NotNull String server,
			@NotNull String world,
			int x, int y, int z) {
		super(x, y, z);
		this.server = server;
		this.world = world;
	}

	public WorldPos(
			@NotNull String server,
			@NotNull String world,
			BlockPos pos) {
		super(pos);
		this.server = server;
		this.world = world;
	}

	@NotNull
	public String getServer() {
		return server;
	}

	@NotNull
	public String getWorld() {
		return world;
	}

	@NotNull
	public BlockPos getBlockPos() {
		return this;
	}

	public Vec3 getCenter() {
		return new Vec3(getX() + .5, getY() + .5, getZ() + .5);
	}
}
