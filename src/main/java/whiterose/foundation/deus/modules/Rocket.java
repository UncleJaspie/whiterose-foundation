package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import whiterose.foundation.deus.gui.click.elements.Panel;
import whiterose.foundation.deus.gui.click.elements.ScrollSlider;

public class Rocket extends CheatModule {
    
    @Cfg("strength") private int strength;
    public Rocket() {
        super("Rocket", Category.MOVE, PerformMode.SINGLE);
        strength = 2;
    }
    
    @Override public void onPerform(PerformSource src) {
        this.utils.player().addVelocity(0.0d, strength * 0.5d, 0.0d);
    }
    
    @Override public String moduleDesc() {
        return lang.get("Acceleration to the sky");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
                new ScrollSlider("Strength", strength, 30) {
                    @Override public void onScroll(int dir, boolean withShift) {
                        strength = processSlider(dir, withShift);
                    }
                    @Override public String elementDesc() {
                        return lang.get("Acceleration amount");
                    }
                }
        );
    }

}