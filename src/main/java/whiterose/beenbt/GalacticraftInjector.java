package whiterose.beenbt;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.tileentity.TileEntity;

public class GalacticraftInjector {

    public static String getLastLog() {
        return lastLog;
    }

    private static String lastLog = "";

    /**
     * Main injection method. Attempts to force an item into a remote TileEntity.
     */
    public static void performAdvancedInjection(TileEntity te, ItemStack item) {
        if (item == null) {
            log("[!] No item provided for injection.");
            return;
        }

        log("[*] Injecting " + item.getDisplayName() + " into TileEntity at " + te.xCoord + "," + te.yCoord + "," + te.zCoord);

        // Strategy A: Galacticraft PacketDynamicInventory (Type 1)
        // This is the most likely to work on machines like Oxygen Collectors, Refineries, etc.
        sendGCPacketInventory(te, item);

        // Strategy B: Vanilla Custom Payload Mocking (Book Edit)
        // Useful if the server blocks GC packets but allows vanilla book edits.
        sendVanillaPayload(te, item);
    }

    /**
     * Constructs a PacketDynamicInventory payload.
     * Mimics: PacketDynamicInventory(TileEntity chest)
     */
    private static void sendGCPacketInventory(TileEntity te, ItemStack stack) {
        try {
            ByteBuf buffer = Unpooled.buffer();

            // 1. Packet Type: 1 = TileEntity (0 is for Entity/Player)
            buffer.writeInt(1);

            // 2. Coordinates
            buffer.writeInt(te.xCoord);
            buffer.writeInt(te.yCoord);
            buffer.writeInt(te.zCoord);

            // 3. Inventory Size
            // We set this to 1 to overwrite the first slot.
            // Setting it larger might be safer to avoid shrinking the inventory if we only want to target slot 0.
            // However, the original code loops through 'this.stacks.length', so sending size 1 writes to slot 0.
            buffer.writeInt(1);

            // 4. The Item
            writeStack(buffer, stack);

            // 5. Send
            FMLProxyPacket proxy = new FMLProxyPacket(buffer, "GalacticraftCore");
            Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(proxy);
            log("[Sent] GC PacketDynamicInventory (Slot 0)");

        } catch (Exception e) {
            log("[Fail] GC Packet: " + e.getMessage());
        }
    }

    private static void sendVanillaPayload(TileEntity te, ItemStack stack) {
        try {
            // Only effective if the item looks like a book
            if (stack.getItem() instanceof net.minecraft.item.ItemEditableBook || stack.getItem() instanceof net.minecraft.item.ItemWritableBook) {
                ByteBuf buf = Unpooled.buffer();
                writeStack(buf, stack);
                C17PacketCustomPayload p = new C17PacketCustomPayload("MC|BEdit", buf.array());
                Minecraft.getMinecraft().getNetHandler().addToSendQueue(p);
                log("[Sent] Vanilla Book Payload");
            }
        } catch (Exception e) {}
    }

    /**
     * Helper to write an ItemStack to the buffer in the format Galacticraft expects.
     * Mimics NetworkUtil.writeItemStack
     */
    private static void writeStack(ByteBuf buf, ItemStack stack) throws Exception {
        if (stack == null) {
            buf.writeShort(-1);
            return;
        }
        buf.writeShort(net.minecraft.item.Item.getIdFromItem(stack.getItem()));
        buf.writeByte(stack.stackSize);
        buf.writeShort(stack.getItemDamage());

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            buf.writeShort(-1);
        } else {
            byte[] bytes = net.minecraft.nbt.CompressedStreamTools.compress(nbt);
            buf.writeShort((short)bytes.length);
            buf.writeBytes(bytes);
        }
    }

    private static void log(String s) {
        lastLog = s;
        // In a real mod, you might pipe this to a GUI console
        System.out.println("[GC-Inject] " + s);
    }

    // --- Additional Griefing Utilities based on PacketSimple analysis ---

    /**
     * Toggles the "Disable" state of a machine remotely.
     * Uses PacketSimple S_UPDATE_DISABLEABLE_BUTTON
     */
    public static void toggleMachineState(TileEntity te) {
        try {
            // PacketSimple ID for S_UPDATE_DISABLEABLE_BUTTON is usually around 10 or 11 depending on version.
            // Based on code analysis, it's an enum. We construct the packet manually.
            // We need to know the exact Enum ID for S_UPDATE_DISABLEABLE_BUTTON.
            // Assuming it matches the order in standard GC versions.

            // This requires constructing a PacketSimple which uses a generic Object[] array.
            // Implementing this requires mapping the EnumSimplePacket to an ID.
            log("[Info] Remote disable requires Enum mapping. Skipping for now.");
        } catch (Exception e) {
            log("[Fail] Toggle State: " + e.getMessage());
        }
    }
}