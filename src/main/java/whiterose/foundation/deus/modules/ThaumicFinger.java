package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;

public class ThaumicFinger extends CheatModule {
    
    public ThaumicFinger() {
        super("ThaumicFinger", Category.MODS, PerformMode.SINGLE);
    }
    
    @Override public void onPerform(PerformSource src) {
        utils.sendPacket("thaumichorizons", (byte) 9, utils.myId(), utils.worldId());
    }

    @Override public boolean isWorking() {
        return Loader.isModLoaded("ThaumicHorizons");
    }
    
    @Override public String moduleDesc() {
        return lang.get("Opening the dupe workbench");
    }

}
