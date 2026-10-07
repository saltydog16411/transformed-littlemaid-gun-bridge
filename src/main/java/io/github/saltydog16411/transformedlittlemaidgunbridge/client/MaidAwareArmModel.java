package io.github.saltydog16411.transformedlittlemaidgunbridge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelRenderLayer;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.maidmodel.ModelRenderer;
import net.sistr.littlemaidmodelloader.multimodel.IMultiModel;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMMatrixStack;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMVertexConsumer;
import net.sistr.transformintolittlemaid.util.LittleMaidTransformable;
import objmodel.IModelCustom;
import org.slf4j.Logger;

/**
 * IModelCustom proxy installed into GVCLib RenderGunEvent.arm_model.
 *
 * <p>GVCLib naturally calls this object when it draws rightarm/leftarm, so its own
 * ADS/recoil/reload PoseStack is preserved. The working render transforms are intentionally
 * kept equivalent to the verified internal 1.0.15 baseline.</p>
 */
final class MaidAwareArmModel implements IModelCustom {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float MODEL_SCALE = 0.0625F;
    private static final Set<String> WARNED =
        Collections.synchronizedSet(new HashSet<>());

    private final IModelCustom delegate;

    MaidAwareArmModel(IModelCustom delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public void renderAll(VertexConsumer buffer, PoseStack poseStack) {
        delegate.renderAll(buffer, poseStack);
    }

    @Override
    public void renderOnly(VertexConsumer buffer, PoseStack poseStack, String... groupNames) {
        delegate.renderOnly(buffer, poseStack, groupNames);
    }

    @Override
    public void renderPart(VertexConsumer buffer, PoseStack poseStack, String partName) {
        if (intercept(poseStack, partName)) {
            return;
        }
        delegate.renderPart(buffer, poseStack, partName);
    }

    @Override
    public void renderPartColor(
        VertexConsumer buffer,
        PoseStack poseStack,
        String partName,
        int red,
        int green,
        int blue,
        int light
    ) {
        if (intercept(poseStack, partName)) {
            return;
        }
        delegate.renderPartColor(buffer, poseStack, partName, red, green, blue, light);
    }

    @Override
    public void renderAllExcept(VertexConsumer buffer, PoseStack poseStack, String... excludedGroupNames) {
        delegate.renderAllExcept(buffer, poseStack, excludedGroupNames);
    }

    private static boolean intercept(PoseStack poseStack, String partName) {
        if (!"rightarm".equals(partName) && !"leftarm".equals(partName)) {
            return false;
        }

        Player player = ArmRenderContext.player;
        MultiBufferSource buffers = ArmRenderContext.buffers;
        if (player == null || buffers == null) {
            return false;
        }

        boolean transformed = player instanceof LittleMaidTransformable transformable
            && transformable.isTransformedLittleMaid_TLM();
        if (!transformed) {
            return false;
        }

        boolean left = "leftarm".equals(partName);
        renderMaidArm(player, poseStack, buffers, ArmRenderContext.packedLight, left);

        // When transformed, never draw GVCLib's Steve/Alex arm as a fallback.
        return true;
    }

    private static void renderMaidArm(
        Player player,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        boolean left
    ) {
        String side = left ? "LEFT" : "RIGHT";
        String stage = "entry";
        try {
            if (!(player instanceof IHasMultiModel multiModel)) {
                warnOnce("no-multimodel", "Cannot render maid gun arm: player has no IHasMultiModel.", null);
                return;
            }

            stage = "model";
            IMultiModel model = multiModel
                .getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.BODY)
                .orElse(null);
            if (model == null) {
                model = multiModel
                    .getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                    .orElse(null);
            }
            if (model == null) {
                warnOnce("model-null", "Cannot render maid gun arm: selected maid model is unavailable.", null);
                return;
            }

            stage = "texture";
            ResourceLocation selectedTexture = multiModel
                .getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(null);
            if (selectedTexture == null) {
                selectedTexture = multiModel
                    .getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.BODY, false)
                    .orElse(null);
            }
            if (selectedTexture == null) {
                warnOnce("texture-null", "Cannot render maid gun arm: selected maid texture is unavailable.", null);
                return;
            }
            ResourceLocation armTexture = resolveLittleForcesBaseTexture(multiModel, selectedTexture);

            stage = "arm-field";
            ModelRenderer arm = getBipedArm(model, left);
            if (arm == null) {
                warnOnce(
                    "arm-field:" + model.getClass().getName() + ':' + side,
                    "Cannot render maid gun arm: model " + model.getClass().getName()
                        + " does not expose " + (left ? "bipedLeftArm" : "bipedRightArm") + '.',
                    null
                );
                return;
            }

            stage = "buffer";
            VertexConsumer maidConsumer = buffers.getBuffer(MultiModelRenderLayer.getDefault(armTexture));

            poseStack.pushPose();
            List<ModelRenderer> parts = new ArrayList<>();
            List<float[]> rotations = new ArrayList<>();
            try {
                stage = "matrix";
                MMMatrixStack mmStack = new MMMatrixStack(poseStack);

                // Verified internal 1.0.15 mapping from LMML maid arms into GVCLib arm space.
                if (!left) {
                    mmStack.translate(0.75D, 0.075D, -1.85625D);
                    mmStack.rotateXDeg(90.0F);
                    mmStack.scale(3.0F, 5.0F, 3.0F);
                } else {
                    mmStack.translate(1.55135105D, -0.08896428D, 1.82131433D);
                    mmStack.rotateYDeg(-60.0F);
                    mmStack.rotateXDeg(75.0F);
                    mmStack.scale(3.0F, 5.0F, 3.0F);
                }

                ModelRenderer.setParam(
                    mmStack,
                    new MMVertexConsumer(maidConsumer),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
                );

                stage = "render";
                saveAndStraighten(arm, parts, rotations);
                arm.render(MODEL_SCALE);
            } finally {
                restoreRotations(parts, rotations);
                poseStack.popPose();
            }
        } catch (Throwable t) {
            warnOnce(
                "exception:" + stage + ':' + side + ':' + t.getClass().getName(),
                "Maid gun arm rendering failed at stage " + stage + " (side=" + side + ").",
                t
            );
        }
    }

    private static ModelRenderer getBipedArm(IMultiModel model, boolean left) {
        String fieldName = left ? "bipedLeftArm" : "bipedRightArm";
        try {
            Field field = model.getClass().getField(fieldName);
            Object value = field.get(model);
            return value instanceof ModelRenderer renderer ? renderer : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static ResourceLocation resolveLittleForcesBaseTexture(
        IHasMultiModel multiModel,
        ResourceLocation selectedTexture
    ) {
        String id = selectedTexture.toString();
        int colon = id.indexOf(':');
        if (colon <= 0 || colon >= id.length() - 1) {
            return selectedTexture;
        }

        String namespace = id.substring(0, colon);
        String path = id.substring(colon + 1);
        String lower = path.toLowerCase();
        if (!lower.contains("/littleforces/")) {
            return selectedTexture;
        }

        int marker = lower.indexOf("_lfms/");
        String family = "lfms";
        int markerLength = "_lfms/".length();
        if (marker < 0) {
            marker = lower.indexOf("_lfs/");
            family = "lfs";
            markerLength = "_lfs/".length();
        }
        if (marker < 0) {
            return selectedTexture;
        }

        try {
            int color = multiModel.getColorMM().getIndex();
            if (color < 0 || color > 15) {
                return selectedTexture;
            }
            String baseDir = path.substring(0, marker + markerLength);
            String basePath = baseDir + "littlemaid_" + family + '_'
                + Integer.toHexString(color) + ".png";
            return new ResourceLocation(namespace, basePath);
        } catch (Throwable ignored) {
            return selectedTexture;
        }
    }

    private static void saveAndStraighten(
        ModelRenderer part,
        List<ModelRenderer> parts,
        List<float[]> rotations
    ) {
        parts.add(part);
        rotations.add(new float[] {
            part.rotateAngleX,
            part.rotateAngleY,
            part.rotateAngleZ
        });
        part.rotateAngleX = 0.0F;
        part.rotateAngleY = 0.0F;
        part.rotateAngleZ = 0.0F;
        if (part.childModels != null) {
            for (ModelRenderer child : part.childModels) {
                saveAndStraighten(child, parts, rotations);
            }
        }
    }

    private static void restoreRotations(
        List<ModelRenderer> parts,
        List<float[]> rotations
    ) {
        int size = Math.min(parts.size(), rotations.size());
        for (int i = 0; i < size; i++) {
            ModelRenderer part = parts.get(i);
            float[] r = rotations.get(i);
            part.rotateAngleX = r[0];
            part.rotateAngleY = r[1];
            part.rotateAngleZ = r[2];
        }
    }

    private static void warnOnce(String key, String message, Throwable throwable) {
        if (!WARNED.add(key)) {
            return;
        }
        if (throwable == null) {
            LOGGER.warn(message);
        } else {
            LOGGER.warn(message, throwable);
        }
    }
}
