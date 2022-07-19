package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.client.player.LocalPlayer;

public class Yaw {
    private double value;

    public Yaw(double yaw) {
        this.value = yaw;
    }

    public static Yaw ofPlayer(LocalPlayer p) {
        return new Yaw(p.getYRot());
    }

    public double value() {
        return this.value;
    }
}
