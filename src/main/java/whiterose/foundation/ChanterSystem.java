package whiterose.foundation;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.RegistryNamespaced;

import javax.swing.*;
import java.awt.*;

public class ChanterSystem {
    private static ChanterSystem instance;
    private JFrame frame;
    private JTextArea nbtJsonArea;
    private JTextField itemIdField;

    public ChanterSystem() {
        createUI();
    }

    public static ChanterSystem getInstance() {
        if (instance == null) instance = new ChanterSystem();
        return instance;
    }

    private void createUI() {
        frame = new JFrame("Xeno Chanter");
        frame.setSize(450, 600);
        frame.setLayout(new BorderLayout());

        JPanel top = new JPanel(new GridLayout(0, 1));
        top.setBorder(BorderFactory.createTitledBorder("Item Configuration"));

        itemIdField = new JTextField("minecraft:diamond");
        top.add(new JLabel("Item Registry ID:"));
        top.add(itemIdField);

        nbtJsonArea = new JTextArea("{display:{Name:\"§cFoundation Artifact\",Lore:[\"§7Custom NBT Data\"]}}");
        nbtJsonArea.setLineWrap(true);
        nbtJsonArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(nbtJsonArea);
        scroll.setBorder(BorderFactory.createTitledBorder("NBT JSON"));

        JButton btnApply = new JButton("Test Build Item");
        btnApply.setBackground(new Color(200, 200, 255));

        btnApply.addActionListener(e -> {
            ItemStack stack = getCurrentItem();
            if (stack != null) {
                PacketScriptTool.log("Built: " + stack.getDisplayName() + " with tag: " + stack.getTagCompound());
            } else {
                PacketScriptTool.log("Failed to build item. Check ID/JSON.");
            }
        });

        frame.add(top, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(btnApply, BorderLayout.SOUTH);
        frame.setAlwaysOnTop(true);
    }

    public void show() { frame.setVisible(true); }

    public ItemStack getCurrentItem() {
        try {
            Item item = (Item) Item.itemRegistry.getObject(itemIdField.getText());
            if (item == null) return null;

            ItemStack stack = new ItemStack(item);
            stack.setTagCompound(getOutNBT());
            return stack;
        } catch (Exception e) {
            return null;
        }
    }

    public NBTTagCompound getOutNBT() {
        try {
            String json = nbtJsonArea.getText().trim();
            if (json.isEmpty()) return new NBTTagCompound();
            return (NBTTagCompound) JsonToNBT.func_150315_a(json);
        } catch (Exception e) {
            return new NBTTagCompound();
        }
    }
}