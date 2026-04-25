package com.eclipse.mixin;

import eclipse.modules.movement.PearlPhase;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
    private void eclipse$bypassBlockCollisions(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
        if (PearlPhase.shouldBypassBlockCollision((Entity) (Object) this)) {
            cir.setReturnValue(movement);
        }
    }

    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void eclipse$skipInsideWallCheck(CallbackInfoReturnable<Boolean> cir) {
        if (PearlPhase.shouldBypassBlockCollision((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void eclipse$skipPushOutOfBlocks(double x, double y, double z, CallbackInfo ci) {
        if (PearlPhase.shouldBypassBlockCollision((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}
