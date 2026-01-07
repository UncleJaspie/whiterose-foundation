package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.utils.Rand;
import whiterose.foundation.deus.utils.Reflections;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;

public class GuiReplacer extends CheatModule {
    
    public GuiReplacer() {
        super("GuiReplacer", Category.NONE, PerformMode.ENABLED_ON_START);
    }
    
    private void replaceSplash(GuiScreen gui) {
        if (gui instanceof GuiMainMenu) {
            try {
                Reflections.setPrivateValue(GuiMainMenu.class, (GuiMainMenu) gui, Rand.splash(), 3);
            } catch(Exception e) {}
        }
    }
    
    @Override public void onPostInit() {
        replaceSplash(utils.currentScreen());
    }
    
    @Override public boolean isWidgetable() {
        return false;
    }
    
    @SubscribeEvent public void guiOpen(GuiOpenEvent e) {
        replaceSplash(e.gui);
    }

}