package com.eclipse.mixin;

import eclipse.EclipseConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    private MinecraftClient client;

    @Shadow
    private Camera camera;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void eclipse$getFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (EclipseConfig.customFov()) {
            float fov = (float) EclipseConfig.fov();
            if (EclipseConfig.lowCamera() && client.player != null && client.player.isSneaking()) {
                fov *= (float) EclipseConfig.sneakFovScale();
            }

            cir.setReturnValue(fov);
        }
    }

    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void eclipse$updateCrosshairTarget(float tickProgress, CallbackInfo ci) {
        if (!EclipseConfig.lowCamera()) return;
        if (client.player == null || client.world == null || client.interactionManager == null) return;
        if (!client.options.getPerspective().isFirstPerson()) return;

        Vec3d start = camera.getCameraPos();
        Vec3d direction = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        Vec3d end = start.add(direction.multiply(5.0));

        HitResult hit = client.world.raycast(new RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            client.player
        ));

        client.crosshairTarget = hit;
        client.targetedEntity = null;
    }
}
