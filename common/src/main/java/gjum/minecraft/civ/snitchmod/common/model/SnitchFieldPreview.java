package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class SnitchFieldPreview {
	private Snitch source;
	private Direction direction;
	private Snitch field;

	public SnitchFieldPreview(
		Snitch source,
		Direction direction
	) {
		this.source = source;
		this.direction = direction;

		BlockPos previewPos = transposeSnitchFieldPositionByDirection(source.getPos(), direction);
		this.field = new Snitch(
			new WorldPos(
				source.getPos().getServer(),
				source.getPos().getWorld(),
				previewPos.getX(),
				previewPos.getY(),
				previewPos.getZ()
			)
		);
	}

	public static @NotNull BlockPos transposeSnitchFieldPositionByDirection(
		@NotNull
		BlockPos fieldCenter,
		@NotNull
		Direction direction
	) {
		int x = fieldCenter.getX();
		int y = fieldCenter.getY();
		int z = fieldCenter.getZ();
		double yaw = direction.yaw().value();
		yaw = ((double) yaw + 180) % 360;
		if (yaw < 0) {
			yaw += 360;
		}
		double pitch = direction.pitch().value();

		// Straight up
		if (pitch <= -60) {
			return new BlockPos(x, y+23, z);
		// Bit lower
		} else if ((pitch > -60) && (pitch <= -30)) {
			y += 22;
		// Middle
		} else if ((pitch > -30) && (pitch <= 30)) {
			// North
			if ((yaw >= 337.5) || (yaw < 22.5)) {
				return new BlockPos(x, y, z-23);
			// East
			} else if ((yaw >= 67.5) && (yaw < 112.5)) {
				return new BlockPos(x+23, y, z);
			// South
			} else if ((yaw >= 157.5) && (yaw < 202.5)) {
				return new BlockPos(x, y, z+23);
			// West
			} else if ((yaw >= 247.5) && (yaw < 292.5)) {
				return new BlockPos(x-23, y, z);
			}
		// Bit lower
		} else if ((pitch > 30) && (pitch <= 60)) {
			y -= 22;
		// Straight down
		} else if (pitch >= 60) {
			return new BlockPos(x, y-23, z);
		}

		// North
		if ((yaw >= 337.5) || (yaw < 22.5)) {
			return new BlockPos(x, y, z-22);
		// North East
		} else if ((yaw >= 22.5) && (yaw < 67.5)) {
			return new BlockPos(x+22, y, z-22);
		// East
		} else if ((yaw >= 67.5) && (yaw < 112.5)) {
			return new BlockPos(x+22, y, z);
		// South East
		} else if ((yaw >= 112.5) && (yaw < 157.5)) {
			return new BlockPos(x+22, y, z+22);
		// South
		} else if ((yaw >= 157.5) && (yaw < 202.5)) {
			return new BlockPos(x, y, z+22);
		// South West
		} else if ((yaw >= 202.5) && (yaw < 247.5)) {
			return new BlockPos(x-22, y, z+22);
		// West
		} else if ((yaw >= 247.5) && (yaw < 292.5)) {
			return new BlockPos(x-22, y, z);
		// North West
		} else if ((yaw >= 292.5) && (yaw < 337.5)) {
			return new BlockPos(x-22, y, z-22);
		}

		throw new IllegalArgumentException(
			String.format(
				"Out of range values of yaw %f and/or pitch %f.",
				direction.yaw().value(),
				direction.pitch().value()
			)
		);
	}

	public Snitch source() {
		return this.source;
	}

	public Direction direction() {
		return this.direction;
	}

	public Snitch field() {
		return this.field;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SnitchFieldPreview)) {
			return false;
		}
		SnitchFieldPreview p = (SnitchFieldPreview)obj;
		return this.field.pos.equals(p.field.pos);
	}
}
