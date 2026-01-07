package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.gui.click.elements.Button;
import whiterose.foundation.deus.gui.click.elements.Panel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.MouseEvent;

public class GalacticFire extends CheatModule {
    
    @Cfg("inRadius") private boolean inRadius;
    
    public GalacticFire() {
        super("GalacticFire", Category.MODS, PerformMode.TOGGLE);
    }
    
    private void sendFirePacket(Entity e) {
        if (e != null) {
            utils.sendPacket("GalacticraftCore", (byte) 0, 7, e.getEntityId());    
        }
    }
    
    @SubscribeEvent public void mouseEvent(MouseEvent e) {
        if (e.button == 0 && e.buttonstate) {
            if (inRadius) {
                utils.nearEntityes().filter(ent -> ent instanceof EntityLivingBase).forEach(this::sendFirePacket);
            } else {
                sendFirePacket(utils.entity());
            }
        }
    }
    
    @Override public boolean isWorking() {
        return Loader.isModLoaded("GalacticraftCore");
    }

    @Override public String moduleDesc() {
        return lang.get("Sets fire to living creatures by left mouse click");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
            new Button("InRadius", inRadius) {
                @Override public void onLeftClick() {
                    buttonValue(inRadius = !inRadius);
                }
                @Override public String elementDesc() {
                    return lang.get("By radius or sight");
                }
            }
        );
    }

}
