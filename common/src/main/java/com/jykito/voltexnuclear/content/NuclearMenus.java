package com.jykito.voltexnuclear.content;

import com.jykito.voltexnuclear.cascade.CascadeMenu;
import com.jykito.voltexnuclear.electrolyzer.ElectrolyzerMenu;
import com.jykito.voltexnuclear.platform.Platform;
import com.jykito.voltexnuclear.press.PelletPressMenu;
import com.jykito.voltexnuclear.column.ColumnMenu;
import com.jykito.voltexnuclear.fusion.TorusMenu;
import com.jykito.voltexnuclear.radiochem.RadiochemMenu;
import com.jykito.voltexnuclear.reactor.ReactorMenu;
import com.jykito.voltexnuclear.starforge.StarForgeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

/**
 * Menu types of the add-on. They carry no extra data on opening (the window gets its numbers through the menu's data
 * slots), so the plain vanilla menu type serves both loaders.
 */
public final class NuclearMenus {

    private NuclearMenus() {}

    public static Supplier<MenuType<CascadeMenu>> CASCADE;
    public static Supplier<MenuType<PelletPressMenu>> PELLET_PRESS;
    public static Supplier<MenuType<ElectrolyzerMenu>> ELECTROLYZER;
    public static Supplier<MenuType<ReactorMenu>> REACTOR;
    public static Supplier<MenuType<RadiochemMenu>> RADIOCHEM;
    public static Supplier<MenuType<ColumnMenu>> COLUMN;
    public static Supplier<MenuType<TorusMenu>> TORUS;
    public static Supplier<MenuType<StarForgeMenu>> STAR_FORGE;

    static void register(Platform p) {
        CASCADE = p.register(Registries.MENU, "cascade", () -> new MenuType<>(CascadeMenu::new, FeatureFlags.DEFAULT_FLAGS));
        PELLET_PRESS = p.register(Registries.MENU, "pellet_press", () -> new MenuType<>(PelletPressMenu::new, FeatureFlags.DEFAULT_FLAGS));
        ELECTROLYZER = p.register(Registries.MENU, "electrolyzer", () -> new MenuType<>(ElectrolyzerMenu::new, FeatureFlags.DEFAULT_FLAGS));
        REACTOR = p.register(Registries.MENU, "reactor", () -> new MenuType<>(ReactorMenu::new, FeatureFlags.DEFAULT_FLAGS));
        RADIOCHEM = p.register(Registries.MENU, "radiochem", () -> new MenuType<>(RadiochemMenu::new, FeatureFlags.DEFAULT_FLAGS));
        COLUMN = p.register(Registries.MENU, "heavy_water_column", () -> new MenuType<>(ColumnMenu::new, FeatureFlags.DEFAULT_FLAGS));
        TORUS = p.register(Registries.MENU, "torus", () -> new MenuType<>(TorusMenu::new, FeatureFlags.DEFAULT_FLAGS));
        STAR_FORGE = p.register(Registries.MENU, "star_forge", () -> new MenuType<>(StarForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    }
}
