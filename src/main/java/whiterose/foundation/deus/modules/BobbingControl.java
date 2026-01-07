package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import net.minecraftforge.client.event.RenderWorldLastEvent;

public class BobbingControl extends CheatModule {

    private XRay xray;
    private Esp esp;

    public BobbingControl() {
        super("BobbingControl", Category.NONE, PerformMode.ENABLED_ON_START);
    }
    
    @Override public boolean isWidgetable() {
        return false;
    }
    
    @Override public void onPostInit() {
        xray = (XRay) moduleHandler().getModuleByClass(XRay.class);
        esp = (Esp) moduleHandler().getModuleByClass(Esp.class);
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void renderWorld(RenderWorldLastEvent e) {
        xray.linesCheck = false;
        esp.linesCheck = false;
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST) public void renderWorldL(RenderWorldLastEvent e) {
        utils.mc().gameSettings.viewBobbing = (!xray.linesCheck || !xray.bindLines) && (!esp.linesCheck || !esp.bindLines);
    }

}
