package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import net.minecraft.tileentity.TileEntity;

public class RedBarrelGive extends CheatModule {
    
    public RedBarrelGive() {
        super("RedBarrelGive", Category.MODS, PerformMode.SINGLE);
    }
    
    @Override public void onPerform(PerformSource src) {
        TileEntity checkTile = utils.tile();
        try {
            if (Class.forName("mrtjp.projectred.exploration.TileBarrel").isInstance(checkTile)) {
                utils.sendPacket("MrTJPCoreMod", (byte) 1, utils.coords(checkTile), (byte) 2, giveSelector().givedItem(), giveSelector().fillAllSlots() ? Integer.MAX_VALUE : giveSelector().itemCount());
            }
        } catch(Exception e) {}
    }
    
    @Override public String moduleDesc() {
        return lang.get("Issuing an item to the Barrel (ProjectRed) which the player is looking at");
    }
    
    @Override public boolean isWorking() {
        return Loader.isModLoaded("ProjRed|Exploration");
    }

}