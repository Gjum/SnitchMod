package gjum.minecraft.civ.snitchmod.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Utils {
	public static @NotNull String nonEmptyOrDefault(@Nullable String s, @NotNull String default_) {
		if (s == null) return default_;
		if (s.isEmpty()) return default_;
		return s;
	}
}
