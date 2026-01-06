package whiterose.foundation;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.*;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class PlayerInspector {

    public static KeyBinding inspectorKey = new KeyBinding("key.beenbtviewer.inspect", Keyboard.KEY_X, "key.categories.beenbtviewer");

    // Tracking for the wireframe feature
    private static final Set<Integer> highlightedEntities = new HashSet<>();
    private static boolean wireframeEnabled = true;

    private static final double INSPECT_DISTANCE = 50.0D;
    private static final double LIST_DISTANCE = 100.0D;

    public static void init() {
        ClientRegistry.registerKeyBinding(inspectorKey);
        PlayerInspector instance = new PlayerInspector();
        FMLCommonHandler.instance().bus().register(instance);
        MinecraftForge.EVENT_BUS.register(instance); // Needed for RenderWorldLastEvent
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (inspectorKey.isPressed() && mc.currentScreen == null) {
            Entity target = getPointedEntity(mc, INSPECT_DISTANCE);
            if (target instanceof EntityLivingBase) {
                openInspector((EntityLivingBase) target);
            } else {
                openEntityListScanner(mc);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!wireframeEnabled || highlightedEntities.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        float partialTicks = event.partialTicks;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        // Setup GL for wireframe
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST); // See through walls
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);

        double renderPosX = RenderManager.instance.viewerPosX;
        double renderPosY = RenderManager.instance.viewerPosY;
        double renderPosZ = RenderManager.instance.viewerPosZ;

        long time = System.currentTimeMillis();

        for (Object obj : mc.theWorld.loadedEntityList) {
            Entity entity = (Entity) obj;
            if (highlightedEntities.contains(entity.getEntityId())) {
                drawWireframe(entity, renderPosX, renderPosY, renderPosZ, partialTicks, time);
            }
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void drawWireframe(Entity entity, double rX, double rY, double rZ, float partialTicks, long time) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rZ;

        AxisAlignedBB bb = entity.boundingBox;
        double width = (bb.maxX - bb.minX) / 2.0;
        double height = (bb.maxY - bb.minY);

        // Rainbow logic
        float hue = (float) ((time % 3000) / 3000.0);
        Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.7f);

        // Draw Box
        AxisAlignedBB drawBox = AxisAlignedBB.getBoundingBox(x - width, y, z - width, x + width, y + height, z + width);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(drawBox.minX, drawBox.minY, drawBox.minZ);
        GL11.glVertex3d(drawBox.maxX, drawBox.minY, drawBox.minZ);
        GL11.glVertex3d(drawBox.maxX, drawBox.minY, drawBox.maxZ);
        GL11.glVertex3d(drawBox.minX, drawBox.minY, drawBox.maxZ);
        GL11.glVertex3d(drawBox.minX, drawBox.minY, drawBox.minZ);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(drawBox.minX, drawBox.maxY, drawBox.minZ);
        GL11.glVertex3d(drawBox.maxX, drawBox.maxY, drawBox.minZ);
        GL11.glVertex3d(drawBox.maxX, drawBox.maxY, drawBox.maxZ);
        GL11.glVertex3d(drawBox.minX, drawBox.maxY, drawBox.maxZ);
        GL11.glVertex3d(drawBox.minX, drawBox.maxY, drawBox.minZ);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(drawBox.minX, drawBox.minY, drawBox.minZ); GL11.glVertex3d(drawBox.minX, drawBox.maxY, drawBox.minZ);
        GL11.glVertex3d(drawBox.maxX, drawBox.minY, drawBox.minZ); GL11.glVertex3d(drawBox.maxX, drawBox.maxY, drawBox.minZ);
        GL11.glVertex3d(drawBox.maxX, drawBox.minY, drawBox.maxZ); GL11.glVertex3d(drawBox.maxX, drawBox.maxY, drawBox.maxZ);
        GL11.glVertex3d(drawBox.minX, drawBox.minY, drawBox.maxZ); GL11.glVertex3d(drawBox.minX, drawBox.maxY, drawBox.maxZ);
        GL11.glEnd();
    }

    private Entity getPointedEntity(Minecraft mc, double reach) {
        Entity renderViewEntity = mc.renderViewEntity;
        if (renderViewEntity == null) return null;

        Vec3 position = Vec3.createVectorHelper(renderViewEntity.posX, renderViewEntity.posY + (double)renderViewEntity.getEyeHeight(), renderViewEntity.posZ);
        Vec3 look = renderViewEntity.getLookVec();
        Vec3 rayEnd = position.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        Entity pointedEntity = null;
        List list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(renderViewEntity,
                renderViewEntity.boundingBox.addCoord(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach).expand(1.0, 1.0, 1.0));

        double minDistance = reach;
        for (Object o : list) {
            Entity entity = (Entity) o;
            if (entity.canBeCollidedWith()) {
                float borderSize = entity.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity.boundingBox.expand(borderSize, borderSize, borderSize);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(position, rayEnd);

                if (axisalignedbb.isVecInside(position)) {
                    if (0.0D < minDistance) {
                        pointedEntity = entity;
                        minDistance = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double dist = position.distanceTo(movingobjectposition.hitVec);
                    if (dist < minDistance) {
                        pointedEntity = entity;
                        minDistance = dist;
                    }
                }
            }
        }
        return pointedEntity;
    }

    private void openEntityListScanner(final Minecraft mc) {
        final List<EntityLivingBase> nearbyEntities = new ArrayList<EntityLivingBase>();
        for (Object obj : mc.theWorld.loadedEntityList) {
            if (obj instanceof EntityLivingBase && obj != mc.thePlayer) {
                EntityLivingBase e = (EntityLivingBase) obj;
                if (mc.thePlayer.getDistanceToEntity(e) <= LIST_DISTANCE) {
                    nearbyEntities.add(e);
                }
            }
        }

        Collections.sort(nearbyEntities, (o1, o2) -> Double.compare(mc.thePlayer.getDistanceToEntity(o1), mc.thePlayer.getDistanceToEntity(o2)));

        if (nearbyEntities.isEmpty()) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[Inspector] No entities nearby."));
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            final JFrame frame = new JFrame("Nearby Entities");
            frame.setSize(400, 500);
            frame.setLocationRelativeTo(null);

            DefaultListModel<String> model = new DefaultListModel<>();
            for (EntityLivingBase e : nearbyEntities) {
                String type = (e instanceof EntityPlayer) ? "[PLAYER] " : "[MOB] ";
                model.addElement(type + "[" + (int)mc.thePlayer.getDistanceToEntity(e) + "m] " + e.getCommandSenderName());
            }

            final JList<String> list = new JList<>(model);
            list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        int index = list.locationToIndex(evt.getPoint());
                        if (index >= 0) openInspector(nearbyEntities.get(index));
                    }
                }
            });

            frame.add(new JScrollPane(list));
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
        });
    }

    private void openInspector(final EntityLivingBase target) {
        final NBTTagCompound nbt = new NBTTagCompound();
        try { target.writeToNBT(nbt); } catch (Exception e) { target.writeEntityToNBT(nbt); }

        final String entityName = target.getCommandSenderName();

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

            JFrame frame = new JFrame("NBT Inspector: " + entityName);
            frame.setSize(900, 750);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());

            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JButton expandAllBtn = new JButton("Expand All");
            JButton collapseAllBtn = new JButton("Collapse All");

            // Wireframe Toggles
            final JButton toggleWireframeBtn = new JButton(highlightedEntities.contains(target.getEntityId()) ? "Disable Wireframe" : "Enable Wireframe");
            toggleWireframeBtn.setBackground(highlightedEntities.contains(target.getEntityId()) ? new Color(255, 150, 150) : new Color(150, 255, 150));

            toggleWireframeBtn.addActionListener(e -> {
                int id = target.getEntityId();
                if (highlightedEntities.contains(id)) {
                    highlightedEntities.remove(id);
                    toggleWireframeBtn.setText("Enable Wireframe");
                    toggleWireframeBtn.setBackground(new Color(150, 255, 150));
                } else {
                    highlightedEntities.add(id);
                    toggleWireframeBtn.setText("Disable Wireframe");
                    toggleWireframeBtn.setBackground(new Color(255, 150, 150));
                }
            });

            JComboBox<String> potionDropdown = new JComboBox<>();
            potionDropdown.addItem("--- Potion Registry ---");
            for (Potion p : Potion.potionTypes) {
                if (p != null) {
                    String localized = StatCollector.translateToLocal(p.getName());
                    potionDropdown.addItem(p.id + ": " + localized);
                }
            }

            JButton copyPotionId = new JButton("Copy ID");
            copyPotionId.addActionListener(e -> {
                String selected = (String) potionDropdown.getSelectedItem();
                if (selected != null && selected.contains(":")) setClipboard(selected.split(":")[0]);
            });

            topPanel.add(toggleWireframeBtn);
            topPanel.add(new JLabel(" | "));
            topPanel.add(expandAllBtn);
            topPanel.add(collapseAllBtn);
            topPanel.add(new JLabel(" | Registry: "));
            topPanel.add(potionDropdown);
            topPanel.add(copyPotionId);

            DefaultMutableTreeNode rootNode = createNbtTree(nbt, "Root");
            final JTree nbtTree = new JTree(new DefaultTreeModel(rootNode));
            nbtTree.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(nbtTree);

            expandAllBtn.addActionListener(e -> {
                for (int i = 0; i < nbtTree.getRowCount(); i++) nbtTree.expandRow(i);
            });
            collapseAllBtn.addActionListener(e -> {
                for (int i = nbtTree.getRowCount() - 1; i >= 1; i--) nbtTree.collapseRow(i);
            });

            JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
            JButton copyJsonBtn = new JButton("Copy as SNBT");
            JButton copySelectedBtn = new JButton("Copy Value");

            copyJsonBtn.addActionListener(e -> {
                setClipboard(nbt.toString());
                JOptionPane.showMessageDialog(frame, "Full NBT Copied!");
            });

            copySelectedBtn.addActionListener(e -> {
                TreePath path = nbtTree.getSelectionPath();
                if (path != null) setClipboard(path.getLastPathComponent().toString());
            });

            bottomPanel.add(copyJsonBtn);
            bottomPanel.add(copySelectedBtn);

            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(bottomPanel, BorderLayout.SOUTH);
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
        });
    }

    private DefaultMutableTreeNode createNbtTree(NBTBase base, String name) {
        String label = name + ": ";
        DefaultMutableTreeNode node;

        if (base instanceof NBTTagCompound) {
            node = new DefaultMutableTreeNode(name + " (" + ((NBTTagCompound)base).func_150296_c().size() + " tags)");
            NBTTagCompound compound = (NBTTagCompound) base;
            List<String> keys = new ArrayList<>((Set<String>) compound.func_150296_c());
            Collections.sort(keys);
            for (String key : keys) {
                node.add(createNbtTree(compound.getTag(key), key));
            }
        } else if (base instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) base;
            node = new DefaultMutableTreeNode(name + " [" + list.tagCount() + " entries]");
            for (int i = 0; i < list.tagCount(); i++) {
                node.add(new DefaultMutableTreeNode("[" + i + "] " + list.getCompoundTagAt(i).toString()));
            }
        } else {
            node = new DefaultMutableTreeNode(label + base.toString());
        }
        return node;
    }

    private static void setClipboard(String text) {
        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }
}