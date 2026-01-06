package whiterose.beenbt;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import forestry.api.arboriculture.TreeManager;
import forestry.core.utils.GeneticsUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

/**
 * Integrated Sapling Converter for the Bee NBT Viewer Mod.
 * This class handles spoofing the Treealyzer conversion logic.
 */
public class SaplingConverter {

    public static SimpleNetworkWrapper network;

    @SideOnly(Side.CLIENT)
    public static KeyBinding keyConvert;

    /**
     * Call this from HudRenderHandler.init or BeeNBTViewerMod.init
     */
    public static void init() {
        // Initialize the network channel using the main ModID for consistency
        network = NetworkRegistry.INSTANCE.newSimpleChannel("SaplingSpoof");
        network.registerMessage(Handler.class, MessageConvert.class, 0, Side.SERVER);

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            setupClient();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void setupClient() {
        // Using LMnu (Left Alt) to trigger the conversion
        keyConvert = new KeyBinding("key.beenbtviewer.convert_sapling", Keyboard.KEY_LMENU, "key.categories.beenbtviewer");
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(keyConvert);

        // Register this class to the FML bus for key input events
        FMLCommonHandler.instance().bus().register(new SaplingConverter());
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // Check if the key was just pressed and no GUI is open (player is in world)
        if (keyConvert.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();

            // If the item is valid but NOT yet recognized as a Forestry Genetic tree
            if (held != null && !TreeManager.treeRoot.isMember(held)) {
                network.sendToServer(new MessageConvert());
            }
        }
    }

    /**
     * The spoof trigger packet sent from Client to Server.
     */
    public static class MessageConvert implements IMessage {
        @Override public void fromBytes(ByteBuf buf) {}
        @Override public void toBytes(ByteBuf buf) {}
    }

    /**
     * Server-side Handler to execute the "Analysis" logic.
     */
    public static class Handler implements IMessageHandler<MessageConvert, IMessage> {
        @Override
        public IMessage onMessage(MessageConvert message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) return null;

            int currentSlot = player.inventory.currentItem;
            ItemStack specimen = player.inventory.getStackInSlot(currentSlot);

            if (specimen != null) {
                // If the item is not a Forestry member, spoof the Treealyzer's conversion logic
                if (!TreeManager.treeRoot.isMember(specimen)) {
                    // This method is the internal logic used by the Treealyzer block/item
                    ItemStack ersatz = GeneticsUtil.convertSaplingToGeneticEquivalent(specimen);

                    if (ersatz != null) {
                        ersatz.stackSize = specimen.stackSize;

                        // Replace the item in the player's hand on the server
                        player.inventory.setInventorySlotContents(currentSlot, ersatz);

                        // Force a sync to the client so the item doesn't "glitch" back
                        player.inventoryContainer.detectAndSendChanges();
                    }
                }
            }
            return null;
        }
    }
}