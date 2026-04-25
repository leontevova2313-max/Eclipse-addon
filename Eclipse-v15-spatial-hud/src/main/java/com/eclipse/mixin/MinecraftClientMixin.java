package com.eclipse.mixin;

import eclipse.Eclipse;
import eclipse.client.runtime.ClientRuntime;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Unique
    private long eclipse$tickStartedAt;

    @Inject(method = "tick", at = @At("HEAD"))
    private void eclipse$measureTickHead(CallbackInfo ci) {
        eclipse$tickStartedAt = System.nanoTime();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void eclipse$measureTickTail(CallbackInfo ci) {
        int enabled = 0;
        for (Module module : Eclipse.allModules()) {
            if (module.isActive()) enabled++;
        }
        ClientRuntime.sampleTick(System.nanoTime() - eclipse$tickStartedAt, enabled);
    }
}
