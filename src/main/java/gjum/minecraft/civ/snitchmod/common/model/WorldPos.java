package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WorldPos extends BlockPos {
	@NotNull
	public final String server;
	@NotNull
	public final String world;

	public WorldPos(
		@NotNull String server,
		@NotNull String world,
		int x,
		int y,
		int z
	) {
		super(x, y, z);
		this.server = server;
		this.world = world;
	}

	public WorldPos(
		@NotNull String server,
		@NotNull String world,
		BlockPos pos
	) {
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

	public Vec3 getCenter() {
		return new Vec3(getX() + .5, getY() + .5, getZ() + .5);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof WorldPos worldPos)) return false;
		if (!super.equals(o)) return false;
		return server.equals(worldPos.server) && world.equals(worldPos.world);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), server, world);
	}
}
