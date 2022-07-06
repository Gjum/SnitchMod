package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.client.player.LocalPlayer;

public class Direction {
    private Yaw yaw;
    private Pitch pitch;

    public Direction(Yaw yaw, Pitch pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static Direction ofPlayer(LocalPlayer p) {
        return new Direction(Yaw.ofPlayer(p), Pitch.ofPlayer(p));
    }

    public Yaw yaw() {
        return this.yaw;
    }

    public Pitch pitch() {
        return this.pitch;
    }
}
