package com.eclipse.mixin;

import eclipse.skins.SkinCustomizationManager;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void eclipse$overrideLocalSkin(CallbackInfoReturnable<SkinTextures> cir) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;
        SkinTextures textures = SkinCustomizationManager.overrideSkin(self.getProfile());
        if (textures != null) cir.setReturnValue(textures);
    }
}
