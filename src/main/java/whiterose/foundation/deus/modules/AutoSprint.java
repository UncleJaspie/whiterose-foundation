package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;

public class AutoSprint extends CheatModule {
    
    public AutoSprint() {
        super("AutoSprint", Category.MOVE, PerformMode.TOGGLE);
    }
    
    @Override public void onTick(boolean inGame) {
        if (inGame) {
            utils.player().setSprinting(true);
        }
    }
    
    @Override public String moduleDesc() {
        return lang.get("Non-stop sprint");
    }

}