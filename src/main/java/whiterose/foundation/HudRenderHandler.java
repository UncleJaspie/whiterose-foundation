package whiterose.foundation;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import forestry.api.apiculture.IBee;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.EnumTreeChromosome;
import forestry.api.arboriculture.IAlleleGrowth;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleInteger;
import forestry.api.genetics.IAlleleBoolean;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.IChromosomeType;
import forestry.api.genetics.ISpeciesRoot;
import forestry.arboriculture.tiles.TileTreeContainer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Mod(modid = HudRenderHandler.MODID, name = "Whiterose Foundation", version = "6.7", dependencies = "required-after:Forestry", useMetadata = true)
public class HudRenderHandler {
    public static final String MODID = "whiterose_foundation";
    public static Logger logger = FMLLog.getLogger();
    public static KeyBinding keyOverlay;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        keyOverlay = new KeyBinding("Beealyzer Overlay", Keyboard.KEY_LMENU, "Whiterose Foundation");
        ClientRegistry.registerKeyBinding(keyOverlay);
        PlayerInspector.init();
        PacketScriptTool.init();
        WireframeManager.init();
        NetherScope.init();
        MinecraftForge.EVENT_BUS.register(new HudRenderHandler.ClientEvents());
    }

    public static class TraitEntry {
        public final String category;
        public final String active;
        public final String inactive;
        public final boolean isHeterozygous;

        public TraitEntry(String category, String active, String inactive) {
            this.category = category;
            this.active = active;
            this.inactive = inactive;
            this.isHeterozygous = !active.equals(inactive);
        }
    }

    public static class ClientEvents {
        private static final int COLOR_MATCH_GREEN = 0xCC00FF00;
        private static final int COLOR_MISMATCH_RED = 0xCCFF5555;
        private static final int COLOR_BACKGROUND = 0xFF151515;
        private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
        private static final int COLOR_HEADER_GRAY = 0xFFAAAAAA;
        private static final int COLOR_GOLD = 0xFFFFD700;

        @SubscribeEvent
        public void onGuiPostRender(GuiScreenEvent.DrawScreenEvent.Post event) {
            drawHUDLogic(event.mouseX, event.mouseY, event.gui);
        }

        @SubscribeEvent
        public void onRenderTick(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
            if (event.type == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL) {
                if (Minecraft.getMinecraft().currentScreen == null) {
                    drawHUDLogic(-1, -1, null);
                }
            }
        }

        private void drawHUDLogic(int mouseX, int mouseY, GuiScreen gui) {
            Minecraft mc = Minecraft.getMinecraft();
            if (!Keyboard.isKeyDown(keyOverlay.getKeyCode())) return;

            IIndividual individual = null;
            ISpeciesRoot root = null;

            // Updated Multi-Source Detection
            ItemStack hoveredStack = findStackUniversal(mc, gui, mouseX, mouseY);

            if (hoveredStack != null) {
                root = AlleleManager.alleleRegistry.getSpeciesRoot(hoveredStack);
                if (root != null) individual = root.getMember(hoveredStack);
            }

            if (individual == null && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                TileEntity te = mc.theWorld.getTileEntity(mc.objectMouseOver.blockX, mc.objectMouseOver.blockY, mc.objectMouseOver.blockZ);
                if (te instanceof TileTreeContainer) {
                    individual = ((TileTreeContainer) te).getTree();
                    root = AlleleManager.alleleRegistry.getSpeciesRoot("rootTrees");
                } else if (te instanceof IBeeHousing) {
                    root = AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
                    ItemStack queen = ((IBeeHousing) te).getBeeInventory().getQueen();
                    if (queen == null) queen = tryGetQueenFromNBT(te);
                    if (queen != null) individual = root.getMember(queen);
                }
            }

            if (individual != null && root != null) {
                String typeTitle = formatRootTitle(root.getUID());
                List<TraitEntry> traits = (individual instanceof ITree) ? getTreeTraits((ITree) individual) :
                        (individual instanceof IBee) ? getBeeTraits((IBee) individual) : new ArrayList<TraitEntry>();

                int renderX = (mouseX == -1) ? mc.displayWidth / 4 + 20 : mouseX;
                int renderY = (mouseY == -1) ? mc.displayHeight / 4 : mouseY;
                renderHUD(mc, traits, typeTitle, renderX, renderY);
            }
        }

        /**
         * Enhanced Stack Detection including NEI and Modded Container fallbacks
         */
        private ItemStack findStackUniversal(Minecraft mc, GuiScreen gui, int mouseX, int mouseY) {
            if (gui == null) return null;

            // 1. NEI Hook (Reflection)
            try {
                Class<?> gcm = Class.forName("codechicken.nei.guihook.GuiContainerManager");
                Method getStackMouse = gcm.getMethod("getStackMouseOver", GuiScreen.class);
                ItemStack stack = (ItemStack) getStackMouse.invoke(null, gui);
                if (stack != null) return stack;
            } catch (Exception ignored) {}

            // 2. Standard and Modded Container Check
            if (gui instanceof GuiContainer) {
                GuiContainer container = (GuiContainer) gui;
                try {
                    // Try reflection for standard hovered slot
                    Slot slot = (Slot) ReflectionHelper.getPrivateValue(GuiContainer.class, container, "field_147006_u", "theSlot", "u");
                    if (slot != null && slot.getHasStack()) return slot.getStack();

                    // Brute force check for custom containers where 'theSlot' isn't updated
                    int guiLeft = ReflectionHelper.getPrivateValue(GuiContainer.class, container, "field_147003_i", "guiLeft", "i");
                    int guiTop = ReflectionHelper.getPrivateValue(GuiContainer.class, container, "field_147009_r", "guiTop", "r");
                    for (Object obj : container.inventorySlots.inventorySlots) {
                        Slot s = (Slot) obj;
                        int x = mouseX - guiLeft;
                        int y = mouseY - guiTop;
                        if (x >= s.xDisplayPosition - 1 && x <= s.xDisplayPosition + 16 && y >= s.yDisplayPosition - 1 && y <= s.yDisplayPosition + 16) {
                            if (s.getHasStack()) return s.getStack();
                        }
                    }
                } catch (Exception ignored) {}
            }
            return null;
        }

        private ItemStack tryGetQueenFromNBT(TileEntity te) {
            try {
                NBTTagCompound nbt = new NBTTagCompound();
                te.writeToNBT(nbt);
                if (nbt.hasKey("Items", Constants.NBT.TAG_LIST)) {
                    NBTTagList items = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
                    for (int i = 0; i < items.tagCount(); i++) {
                        NBTTagCompound itemNbt = items.getCompoundTagAt(i);
                        if (itemNbt.getByte("Slot") == 0) return ItemStack.loadItemStackFromNBT(itemNbt);
                    }
                }
            } catch (Exception e) {}
            return null;
        }

        private String formatRootTitle(String uid) {
            if (uid.toLowerCase().contains("bees")) return "Bee Genome";
            if (uid.toLowerCase().contains("trees")) return "Tree Genome";
            if (uid.toLowerCase().contains("butterflies")) return "Butterfly Genome";
            if (uid.toLowerCase().contains("flowers")) return "Flower Genome";
            String clean = uid.replace("forestry.root", "").replace("root", "");
            if (clean.isEmpty()) return "Genome";
            return clean.substring(0, 1).toUpperCase() + clean.substring(1).toLowerCase() + " Genome";
        }

        private List<TraitEntry> getTreeTraits(ITree tree) {
            List<TraitEntry> list = new ArrayList<TraitEntry>();
            list.add(getEntry("Species", tree, EnumTreeChromosome.SPECIES));
            list.add(getEntry("Saplings", tree, EnumTreeChromosome.FERTILITY));
            list.add(getEntry("Maturity", tree, EnumTreeChromosome.MATURATION));
            list.add(getEntry("Height", tree, EnumTreeChromosome.HEIGHT));
            list.add(getEntry("Girth", tree, EnumTreeChromosome.GIRTH));
            list.add(getEntry("Yield", tree, EnumTreeChromosome.YIELD));
            list.add(getEntry("Sappiness", tree, EnumTreeChromosome.SAPPINESS));
            list.add(getEntry("Fireproof", tree, EnumTreeChromosome.FIREPROOF));
            list.add(getEntry("Effect", tree, EnumTreeChromosome.EFFECT));
            list.add(getEntry("Growth", tree, EnumTreeChromosome.GROWTH));
            list.add(getEntry("Fruits", tree, EnumTreeChromosome.FRUITS));
            return list;
        }

        private List<TraitEntry> getBeeTraits(IBee bee) {
            List<TraitEntry> list = new ArrayList<TraitEntry>();
            list.add(getEntry("Species", bee, EnumBeeChromosome.SPECIES));
            list.add(getEntry("Lifespan", bee, EnumBeeChromosome.LIFESPAN));
            list.add(getEntry("Speed", bee, EnumBeeChromosome.SPEED));
            list.add(getEntry("Flowers", bee, EnumBeeChromosome.FLOWER_PROVIDER));
            list.add(getEntry("Fertility", bee, EnumBeeChromosome.FERTILITY));
            list.add(getEntry("Area", bee, EnumBeeChromosome.TERRITORY));
            list.add(getEntry("Effect", bee, EnumBeeChromosome.EFFECT));
            list.add(getEntry("Climate", bee, EnumBeeChromosome.TEMPERATURE_TOLERANCE));
            list.add(getEntry("Humidity", bee, EnumBeeChromosome.HUMIDITY_TOLERANCE));
            list.add(getEntry("Nocturnal", bee, EnumBeeChromosome.NOCTURNAL));
            list.add(getEntry("Tolerant Flyer", bee, EnumBeeChromosome.TOLERANT_FLYER));
            list.add(getEntry("Cave Dwelling", bee, EnumBeeChromosome.CAVE_DWELLING));
            return list;
        }

        private TraitEntry getEntry(String label, IIndividual individual, IChromosomeType chromosome) {
            IAllele active = individual.getGenome().getActiveAllele(chromosome);
            IAllele inactive = individual.getGenome().getInactiveAllele(chromosome);
            String activeStr;
            String inactiveStr;

            if (individual instanceof ITree) {
                ITree tree = (ITree) individual;
                if (chromosome == EnumTreeChromosome.GROWTH) {
                    activeStr = StatCollector.translateToLocal(tree.getGenome().getGrowthProvider().getDescription());
                    inactiveStr = StatCollector.translateToLocal(((IAlleleGrowth)inactive).getProvider().getDescription());
                } else if (chromosome == EnumTreeChromosome.FRUITS) {
                    activeStr = formatFruitAllele(active);
                    inactiveStr = formatFruitAllele(inactive);
                } else {
                    activeStr = formatAllele(active, chromosome);
                    inactiveStr = formatAllele(inactive, chromosome);
                }
            } else {
                activeStr = formatAllele(active, chromosome);
                inactiveStr = formatAllele(inactive, chromosome);
            }
            return new TraitEntry(label, activeStr, inactiveStr);
        }

        private String formatFruitAllele(IAllele allele) {
            if (allele == null) return "None";
            String name = allele.getName();
            if (name.equalsIgnoreCase("fruits.none") || name.equalsIgnoreCase("forestry.fruits.none")) return "None";
            String localized = StatCollector.translateToLocal(name);
            if (localized.equals(name)) return name.replace("forestry.fruits.", "").replace("fruits.", "");
            return localized;
        }

        private String formatAllele(IAllele allele, IChromosomeType chromosome) {
            if (allele == null) return "-";
            String name = chromosome.getName().toLowerCase();
            if (name.contains("fertility") && allele instanceof IAlleleInteger) return "x" + ((IAlleleInteger) allele).getValue();
            if (name.equals("girth") && allele instanceof IAlleleInteger) {
                int val = ((IAlleleInteger) allele).getValue();
                return val + "x" + val;
            }
            if (allele instanceof IAlleleBoolean) return ((IAlleleBoolean) allele).getValue() ? "Yes" : "No";
            return formatAlleleName(allele);
        }

        private String formatAlleleName(IAllele allele) {
            String localized = allele.getName();
            if (localized.startsWith("forestry.")) {
                String translated = StatCollector.translateToLocal(localized);
                if (!translated.equals(localized)) return translated;
                String[] parts = localized.split("\\.");
                return parts[parts.length - 1].substring(0, 1).toUpperCase() + parts[parts.length - 1].substring(1).toLowerCase();
            }
            return localized;
        }

        private void renderHUD(Minecraft mc, List<TraitEntry> traits, String title, int x, int y) {
            FontRenderer fr = mc.fontRenderer;
            String h1 = "Category", h2 = "Active", h3 = "Inactive";
            int col1W = fr.getStringWidth(h1), col2W = fr.getStringWidth(h2), col3W = fr.getStringWidth(h3);

            for (TraitEntry t : traits) {
                col1W = Math.max(col1W, fr.getStringWidth(t.category));
                col2W = Math.max(col2W, fr.getStringWidth(t.active));
                col3W = Math.max(col3W, fr.getStringWidth(t.inactive));
            }

            int lineH = 10, sidePad = 8, colPad = 15;
            int totalWidth = col1W + col2W + col3W + (colPad * 2) + (sidePad * 2);
            int totalHeight = (traits.size() + 3) * lineH + 10;

            ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int renderX = (x == -1) ? (res.getScaledWidth() / 2) + 10 : x + 12;
            int renderY = (y == -1) ? (res.getScaledHeight() / 2) - (totalHeight / 2) : y - totalHeight / 2;

            if (renderX + totalWidth > res.getScaledWidth()) renderX = res.getScaledWidth() - totalWidth - 4;
            if (renderY + totalHeight > res.getScaledHeight()) renderY = res.getScaledHeight() - totalHeight - 4;
            if (renderY < 4) renderY = 4;

            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glTranslatef(0, 0, 800);

            Gui.drawRect(renderX, renderY, renderX + totalWidth, renderY + totalHeight, COLOR_BACKGROUND);
            int borderColor = getRainbowColor();
            Gui.drawRect(renderX, renderY, renderX + totalWidth, renderY + 1, borderColor);
            Gui.drawRect(renderX, renderY + totalHeight - 1, renderX + totalWidth, renderY + totalHeight, borderColor);
            Gui.drawRect(renderX, renderY, renderX + 1, renderY + totalHeight, borderColor);
            Gui.drawRect(renderX + totalWidth - 1, renderY, renderX + totalWidth, renderY + totalHeight, borderColor);

            int curY = renderY + 6;
            int c2X = renderX + sidePad + col1W + colPad;
            int c3X = c2X + col2W + colPad;

            fr.drawString(EnumChatFormatting.BOLD + title, renderX + sidePad, curY, COLOR_GOLD);
            curY += lineH;
            fr.drawString(EnumChatFormatting.UNDERLINE + h1, renderX + sidePad, curY, COLOR_TEXT_WHITE);
            fr.drawString(EnumChatFormatting.UNDERLINE + h2, c2X, curY, COLOR_TEXT_WHITE);
            fr.drawString(EnumChatFormatting.UNDERLINE + h3, c3X, curY, COLOR_TEXT_WHITE);
            curY += lineH + 2;

            for (TraitEntry t : traits) {
                int color = t.isHeterozygous ? COLOR_MISMATCH_RED : COLOR_MATCH_GREEN;
                fr.drawString(t.category, renderX + sidePad, curY, COLOR_HEADER_GRAY);
                fr.drawString(t.active, c2X, curY, color);
                fr.drawString(t.inactive, c3X, curY, color);
                curY += lineH;
            }
            GL11.glPopMatrix();
        }

        private int getRainbowColor() {
            float hue = (System.currentTimeMillis() % 8000) / 8000f;
            return java.awt.Color.HSBtoRGB(hue, 0.6f, 1f);
        }
    }
}