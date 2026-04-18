package com.eclipse.mixin;

import eclipse.EclipseConfig;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float cameraY;

    @Shadow
    private float lastCameraY;

    @Shadow
    private Entity focusedEntity;

    @Shadow
    private boolean thirdPerson;

    @Inject(method = "updateEyeHeight", at = @At("TAIL"))
    private void eclipse$updateEyeHeight(CallbackInfo ci) {
        if (!EclipseConfig.lowCamera()) return;
        if (thirdPerson || focusedEntity == null) return;

        float height = (float) (focusedEntity.isSneaking()
            ? EclipseConfig.sneakCameraHeight()
            : EclipseConfig.cameraHeight());
        cameraY = height;
        lastCameraY = height;
    }
}
