package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.gui.click.elements.Button;
import whiterose.foundation.deus.gui.click.elements.Panel;
import whiterose.foundation.deus.gui.click.elements.ScrollSlider;
import whiterose.foundation.deus.utils.TickHelper;

public class AutoDrop extends CheatModule {
    
    @Cfg("allStack") private boolean allStack;
    @Cfg("delay") private int delay;
    @Cfg("slot") private int slot;
    
    public AutoDrop() {
        super("AutoDrop", Category.PLAYER, PerformMode.TOGGLE);
        slot = 1;
    }
    
    @Override public int tickDelay() {
        return delay;
    }
    
    @Override public void onTick(boolean inGame) {
        if (inGame) {
            utils.dropSlot(utils.mySlotsCount() + (slot - 1), allStack);
        }
    }
    
    @Override public String moduleDesc() {
        return lang.get("Item drop from the active slot");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
            new ScrollSlider("Delay", delay, 0, TickHelper.ONE_SEC) {
                @Override public void onScroll(int dir, boolean withShift) {
                    delay = processSlider(dir, withShift);
                }
                @Override
                public String elementDesc() {
                    return lang.get("Item drop delay");
                }
            },
            new ScrollSlider("Slot", slot, 9) {
                @Override public void onScroll(int dir, boolean withShift) {
                    slot = processSlider(dir, withShift);
                }
                @Override public String elementDesc() {
                    return lang.get("The slot from which items are dropped");
                }
            },
            new Button("AllStack", allStack) {
                @Override public void onLeftClick() {
                    buttonValue(allStack = !allStack);
                }
                @Override public String elementDesc() {
                    return lang.get("Drop an entire stack or one item at a time");
                }
            }
        );
    }

}