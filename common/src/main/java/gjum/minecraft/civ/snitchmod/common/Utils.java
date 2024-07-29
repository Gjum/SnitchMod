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

	/**
	 * Clamps the value to fit between min and max. If the value is less
	 * than {@code min}, then {@code min} is returned. If the value is greater
	 * than {@code max}, then {@code max} is returned. Otherwise, the original
	 * value is returned. If value is NaN, the result is also NaN.
	 * <p>
	 * Unlike the numerical comparison operators, this method considers
	 * negative zero to be strictly smaller than positive zero.
	 * E.g., {@code clamp(-0.0f, 0.0f, 1.0f)} returns 0.0f.
	 *
	 * @param value value to clamp
	 * @param min minimal allowed value
	 * @param max maximal allowed value
	 * @return a clamped value that fits into {@code min..max} interval
	 * @throws IllegalArgumentException if either of {@code min} and {@code max}
	 * arguments is NaN, or {@code min > max}, or {@code min} is +0.0f, and
	 * {@code max} is -0.0f.
	 *
	 * @since 21
	 *
	 * awoo: replace with Math.clamp in Java 21. Code copy-pasted from: https://github.com/openjdk/jdk/commit/94e7cc8587356988e713d23d1653bdd5c43fb3f1#diff-9f89bc5023b5954e0e7ea49a68dafb0b115072e5b4bd3fc33c9e116c0c19bf57R2282)
	 */
	public static float clamp(float value, float min, float max) {
		// This unusual condition allows keeping only one branch
		// on common path when min < max and neither of them is NaN.
		// If min == max, we should additionally check for +0.0/-0.0 case,
		// so we're still visiting the if statement.
		if (!(min < max)) { // min greater than, equal to, or unordered with respect to max; NaN values are unordered
			if (Float.isNaN(min)) {
				throw new IllegalArgumentException("min is NaN");
			}
			if (Float.isNaN(max)) {
				throw new IllegalArgumentException("max is NaN");
			}
			if (Float.compare(min, max) > 0) {
				throw new IllegalArgumentException(min + " > " + max);
			}
			// Fall-through if min and max are exactly equal (or min = -0.0 and max = +0.0)
			// and none of them is NaN
		}
		return Math.min(max, Math.max(value, min));
	}
}
