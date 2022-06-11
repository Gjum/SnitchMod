package gjum.minecraft.civ.snitchmod.common;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlacementHelper {
	public static @Nullable BlockPos transposeSnitchFieldPositionByDirection(
			@NotNull
			BlockPos fieldCenter,
			double yaw,
			double pitch) {
		int x = fieldCenter.getX();
		int y = fieldCenter.getY();
		int z = fieldCenter.getZ();
		yaw = ((double) yaw + 180) % 360;
		if (yaw < 0) {
			yaw += 360;
		}

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

		return null;
	}
}
