package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class QuestGive extends CheatModule {
    
    public QuestGive() {
        super("QuestGive", Category.MODS, PerformMode.SINGLE);
    }
    
    @Override public void onPerform(PerformSource src) {
        TileEntity checkTile = utils.tile();
        try {
            if (Class.forName("betterquesting.blocks.TileSubmitStation").isInstance(checkTile)) {
                NBTTagCompound stackTag = new NBTTagCompound();
                NBTTagCompound tileTag = new NBTTagCompound();
                NBTTagCompound root = new NBTTagCompound();
                giveSelector().givedItem().writeToNBT(stackTag);
                checkTile.writeToNBT(tileTag);
                if (giveSelector().fillAllSlots()) {
                    tileTag.setTag("ouput", stackTag);
                }
                tileTag.setTag("input", stackTag);
                root.setTag("tile", tileTag);
                Class.forName("betterquesting.network.PacketSender").getMethod("sendToServer", Class.forName("betterquesting.api.network.PacketQuesting")).invoke(Class.forName("betterquesting.network.PacketSender").getField("INSTANCE").get(null), Class.forName("betterquesting.api.network.PacketQuesting").getConstructor(ResourceLocation.class, NBTTagCompound.class).newInstance(new ResourceLocation("betterquesting:edit_station"), root));
            }
        } catch (Exception e) {}
    }
    
    @Override public String moduleDesc() {
        return lang.get("Issuance of an item in the SubmitStation (OSS) which the player is looking at");
    }
    
    @Override public boolean isWorking() {
        return Loader.isModLoaded("betterquesting");
    }

}