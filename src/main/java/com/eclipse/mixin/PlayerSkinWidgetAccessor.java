package com.eclipse.mixin;

import net.minecraft.client.gui.widget.PlayerSkinWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerSkinWidget.class)
public interface PlayerSkinWidgetAccessor {
    @Accessor("xRotation")
    void eclipse$setXRotation(float value);

    @Accessor("yRotation")
    void eclipse$setYRotation(float value);
}
