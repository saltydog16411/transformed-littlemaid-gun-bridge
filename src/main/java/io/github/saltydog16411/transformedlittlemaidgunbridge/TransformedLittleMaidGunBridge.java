package io.github.saltydog16411.transformedlittlemaidgunbridge;

import io.github.saltydog16411.transformedlittlemaidgunbridge.client.ClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(TransformedLittleMaidGunBridge.MODID)
public final class TransformedLittleMaidGunBridge {
    public static final String MODID = "transformedlittlemaidgunbridge";

    public TransformedLittleMaidGunBridge() {
        // Keep every net.minecraft.client reference behind a physical-side gate.
        // This lets a dedicated server safely ignore the mod if the JAR is placed there by mistake.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientBootstrap::init);
    }
}
