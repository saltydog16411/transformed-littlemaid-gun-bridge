package io.github.saltydog16411.transformedlittlemaidgunbridge.mixin;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelRenderer;
import net.sistr.lmrbcompat.forge.gvclib.client.LMGunBaseFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(
    targets = "net.sistr.transformintolittlemaid.client.renderer.LittleMaidPlayerRenderer$LittleMaidMultiModel",
    remap = false
)
public abstract class MixinLittleMaidMultiModel extends MultiModelRenderer {
    protected MixinLittleMaidMultiModel(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(
        method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;)V",
        at = @At("RETURN"),
        remap = false
    )
    private void tlmgb$addGunLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        this.addLayer(new LMGunBaseFeatureRenderer(this));
    }
}
