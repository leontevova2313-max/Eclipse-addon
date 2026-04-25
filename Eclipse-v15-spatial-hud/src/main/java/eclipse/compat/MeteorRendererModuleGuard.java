package eclipse.compat;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;

import java.lang.reflect.Field;
import java.util.Locale;

public final class MeteorRendererModuleGuard {
    private static final Class<? extends Module>[] RENDER_MODULE_TYPES = new Class[] {
        Chams.class,
        ESP.class,
        NoRender.class
    };

    private MeteorRendererModuleGuard() {
    }

    public static void refresh(Object renderer) {
        if (renderer == null) return;

        Modules modules = modules();
        if (modules == null) return;

        Class<?> type = renderer.getClass();
        while (type != null && type != Object.class) {
            refreshDeclaredFields(renderer, type, modules);
            type = type.getSuperclass();
        }
    }

    private static Modules modules() {
        try {
            return Modules.get();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void refreshDeclaredFields(Object renderer, Class<?> type, Modules modules) {
        for (Field field : type.getDeclaredFields()) {
            Class<? extends Module> moduleType = moduleTypeFor(field);
            if (moduleType == null) continue;

            try {
                field.setAccessible(true);
                if (field.get(renderer) != null) continue;

                Module module = modules.get(moduleType);
                if (module != null) field.set(renderer, module);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Rendering must not crash because another mixin renamed, reshaped, or hid a cached module field.
            }
        }
    }

    private static Class<? extends Module> moduleTypeFor(Field field) {
        Class<?> fieldType = field.getType();

        for (Class<? extends Module> moduleType : RENDER_MODULE_TYPES) {
            if (fieldType == moduleType || moduleType.isAssignableFrom(fieldType)) {
                return moduleType;
            }
        }

        // Meteor @Unique fields can be prefixed/remapped by mixin merging; exact names are brittle.
        String name = field.getName().toLowerCase(Locale.ROOT);
        if (name.contains("chams") && fieldType.isAssignableFrom(Chams.class)) return Chams.class;
        if (name.contains("esp") && fieldType.isAssignableFrom(ESP.class)) return ESP.class;
        if (name.contains("norender") && fieldType.isAssignableFrom(NoRender.class)) return NoRender.class;

        return null;
    }
}
