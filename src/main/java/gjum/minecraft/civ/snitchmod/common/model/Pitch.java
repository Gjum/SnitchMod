package gjum.minecraft.civ.snitchmod.common.model;

import net.minecraft.client.player.LocalPlayer;

public class Pitch {
    private double value;

    public Pitch(double pitch) {
        this.value = pitch;
    }

    public static Pitch ofPlayer(LocalPlayer p) {
        return new Pitch(p.getXRot());
    }

    public double value() {
        return this.value;
    }
}
