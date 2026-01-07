package whiterose.foundation.deus.modules;

import cpw.mods.fml.common.Loader;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class OneWayTicket extends CheatModule {
    
    public OneWayTicket() {
        super("OneWayTicket", Category.MODS, PerformMode.SINGLE);
    }
    
    @Override public void onPerform(PerformSource src) {
        ItemStack ckeckItem = utils.item();
        try {
            if (Class.forName("mods.railcraft.common.util.network.IEditableItem").isInstance(ckeckItem.getItem())) {
                NBTTagCompound nbt = giveSelector().givedNBT();
                if (!nbt.hasNoTags()) {
                    Class.forName("mods.railcraft.common.util.network.PacketDispatcher").getMethod("sendToServer", Class.forName("mods.railcraft.common.util.network.RailcraftPacket")).invoke(null, Class.forName("mods.railcraft.common.util.network.PacketCurrentItemNBT").getConstructor(EntityPlayer.class, ItemStack.class).newInstance(utils.player(), utils.item(ckeckItem, nbt)));
                }
            }
        } catch(Exception e) {}
    }
    
    @Override public String moduleDesc() {
        return lang.get("Applying NBT from Chanter to a Ticket or Routing Table in hand");
    }
    
    @Override public boolean isWorking() {
        return Loader.isModLoaded("Railcraft");
    }

}
