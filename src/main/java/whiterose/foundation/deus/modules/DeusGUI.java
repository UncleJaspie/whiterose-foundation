package whiterose.foundation.deus.modules;

import org.lwjgl.input.Keyboard;

import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import whiterose.foundation.deus.gui.click.DeusGuiScreen;

public class DeusGUI extends CheatModule {
    
    public DeusGUI() {
        super("DeusGUI", Category.NONE, PerformMode.SINGLE);
        setKeyBind(Keyboard.KEY_B);
    }
    
    @Override public void onPerform(PerformSource type) {
        utils.openGui(new DeusGuiScreen(moduleHandler()), true);
    }
    
    @Override public boolean allowStateMessages() {
        return false;
    }

}