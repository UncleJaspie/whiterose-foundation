package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import whiterose.foundation.deus.gui.swing.CreditsGui;

public class Credits extends CheatModule {
    
    public Credits() {
        super("Credits", Category.MISC, PerformMode.SINGLE);
    }
    
    @Override public void onPerform(PerformSource src) {
        new CreditsGui().showFrame();
    }
    
    @Override public String moduleDesc() {
        return lang.get("Product information + links");
    }

}
