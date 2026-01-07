package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.utils.Reflections;
import net.minecraft.entity.Entity;

public class NoWeb extends CheatModule {
    
    public NoWeb() {
        super("NoWeb", Category.MOVE, PerformMode.TOGGLE);
    }
    
    @Override public void onTick(boolean inGame) {
        if (inGame) {
            Reflections.setPrivateValue(Entity.class, utils.player(), false, 27);
        }
    }
    
    @Override public String moduleDesc() {
        return lang.get("The web no longer clings");
    }

}
