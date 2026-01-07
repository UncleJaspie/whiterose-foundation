package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;

public class TinkerNoFall extends CheatModule {
    
    public TinkerNoFall() {
        super("TinkerNoFall", Category.MODS, PerformMode.TOGGLE);
    }
    
    @Override public void onTick(boolean inGame) {
        if (inGame && utils.player().fallDistance > 2) {
            utils.sendPacket("TConstruct", (byte) 4);
        }
    }
    
    @Override public boolean isWorking() {
        return Loader.isModLoaded("TConstruct");
    }

    @Override public String moduleDesc() {
        return lang.get("Removes fall damage");
    }
    
}
