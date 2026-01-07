package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.gui.click.elements.Panel;
import whiterose.foundation.deus.gui.click.elements.ScrollSlider;
import whiterose.foundation.deus.utils.Reflections;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

public class ClientSpeed extends CheatModule {
    
    @Cfg("tickRate") private int tickRate; 
    private Timer vanilaTimer;
    
    public ClientSpeed() {
        super("ClientSpeed", Category.PLAYER, PerformMode.TOGGLE);
        tickRate = 5;
    }
    
    @Override public void onPostInit() {
        vanilaTimer = Reflections.getPrivateValue(Minecraft.class, utils.mc(), 16);
    }
    
    @Override public void onDisabled() {
        vanilaTimer.timerSpeed = 1;
    }
    
    @Override public void onTick(boolean inGame) {
        if (inGame) {
            vanilaTimer.timerSpeed = tickRate;
        }    
    }
    
    @Override public String moduleDesc() {
        return lang.get("Changing the speed of client ticks");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
            new ScrollSlider("TickRate", tickRate, 20) {
                @Override public void onScroll(int dir, boolean withShift) {
                    tickRate = processSlider(dir, withShift);
                }
                @Override public String elementDesc() {
                    return lang.get("Tick modifier");
                }
            }
        );
    }

}