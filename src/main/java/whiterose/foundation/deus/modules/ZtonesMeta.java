package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import whiterose.foundation.deus.gui.click.elements.Button;
import whiterose.foundation.deus.gui.click.elements.Panel;

public class ZtonesMeta extends CheatModule {
    
    @Cfg("metaUp") private boolean metaUp;
    
    public ZtonesMeta() {
        super("ZtonesMeta", Category.MODS, PerformMode.SINGLE);
    }
    
    @Override public void onPerform(PerformSource src) {
        utils.sendPacket("Ztones", 0, metaUp);
    }
    
    @Override public boolean isWorking() {
        return Loader.isModLoaded("Ztones");
    }
    
    @Override public String moduleDesc() {
        return lang.get("Changes the item meta up to 15 or down to 0 (the main thing is not to overdo it)");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
            new Button("MetaUP", metaUp) {
                @Override public void onLeftClick() {
                    buttonValue(metaUp = !metaUp);
                }
                @Override public String elementDesc() {
                    return lang.get("Scroll meta up or down");
                }
            }    
        );
    }

}