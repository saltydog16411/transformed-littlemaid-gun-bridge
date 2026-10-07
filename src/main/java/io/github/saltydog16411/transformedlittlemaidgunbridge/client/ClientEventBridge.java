package io.github.saltydog16411.transformedlittlemaidgunbridge.client;

import gvclib.client.event.RenderGunEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import objmodel.IModelCustom;

/**
 * Forge-event based first-person hook.
 *
 * <p>The bridge replaces GVCLib's lazily loaded arm model with a proxy. GVCLib still performs
 * its own ADS, recoil and reload PoseStack transforms, and the proxy only replaces the final
 * rightarm/leftarm geometry when the local player is transformed into a LittleMaid.</p>
 */
public final class ClientEventBridge {

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void beforeRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        ArmRenderContext.begin(player, event.getMultiBufferSource(), event.getPackedLight());
        installArmBridge();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void afterRenderHand(RenderHandEvent event) {
        try {
            // GVCLib initializes arm_model lazily inside render_arm. If it was null at HIGHEST
            // priority, wrap the freshly created model now for the next frame.
            installArmBridge();
        } finally {
            // Always clear even when another mod cancels RenderHandEvent.
            ArmRenderContext.clear();
        }
    }

    private static void installArmBridge() {
        IModelCustom current = RenderGunEvent.arm_model;
        if (current == null || current instanceof MaidAwareArmModel) {
            return;
        }
        RenderGunEvent.arm_model = new MaidAwareArmModel(current);
    }
}
