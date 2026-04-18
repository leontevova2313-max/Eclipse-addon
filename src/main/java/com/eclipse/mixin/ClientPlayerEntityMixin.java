package com.eclipse.mixin;

import eclipse.modules.EclipseNoSlow;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "applyMovementSpeedFactors", at = @At("RETURN"), cancellable = true)
    private void eclipse$applyNoSlowMultiplier(Vec2f movement, CallbackInfoReturnable<Vec2f> cir) {
        cir.setReturnValue(EclipseNoSlow.applyMultiplier(cir.getReturnValue()));
    }
}
