package io.github.saltydog16411.transformedlittlemaidgunbridge.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;

final class ArmRenderContext {
    static Player player;
    static MultiBufferSource buffers;
    static int packedLight;

    private ArmRenderContext() {
    }

    static void begin(Player p, MultiBufferSource b, int light) {
        player = p;
        buffers = b;
        packedLight = light;
    }

    static void clear() {
        player = null;
        buffers = null;
        packedLight = 0;
    }
}
