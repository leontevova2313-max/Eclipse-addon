package eclipse.modules.visuals;

import eclipse.Eclipse;
import eclipse.EclipseConfig;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

public class EclipseCrosshair extends Module {
    private final SettingGroup sgShape = settings.getDefaultGroup();
    private final SettingGroup sgStyle = settings.createGroup("Style");
    private final SettingGroup sgMotion = settings.createGroup("Motion");
    private final SettingGroup sgRules = settings.createGroup("Rules");

    private final Setting<EclipseConfig.CrosshairStyle> style = sgShape.add(new EnumSetting.Builder<EclipseConfig.CrosshairStyle>()
        .name("style")
        .description("Crosshair shape.")
        .defaultValue(EclipseConfig.CrosshairStyle.Classic)
        .onChanged(EclipseConfig::crosshairStyle)
        .build()
    );

    private final Setting<Boolean> dot = sgShape.add(new BoolSetting.Builder()
        .name("dot")
        .description("Draw center dot.")
        .defaultValue(true)
        .visible(() -> style.get() != EclipseConfig.CrosshairStyle.Dot)
        .onChanged(EclipseConfig::crosshairDot)
        .build()
    );

    private final Setting<Integer> gap = sgShape.add(new IntSetting.Builder()
        .name("gap")
        .description("Center gap.")
        .defaultValue(4)
        .range(0, 24)
        .sliderRange(0, 16)
        .visible(this::armsVisible)
        .onChanged(EclipseConfig::crosshairGap)
        .build()
    );

    private final Setting<Integer> length = sgShape.add(new IntSetting.Builder()
        .name("length")
        .description("Arm length.")
        .defaultValue(6)
        .range(1, 32)
        .sliderRange(2, 20)
        .visible(this::armsVisible)
        .onChanged(EclipseConfig::crosshairLength)
        .build()
    );

    private final Setting<Integer> thickness = sgShape.add(new IntSetting.Builder()
        .name("thickness")
        .description("Line thickness.")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 5)
        .onChanged(EclipseConfig::crosshairThickness)
        .build()
    );

    private final Setting<Integer> dotSize = sgShape.add(new IntSetting.Builder()
        .name("dot-size")
        .description("Dot size.")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 5)
        .visible(this::dotVisible)
        .onChanged(EclipseConfig::crosshairDotSize)
        .build()
    );

    private final Setting<Boolean> rainbow = sgStyle.add(new BoolSetting.Builder()
        .name("rainbow")
        .description("Hue cycle.")
        .defaultValue(false)
        .onChanged(EclipseConfig::crosshairRainbow)
        .build()
    );

    private final Setting<SettingColor> color = sgStyle.add(new ColorSetting.Builder()
        .name("color")
        .description("Base color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(this::staticColorVisible)
        .onChanged(value -> EclipseConfig.crosshairColor(value.getPacked()))
        .build()
    );

    private final Setting<Boolean> outline = sgStyle.add(new BoolSetting.Builder()
        .name("outline")
        .description("Visibility outline.")
        .defaultValue(true)
        .onChanged(EclipseConfig::crosshairOutline)
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgStyle.add(new ColorSetting.Builder()
        .name("outline-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(0, 0, 0, 170))
        .visible(outline::get)
        .onChanged(value -> EclipseConfig.crosshairOutlineColor(value.getPacked()))
        .build()
    );

    private final Setting<SettingColor> gradientStart = sgStyle.add(new ColorSetting.Builder()
        .name("gradient-start")
        .description("Start color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(this::staticColorVisible)
        .onChanged(value -> EclipseConfig.crosshairGradientStart(value.getPacked()))
        .build()
    );

    private final Setting<SettingColor> gradientEnd = sgStyle.add(new ColorSetting.Builder()
        .name("gradient-end")
        .description("End color.")
        .defaultValue(new SettingColor(79, 216, 255, 255))
        .visible(this::gradientEndVisible)
        .onChanged(value -> EclipseConfig.crosshairGradientEnd(value.getPacked()))
        .build()
    );

    private final Setting<Integer> opacity = sgStyle.add(new IntSetting.Builder()
        .name("opacity")
        .description("Crosshair alpha.")
        .defaultValue(255)
        .range(30, 255)
        .sliderRange(90, 255)
        .onChanged(EclipseConfig::crosshairOpacity)
        .build()
    );

    private final Setting<Boolean> shadow = sgStyle.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Soft shadow.")
        .defaultValue(false)
        .onChanged(EclipseConfig::crosshairShadow)
        .build()
    );

    private final Setting<Integer> shadowStrength = sgStyle.add(new IntSetting.Builder()
        .name("shadow-strength")
        .description("Shadow alpha.")
        .defaultValue(120)
        .range(20, 220)
        .sliderRange(60, 180)
        .visible(shadow::get)
        .onChanged(EclipseConfig::crosshairShadowStrength)
        .build()
    );

    private final Setting<Boolean> dynamicGap = sgMotion.add(new BoolSetting.Builder()
        .name("dynamic-gap")
        .description("Expand while moving.")
        .defaultValue(true)
        .visible(this::armsVisible)
        .onChanged(EclipseConfig::crosshairDynamicGap)
        .build()
    );

    private final Setting<Double> movementExpansion = sgMotion.add(new DoubleSetting.Builder()
        .name("movement-expansion")
        .description("Movement gap scale.")
        .defaultValue(1.0)
        .range(0.0, 3.0)
        .sliderRange(0.0, 2.0)
        .decimalPlaces(2)
        .visible(() -> armsVisible() && dynamicGap.get())
        .onChanged(EclipseConfig::crosshairMovementExpansion)
        .build()
    );

    private final Setting<Boolean> recoil = sgMotion.add(new BoolSetting.Builder()
        .name("recoil")
        .description("Expand on attack/use.")
        .defaultValue(true)
        .visible(this::armsVisible)
        .onChanged(EclipseConfig::crosshairRecoil)
        .build()
    );

    private final Setting<Boolean> hideWhenHoldingItem = sgRules.add(new BoolSetting.Builder()
        .name("hide-holding")
        .description("Hide while holding item.")
        .defaultValue(false)
        .onChanged(EclipseConfig::hideCrosshairWhenHoldingItem)
        .build()
    );

    private final Setting<Boolean> combatOnly = sgRules.add(new BoolSetting.Builder()
        .name("combat-only")
        .description("Only with weapons.")
        .defaultValue(false)
        .onChanged(EclipseConfig::crosshairCombatOnly)
        .build()
    );

    private long recoilUntil;

    public EclipseCrosshair() {
        super(Eclipse.VISUALS, "eclipse-crosshair", "Configurable Eclipse HUD crosshair.");
    }

    @Override
    public void onActivate() {
        syncConfig();
    }

    @Override
    public void onDeactivate() {
        EclipseConfig.crosshair(false);
    }

    private void syncConfig() {
        EclipseConfig.crosshair(true);
        EclipseConfig.crosshairStyle(style.get());
        EclipseConfig.crosshairOutline(outline.get());
        EclipseConfig.crosshairDot(dot.get());
        EclipseConfig.crosshairDynamicGap(dynamicGap.get());
        EclipseConfig.crosshairGap(gap.get());
        EclipseConfig.crosshairLength(length.get());
        EclipseConfig.crosshairThickness(thickness.get());
        EclipseConfig.crosshairDotSize(dotSize.get());
        EclipseConfig.crosshairColor(color.get().getPacked());
        EclipseConfig.crosshairOutlineColor(outlineColor.get().getPacked());
        EclipseConfig.crosshairGradientStart(gradientStart.get().getPacked());
        EclipseConfig.crosshairGradientEnd(gradientEnd.get().getPacked());
        EclipseConfig.crosshairRainbow(rainbow.get());
        EclipseConfig.crosshairOpacity(opacity.get());
        EclipseConfig.crosshairShadow(shadow.get());
        EclipseConfig.crosshairShadowStrength(shadowStrength.get());
        EclipseConfig.crosshairMovementExpansion(movementExpansion.get());
        EclipseConfig.crosshairRecoil(recoil.get());
        EclipseConfig.hideCrosshairWhenHoldingItem(hideWhenHoldingItem.get());
        EclipseConfig.crosshairCombatOnly(combatOnly.get());
    }

    private boolean armsVisible() {
        return style.get() != EclipseConfig.CrosshairStyle.Dot;
    }

    private boolean dotVisible() {
        return dot.get() || style.get() == EclipseConfig.CrosshairStyle.Dot;
    }

    private boolean staticColorVisible() {
        return !rainbow.get();
    }

    private boolean gradientEndVisible() {
        return armsVisible() && !rainbow.get();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!shouldRenderCrosshair()) return;

        if (EclipseConfig.crosshairRecoil() && (mc.options.attackKey.isPressed() || mc.options.useKey.isPressed())) {
            recoilUntil = System.currentTimeMillis() + 110L;
        }

        int x = event.drawContext.getScaledWindowWidth() / 2;
        int y = event.drawContext.getScaledWindowHeight() / 2;
        int gap = EclipseConfig.crosshairGap() + dynamicExpansion();
        int length = EclipseConfig.crosshairLength();
        int thickness = EclipseConfig.crosshairThickness();
        int primary = primaryColor();
        int secondary = EclipseConfig.crosshairRainbow() ? primary : EclipseConfig.crosshairGradientEnd();

        if (EclipseConfig.crosshairShadow()) {
            drawStyledCrosshair(event, x + 1, y + 1, gap, length, thickness, shadowColor(), shadowColor(), false);
        }

        if (EclipseConfig.crosshairOutline()) {
            drawStyledCrosshair(event, x, y, gap, length, thickness + 2, EclipseConfig.crosshairOutlineColor(), EclipseConfig.crosshairOutlineColor(), true);
        }

        drawStyledCrosshair(event, x, y, gap, length, thickness, primary, secondary, false);

        if (EclipseConfig.crosshairDot()) {
            int dot = EclipseConfig.crosshairDotSize();
            if (EclipseConfig.crosshairOutline()) fillCentered(event, x, y, dot + 2, dot + 2, EclipseConfig.crosshairOutlineColor());
            fillCentered(event, x, y, dot, dot, primary);
        }
    }

    private boolean shouldRenderCrosshair() {
        if (!EclipseConfig.crosshair() || mc.options.hudHidden || mc.player == null) return false;
        if (EclipseConfig.hideCrosshairWhenHoldingItem() && !mc.player.getMainHandStack().isEmpty()) return false;
        return !EclipseConfig.crosshairCombatOnly() || isHoldingCombatItem();
    }

    private boolean isHoldingCombatItem() {
        ItemStack stack = mc.player.getMainHandStack();
        return stack.isIn(ItemTags.SWORDS)
            || stack.isIn(ItemTags.AXES)
            || stack.isOf(Items.BOW)
            || stack.isOf(Items.CROSSBOW)
            || stack.isOf(Items.TRIDENT);
    }

    private int dynamicExpansion() {
        int expansion = 0;
        if (EclipseConfig.crosshairDynamicGap() && mc.player != null) {
            double speed = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ);
            expansion += Math.min(12, (int) Math.round(speed * 80.0 * EclipseConfig.crosshairMovementExpansion()));
        }

        long now = System.currentTimeMillis();
        if (EclipseConfig.crosshairRecoil() && recoilUntil > now) {
            expansion += (int) Math.ceil((recoilUntil - now) / 28.0);
        }

        return expansion;
    }

    private int primaryColor() {
        if (!EclipseConfig.crosshairRainbow()) return EclipseConfig.crosshairGradientStart();
        float hue = (System.currentTimeMillis() % 3000L) / 3000.0F;
        return EclipseConfig.withAlpha(java.awt.Color.HSBtoRGB(hue, 0.72F, 1.0F), EclipseConfig.crosshairOpacity());
    }

    private int shadowColor() {
        return EclipseConfig.withAlpha(0x000000, EclipseConfig.crosshairShadowStrength());
    }

    private void drawStyledCrosshair(Render2DEvent event, int x, int y, int gap, int length, int thickness, int startColor, int endColor, boolean outline) {
        switch (EclipseConfig.crosshairStyle()) {
            case Dot -> {
            }
            case Cross -> drawCrosshair(event, x, y, Math.max(0, gap / 2), length, thickness, startColor, endColor, outline, false);
            case TShape -> drawCrosshair(event, x, y, gap, length, thickness, startColor, endColor, outline, true);
            case Circle -> drawCircle(event, x, y, Math.max(3, gap + length / 2), thickness, startColor);
            case Classic -> drawCrosshair(event, x, y, gap, length, thickness, startColor, endColor, outline, false);
        }
    }

    private void drawCrosshair(Render2DEvent event, int x, int y, int gap, int length, int thickness, int startColor, int endColor, boolean outline, boolean tShape) {
        int extra = outline ? 1 : 0;
        int half = thickness / 2;

        fillGradient(event, x - gap - length - extra, y - half, x - gap + extra, y - half + thickness, endColor, startColor);
        fillGradient(event, x + gap + 1 - extra, y - half, x + gap + 1 + length + extra, y - half + thickness, startColor, endColor);
        fillGradient(event, x - half, y - gap - length - extra, x - half + thickness, y - gap + extra, endColor, startColor);
        if (!tShape) {
            fillGradient(event, x - half, y + gap + 1 - extra, x - half + thickness, y + gap + 1 + length + extra, startColor, endColor);
        }
    }

    private void drawCircle(Render2DEvent event, int x, int y, int radius, int thickness, int color) {
        for (int i = 0; i < thickness; i++) {
            int r = radius + i;
            fill(event, x - r, y - 1, x - r + 1, y + 1, color);
            fill(event, x + r, y - 1, x + r + 1, y + 1, color);
            fill(event, x - 1, y - r, x + 1, y - r + 1, color);
            fill(event, x - 1, y + r, x + 1, y + r + 1, color);
            fill(event, x - r * 7 / 10, y - r * 7 / 10, x - r * 7 / 10 + 1, y - r * 7 / 10 + 1, color);
            fill(event, x + r * 7 / 10, y - r * 7 / 10, x + r * 7 / 10 + 1, y - r * 7 / 10 + 1, color);
            fill(event, x - r * 7 / 10, y + r * 7 / 10, x - r * 7 / 10 + 1, y + r * 7 / 10 + 1, color);
            fill(event, x + r * 7 / 10, y + r * 7 / 10, x + r * 7 / 10 + 1, y + r * 7 / 10 + 1, color);
        }
    }

    private void fillCentered(Render2DEvent event, int centerX, int centerY, int width, int height, int color) {
        int x1 = centerX - width / 2;
        int y1 = centerY - height / 2;
        fill(event, x1, y1, x1 + width, y1 + height, color);
    }

    private void fillGradient(Render2DEvent event, int x1, int y1, int x2, int y2, int colorA, int colorB) {
        event.drawContext.fillGradient(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), colorA, colorB);
    }

    private void fill(Render2DEvent event, int x1, int y1, int x2, int y2, int color) {
        event.drawContext.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), color);
    }
}
