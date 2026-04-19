package com.eclipse.mixin;

import eclipse.modules.ChatFix;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    private boolean eclipse$linkifying;

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void eclipse$fixLinks(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        if (eclipse$linkifying) return;

        Text fixed = ChatFix.fixLinks(message);
        if (fixed == message) return;

        ci.cancel();
        eclipse$linkifying = true;
        ((ChatHud) (Object) this).addMessage(fixed, signature, indicator);
        eclipse$linkifying = false;
    }
}
