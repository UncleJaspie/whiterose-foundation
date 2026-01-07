package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.gui.click.elements.Panel;
import whiterose.foundation.deus.gui.click.elements.ScrollSlider;
import whiterose.foundation.deus.utils.Keys;

public class Spider extends CheatModule {
    
    @Cfg("vSpeed") private float vSpeed;
    private boolean spiding = false;
    
    public Spider() {
        super("Spider", Category.MOVE, PerformMode.TOGGLE);
        vSpeed = 1F;
    }
    
    @Override public void onTick(boolean inGame) {
        if (inGame && utils.player().isCollidedHorizontally && (Keys.isPressed(utils.mc().gameSettings.keyBindForward) || Keys.isPressed(utils.mc().gameSettings.keyBindBack) || Keys.isPressed(utils.mc().gameSettings.keyBindLeft) || Keys.isPressed(utils.mc().gameSettings.keyBindRight))) {
            utils.player().motionY = vSpeed;
            spiding = true;
        } else if(inGame && spiding) {
            utils.player().motionY = Math.min(0.4F, vSpeed);
            spiding = false;
        }
    }
    
    @Override public String moduleDesc() {
        return lang.get("Climbing the walls");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
            new ScrollSlider("Speed", (int) (vSpeed * 10), 30) {
                @Override public void onScroll(int dir, boolean withShift) {
                    vSpeed = (float) processSlider(dir, withShift) / 10;
                }
                @Override public String elementDesc() {
                    return lang.get("Climbing speed");
                }
            }
        );
    }
    
}