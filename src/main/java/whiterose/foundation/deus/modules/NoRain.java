package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;

public class NoRain extends CheatModule {
    
    public NoRain() {
        super("NoRain", Category.WORLD, PerformMode.TOGGLE);
    }
    
    @Override public void onTick(boolean inGame) {
        if (inGame) {
            utils.world().setRainStrength(0);
        }
    }
    
    @Override public String moduleDesc() {
        return lang.get("When the PC is shit");
    }

}
