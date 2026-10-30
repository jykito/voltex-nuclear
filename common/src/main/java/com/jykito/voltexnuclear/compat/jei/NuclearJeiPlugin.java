package com.jykito.voltexnuclear.compat.jei;

import com.jykito.voltexnuclear.VoltexNuclear;
import com.jykito.voltexnuclear.client.CascadeScreen;
import com.jykito.voltexnuclear.client.ElectrolyzerScreen;
import com.jykito.voltexnuclear.client.PelletPressScreen;
import com.jykito.voltexnuclear.client.RadiochemScreen;
import com.jykito.voltexnuclear.client.StarForgeScreen;
import com.jykito.voltexnuclear.content.NuclearItems;
import com.jykito.voltexnuclear.core.cascade.Cascade.Run;
import com.jykito.voltexnuclear.core.reactor.Scheme;
import com.jykito.voltexnuclear.press.PressRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The add-on's recipes in JEI: the centrifuge cascade, the radiochemical plant, the heavy water column, the pellet press, the electrolyzer, the reactor schemes (with the "+" of an open
 * reactor window), and since stage 12 the star forge, the fusion torus and the reactor's irradiation channel. JEI finds this class by its annotation and
 * loads it only where JEI is installed; nothing else in the add-on refers to it. The same class serves JEI on both
 * versions: everything it calls is spelled the same in JEI 15 (1.20.1) and 19 (1.21.1).
 */
@JeiPlugin
public final class NuclearJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return VoltexNuclear.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CascadeCategory(gui), new RadiochemCategory(gui), new ColumnCategory(gui), new PressCategory(gui),
                new ElectrolyzerCategory(gui), new SchemeCategory(gui), new StarForgeCategory(gui), new TorusCategory(gui), new ChannelCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CascadeCategory.TYPE, Run.ALL);
        registration.addRecipes(RadiochemCategory.TYPE, RadiochemCategory.all());
        registration.addRecipes(ColumnCategory.TYPE, ColumnCategory.all());
        registration.addRecipes(PressCategory.TYPE, PressRecipes.all());
        registration.addRecipes(ElectrolyzerCategory.TYPE, ElectrolyzerCategory.all());
        registration.addRecipes(SchemeCategory.TYPE, Scheme.ALL);
        registration.addRecipes(StarForgeCategory.TYPE, List.of(new StarForgeCategory.Melt()));
        registration.addRecipes(TorusCategory.TYPE, List.of(new TorusCategory.Burn()));
        registration.addRecipes(ChannelCategory.TYPE, ChannelCategory.all());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(NuclearItems.CASCADE_CORE.get(), CascadeCategory.TYPE);
        registration.addRecipeCatalyst(NuclearItems.RADIOCHEM_CORE.get(), RadiochemCategory.TYPE);
        registration.addRecipeCatalyst(NuclearItems.COLUMN_CORE.get(), ColumnCategory.TYPE);
        registration.addRecipeCatalyst(NuclearItems.PELLET_PRESS.get(), PressCategory.TYPE);
        registration.addRecipeCatalyst(NuclearItems.ELECTROLYZER.get(), ElectrolyzerCategory.TYPE);
        registration.addRecipeCatalyst(NuclearItems.REACTOR_CORE.get(), SchemeCategory.TYPE, ChannelCategory.TYPE);
        registration.addRecipeCatalyst(NuclearItems.STAR_FORGE_CORE.get(), StarForgeCategory.TYPE);
        registration.addRecipeCatalyst(NuclearItems.TORUS_CORE.get(), TorusCategory.TYPE);
    }

    /** As on the Voltex machine windows: a click on the progress opens the machine's recipes. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(PelletPressScreen.class, PelletPressScreen.SIGN_X, PelletPressScreen.SIGN_Y,
                PelletPressScreen.SIGN_W, PelletPressScreen.SIGN_H, PressCategory.TYPE);
        registration.addRecipeClickArea(CascadeScreen.class, CascadeScreen.PROG_X, CascadeScreen.PROG_Y,
                CascadeScreen.PROG_W, CascadeScreen.PROG_H, CascadeCategory.TYPE);
        registration.addRecipeClickArea(RadiochemScreen.class, RadiochemScreen.PROG_X, RadiochemScreen.PROG_Y,
                RadiochemScreen.PROG_W, RadiochemScreen.PROG_H, RadiochemCategory.TYPE);
        registration.addRecipeClickArea(ElectrolyzerScreen.class, ElectrolyzerScreen.CLICK_X, ElectrolyzerScreen.CLICK_Y,
                ElectrolyzerScreen.CLICK_W, ElectrolyzerScreen.CLICK_H, ElectrolyzerCategory.TYPE);
        registration.addRecipeClickArea(StarForgeScreen.class, StarForgeScreen.PROG_X, StarForgeScreen.PROG_Y,
                StarForgeScreen.PROG_W, StarForgeScreen.PROG_H, StarForgeCategory.TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ReactorTransferHandler(), SchemeCategory.TYPE);
    }
}
