package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;

public class TinkerChest extends CheatModule {
    
    public TinkerChest() {
        super("TinkerChest", Category.MODS, PerformMode.SINGLE);
    }
    
    @Override public void onPerform(PerformSource src) {
        utils.sendPacket("TConstruct", (byte) 1, 102);
    }

    @Override public boolean isWorking() {
        return Loader.isModLoaded("TConstruct");
    }
    
    @Override public String moduleDesc() {
        return lang.get("Opening a travel bag without having it in your inventory");
    }

}
