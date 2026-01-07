package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.utils.Reflections;
import net.minecraft.client.Minecraft;

public class FastPlace extends CheatModule {
    
    public FastPlace() {
        super("FastPlace", Category.WORLD, PerformMode.TOGGLE);
    }
    
    @Override public void onTick(boolean inGame) {
        if (inGame) {
            Reflections.setPrivateValue(Minecraft.class, utils.mc(), 0, 47);
        }
    }
    
    @Override public String moduleDesc() {
        return lang.get("Quick block placing");
    }

}