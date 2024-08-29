package gjum.minecraft.civ.snitchmod.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import gjum.minecraft.civ.snitchmod.common.model.Snitch;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Utils {
	public static @NotNull String nonEmptyOrDefault(@Nullable String s, @NotNull String default_) {
		if (s == null) return default_;
		if (s.isEmpty()) return default_;
		return s;
	}

	public static boolean playerIsLookingAtSnitch(LocalPlayer player, Snitch snitch) {
		Vec3 playerLookAngle = player.getLookAngle();
		Vec3 playerPos = player.getEyePosition();
		AABB snitchBox = new AABB(snitch.pos);

		double invertedLookAngleX = 1/playerLookAngle.x;
		double tx1 = (snitchBox.minX - playerPos.x) * invertedLookAngleX;
		double tx2 = (snitchBox.maxX - playerPos.x) * invertedLookAngleX;

		double tmin = Math.min(tx1, tx2);
		double tmax = Math.max(tx1, tx2);

		double invertedLookAngleY = 1/playerLookAngle.y;
		double ty1 = (snitchBox.minY - playerPos.y) * invertedLookAngleY;
		double ty2 = (snitchBox.maxY - playerPos.y) * invertedLookAngleY;

		tmin = Math.max(tmin, Math.min(ty1, ty2));
		tmax = Math.min(tmax, Math.max(ty1, ty2));

		double invertedLookAngleZ = 1/playerLookAngle.z;
		double tz1 = (snitchBox.minZ - playerPos.z) * invertedLookAngleZ;
		double tz2 = (snitchBox.maxZ - playerPos.z) * invertedLookAngleZ;

		tmin = Math.max(tmin, Math.min(tz1, tz2));
		tmax = Math.min(tmax, Math.max(tz1, tz2));

		return tmax >= 0 && tmax >= tmin;
	}
}
