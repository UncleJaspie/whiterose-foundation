package whiterose.foundation.deus.modules;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JSlider;

import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.gui.ColorPicker;
import whiterose.foundation.deus.api.integration.NEI;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import whiterose.foundation.deus.gui.click.elements.Button;
import whiterose.foundation.deus.gui.click.elements.Panel;
import whiterose.foundation.deus.gui.swing.ColorPickerGui;
import whiterose.foundation.deus.render.Colors;
import whiterose.foundation.deus.render.XRayHintRender;
import whiterose.foundation.deus.utils.Config;
import whiterose.foundation.deus.utils.Reflections;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

public class XRaySelect extends CheatModule {
    
    @Cfg("configBlocks") private List<String> configBlocks;
    @Cfg("guiHint") public boolean guiHint;
    private List<String> missingBlocks;
    public List<SelectedBlock> blocks;
    private String neiSubset;
    
    public XRaySelect() {
        super("XRaySelect", Category.NEI, PerformMode.SINGLE);
        blocks = new CopyOnWriteArrayList<SelectedBlock>();
        missingBlocks = new ArrayList<String>();
        configBlocks = new ArrayList<String>();
        neiSubset = "X-Ray";
        guiHint = true;
    }
    
    private void updateNEI() {
        NEI.addSubset(mod_name + "." + neiSubset, blocks.stream().map(b -> b.itemBlock).collect(Collectors.toList()));
    }
    
    public SelectedBlock getBlock(ItemStack stack) {
        return getBlock(b -> b.itemBlockEquals(stack));
    }
    
    public SelectedBlock getBlock(Block block, int meta) {
        return getBlock(b -> b.blockEquals(block, meta));
    }
    
    private SelectedBlock getBlock(Predicate<SelectedBlock> predicate) {
        return blocks.stream().filter(predicate).findFirst().orElse(null);
    }
    
    @Override public void onPostInit() {
        IdentityHashMap<Item, IItemRenderer> customRender = Reflections.getPrivateValue(MinecraftForgeClient.class, null, 0);
        for (Object obj : Item.itemRegistry) {
            if (obj instanceof ItemBlock && !customRender.containsKey(obj)) {
                MinecraftForgeClient.registerItemRenderer((Item) obj, new XRayHintRender(this));
            }
        }
        for (String cBlock : configBlocks) {
            String[] data = cBlock.split(":");
            Block block = (Block) Block.blockRegistry.getObject(data[0] + ":" + data[1]);
            if (block instanceof BlockAir) {
                missingBlocks.add(cBlock);
            } else {
                int meta = Integer.parseInt(data[2]);
                int color = Integer.parseInt(data[3]);
                float scale = Float.parseFloat(data[4]);
                boolean hidden = data.length <= 5 ? false : Boolean.parseBoolean(data[5]);
                boolean tracer = data.length <= 5 ? false : Boolean.parseBoolean(data[6]);
                blocks.add(new SelectedBlock(new ItemStack(block, 1, meta), color, scale, hidden, tracer));
            }
        }
        updateNEI();
    }
    
    @Override public void onPerform(PerformSource src) {
        switch (src) {
        case BUTTON:
               NEI.openGui("@" + neiSubset);
            break;
        case KEY:
            ItemStack stack = utils.isInGameGui() ? utils.getStackFromView() : NEI.getStackMouseOver();
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                SelectedBlock block = getBlock(stack);
                new XRaySettings(block == null ? new SelectedBlock(stack, Colors.BLACK, 1, false, false) : block).showFrame();
            }
        }
    }
    
    @Override public boolean isWorking() {
        return NEI.isAvailable();
    }
    
    @Override public boolean inGuiPerform() {
        return true;
    }
    
    @Override public boolean allowStateMessages() {
        return false;
    }
    
    @Override public String moduleDesc() {
        return lang.get("Selecting by keybind a block for X-Ray in NEI, or by looking at block. Adds NEI @X-Ray tab");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
            new Button("GuiHint", guiHint) {
                @Override public void onLeftClick() {
                    buttonValue(guiHint = !guiHint);
                }
                @Override public String elementDesc() {
                    return lang.get("Highlighting selected items in inventory");
                }
            }
        );
    }
    
    class XRaySettings extends ColorPickerGui {
        
        JCheckBox hidden, tracer;
        SelectedBlock block;
        JSlider s;

        XRaySettings(SelectedBlock block) {
            super(block.itemBlock.getDisplayName(), block);
            this.block = block;
            s = new JSlider(0, 100);
            s.setPreferredSize(new Dimension(350, 50));
            s.setValue((int)(this.block.scale * 100));
            s.addChangeListener((e) -> {
                this.block.scale = (float) s.getValue() / 100;
            });
            hidden = new JCheckBox("Hidden", block.hidden);
            hidden.addActionListener((e) -> {
                this.block.hidden = hidden.isSelected();
            });
            tracer = new JCheckBox("Tracer", block.tracer);
            tracer.addActionListener((e) -> {
                this.block.tracer = tracer.isSelected();
            });
            buttonsBar.add(clear);
            buttonsBar.add(hidden);
            buttonsBar.add(tracer);
            sliders.add(s, GBC);
        }
        
        @Override public void localizeSet() {
            super.localizeSet();
            tracer.setToolTipText(lang.get("Draw tracer line to block"));
            hidden.setToolTipText(lang.get("Hide block from render"));
            s.setBorder(customTitledBorder(lang.get("Size")));
            clear.setText(lang.get("Delete"));
        }
        
        @Override public void actionPerformed(ActionEvent e) {
            if (e.getSource() == accept) {
                if (!blocks.contains(block)) {
                    blocks.add(block);
                }
            } else if (e.getSource() == clear) {
                blocks.remove(block);
            }
            configBlocks.clear();
            configBlocks.addAll(blocks.stream().map(SelectedBlock::toString).collect(Collectors.toList()));
            configBlocks.addAll(missingBlocks);
            Config.save();
            updateNEI();
            dispose();
        }
    }
    
    public class SelectedBlock extends ColorPicker {
        
        public boolean hidden, tracer;
        final ItemStack itemBlock;
        final Block block;
        final String id;
        final int meta;
        float scale;
        
        SelectedBlock(ItemStack itemBlock, int color, float scale, boolean hidden, boolean tracer) {
            super(color);
            this.scale = scale;
            this.hidden = hidden;
            this.tracer = tracer;
            this.itemBlock = itemBlock;
            this.meta = itemBlock.getItemDamage();
            this.id = itemBlock.getItem().delegate.name();
            this.block = Block.getBlockFromItem(itemBlock.getItem());
        }
        
        boolean itemBlockEquals(ItemStack stack) {
            return itemBlock.isItemEqual(stack);
        }
        
        boolean blockEquals(Block block, int meta) {
            return Block.isEqualTo(this.block, block) && this.meta == meta;
        }
        
        @Override public boolean equals(Object o) {
            if (o instanceof SelectedBlock) {
                SelectedBlock sel = (SelectedBlock) o;
                return itemBlockEquals(sel.itemBlock) && blockEquals(sel.block, sel.meta);
            }
            return false;
        }
        
        @Override public String toString() {
            return String.format("%s:%s:%s:%s:%s:%s", id, meta, rgba, scale, hidden, tracer);
        }
        
    }

}