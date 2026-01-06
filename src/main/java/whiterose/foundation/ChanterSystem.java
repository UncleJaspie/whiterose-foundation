package whiterose.foundation;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;

import javax.swing.*;
import java.awt.*;

/**
 * Universal Item & NBT builder based on Xenobyte Chanter logic.
 */
public class ChanterSystem {
    private static ChanterSystem instance;
    private JFrame frame;
    private JTextArea nbtJsonArea;
    private ItemStack currentBaseItem;

    public ChanterSystem() {
        currentBaseItem = new ItemStack(Items.diamond); // Default
        createUI();
    }

    public static ChanterSystem getInstance() {
        if (instance == null) instance = new ChanterSystem();
        return instance;
    }

    private void createUI() {
        frame = new JFrame("Xeno Chanter / NBT Editor");
        frame.setSize(400, 500);
        frame.setLayout(new BorderLayout());

        nbtJsonArea = new JTextArea("{display:{Name:\"§bHacked Item\",Lore:[\"Line 1\",\"Line 2\"]}}");
        nbtJsonArea.setLineWrap(true);

        JButton btnApply = new JButton("Apply NBT to Placeholder");
        btnApply.addActionListener(e -> {
            try {
                NBTTagCompound tag = (NBTTagCompound) JsonToNBT.func_150315_a(nbtJsonArea.getText());
                currentBaseItem.setTagCompound(tag);
                PacketScriptTool.log("NBT Constructed successfully.");
            } catch (Exception ex) {
                PacketScriptTool.log("Invalid JSON NBT!");
            }
        });

        frame.add(new JScrollPane(nbtJsonArea), BorderLayout.CENTER);
        frame.add(btnApply, BorderLayout.SOUTH);
    }

    public void show() { frame.setVisible(true); frame.setAlwaysOnTop(true); }

    public ItemStack getCurrentItem() { return currentBaseItem; }

    public NBTTagCompound getOutNBT() {
        try {
            return (NBTTagCompound) JsonToNBT.func_150315_a(nbtJsonArea.getText());
        } catch (Exception e) {
            return new NBTTagCompound();
        }
    }

    /**
     * Re-implementation of Xenobyte's bufWriter for universal packet construction
     */
    public static ByteBuf writeXenoData(Object... data) {
        ByteBuf buf = Unpooled.buffer();
        for (Object o : data) {
            if (o instanceof Integer) buf.writeInt((Integer) o);
            else if (o instanceof String) ByteBufUtils.writeUTF8String(buf, (String) o);
            else if (o instanceof ItemStack) ByteBufUtils.writeItemStack(buf, (ItemStack) o);
            else if (o instanceof NBTTagCompound) ByteBufUtils.writeTag(buf, (NBTTagCompound) o);
            else if (o instanceof Boolean) buf.writeBoolean((Boolean) o);
            else if (o instanceof Double) buf.writeDouble((Double) o);
        }
        return buf;
    }
}