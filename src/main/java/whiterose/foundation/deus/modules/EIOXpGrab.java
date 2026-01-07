package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import net.minecraft.tileentity.TileEntity;

public class EIOXpGrab extends CheatModule {

    public EIOXpGrab() {
        super("EIOXpGrab", Category.MODS, PerformMode.SINGLE);
    }
    
    private void sendGrab(TileEntity tile) {
        try {
            if (Class.forName("crazypants.enderio.machine.obelisk.xp.TileExperienceObelisk").isInstance(tile)) {
                utils.sendPacket("enderio", (byte) 69, utils.coords(tile), Short.MAX_VALUE);
            }
        } catch(Exception e) {}
    }
    
    @Override public void onPerform(PerformSource src) {
        utils.nearTiles().forEach(this::sendGrab);
    }
    
    @Override public boolean isWorking() {
        return Loader.isModLoaded("EnderIO");
    }
    
    @Override public String moduleDesc() {
        return lang.get("Drains experience from all experience obelisks in the radius");
    }

}