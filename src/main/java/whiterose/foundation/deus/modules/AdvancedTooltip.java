package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import net.minecraft.item.Item;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class AdvancedTooltip extends CheatModule {
    
    public AdvancedTooltip() {
        super("AdvancedTooltip", Category.MISC, PerformMode.TOGGLE);
    }
    
    @SubscribeEvent public void tooltipHook(ItemTooltipEvent e) {
        e.toolTip.add(utils.stringId(e.itemStack) + " " + Item.getIdFromItem(e.itemStack.getItem()) + ":" + e.itemStack.getItemDamage());
    }
    
    @Override public String moduleDesc() {
        return lang.get("Displays metadata in the item description");
    }

}