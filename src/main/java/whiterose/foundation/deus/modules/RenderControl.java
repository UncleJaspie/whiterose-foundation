package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.gui.click.elements.Button;
import whiterose.foundation.deus.gui.click.elements.Panel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;

public class RenderControl extends CheatModule {
    
    @Cfg("living") private boolean living;
    
    public RenderControl() {
        super("RenderControl", Category.WORLD, PerformMode.TOGGLE);
        living = true;
    }

    @SubscribeEvent public void worldRender(RenderLivingEvent.Pre e) {
        if (!living && !(e.entity instanceof EntityPlayer)) {
            e.setCanceled(true);
        }
    }
    
    @Override public String moduleDesc() {
        return lang.get("Enabling rendering of objects in the world");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
            new Button("LivingBase", living) {
                @Override public void onLeftClick() {
                    buttonValue(living = !living);
                }
                @Override public String elementDesc() {
                    return lang.get("Render living base");
                }
            }
        );
    }

}
