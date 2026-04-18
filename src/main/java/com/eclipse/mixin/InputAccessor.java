package com.eclipse.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {
    @Accessor("movementVector")
    Vec2f eclipse$getMovementVector();

    @Accessor("movementVector")
    void eclipse$setMovementVector(Vec2f movementVector);
}
