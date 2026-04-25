package com.eclipse.mixin;

import eclipse.gui.client.EclipseClientScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey(JILnet/minecraft/client/input/KeyInput;)V", at = @At("TAIL"))
    private void eclipse$openWorkspace(long window, int action, KeyInput keyInput, CallbackInfo ci) {
        if (action != 1) return;
        if (keyInput.getKeycode() != 344) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        if (client.currentScreen instanceof ChatScreen) return;

        EclipseClientScreen.toggle();
    }
}
