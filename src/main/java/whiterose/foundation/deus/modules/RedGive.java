package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;

public class RedGive extends CheatModule {
    
    public RedGive() {
        super("RedGive", Category.MODS, PerformMode.SINGLE);
    }
    
    @Override public void onPerform(PerformSource type) {
        utils.sendPacket("PR|Transp", (byte) 4, (byte) utils.player().inventory.currentItem, giveSelector().givedItem());
    }
    
    @Override public boolean isWorking() {
        return Loader.isModLoaded("ProjRed|Transportation");
    }
    
    @Override public String moduleDesc() {
        return lang.get("Issuing an item to the active slot (will replace the current item)");
    }

}
