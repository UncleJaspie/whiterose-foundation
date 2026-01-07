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
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.swing.*;
import java.awt.*;

public class WireframeManager {

    // Key to open the new GUI
    public static KeyBinding configKey = new KeyBinding("Wireframe Config", Keyboard.KEY_K, "Whiterose Foundation");

    // Filter Settings
    private static boolean showPlayers = false;
    private static boolean showHostiles = false;
    private static boolean showFriendlies = false;
    private static boolean rainbowEffect = true;

    public static void init() {
        // Register key and listeners similarly to your Inspector
        ClientRegistry.registerKeyBinding(configKey);
        WireframeManager instance = new WireframeManager();
        FMLCommonHandler.instance().bus().register(instance);
        MinecraftForge.EVENT_BUS.register(instance);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (configKey.isPressed() && mc.currentScreen == null) {
            openConfigGui();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        float partialTicks = event.partialTicks;
        double rX = RenderManager.instance.viewerPosX;
        double rY = RenderManager.instance.viewerPosY;
        double rZ = RenderManager.instance.viewerPosZ;
        long time = System.currentTimeMillis();

        setupGL();

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityLivingBase)) continue;
            EntityLivingBase entity = (EntityLivingBase) obj;

            // Don't highlight self
            if (entity == mc.thePlayer) continue;

            boolean shouldRender = false;
            Color color = Color.WHITE;

            if (entity instanceof EntityPlayer && showPlayers) {
                shouldRender = true;
                color = Color.CYAN;
            } else if (entity instanceof EntityMob && showHostiles) {
                shouldRender = true;
                color = Color.RED;
            } else if (!(entity instanceof EntityMob) && !(entity instanceof EntityPlayer) && showFriendlies) {
                shouldRender = true;
                color = Color.GREEN;
            }

            if (shouldRender) {
                drawWireframe(entity, rX, rY, rZ, partialTicks, time, color);
            }
        }

        releaseGL();
    }

    private void setupGL() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);
    }

    private void releaseGL() {
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void drawWireframe(Entity entity, double rX, double rY, double rZ, float partialTicks, long time, Color baseColor) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rZ;

        AxisAlignedBB bb = entity.boundingBox;
        if (bb == null) return;

        double width = (bb.maxX - bb.minX) / 2.0;
        double height = (bb.maxY - bb.minY);

        if (rainbowEffect) {
            float hue = (float) ((time % 3000) / 3000.0);
            Color color = Color.getHSBColor(hue, 0.8f, 1.0f);
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.7f);
        } else {
            GL11.glColor4f(baseColor.getRed() / 255f, baseColor.getGreen() / 255f, baseColor.getBlue() / 255f, 0.7f);
        }

        AxisAlignedBB drawBox = AxisAlignedBB.getBoundingBox(x - width, y, z - width, x + width, y + height, z + width);

        // Render box lines
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

    private void openConfigGui() {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

            final JFrame frame = new JFrame("Wireframe Settings");
            frame.setSize(300, 350);
            frame.setLayout(new GridLayout(6, 1, 10, 10));
            frame.setLocationRelativeTo(null);

            final JCheckBox playerCb = new JCheckBox("Show Players", showPlayers);
            final JCheckBox hostileCb = new JCheckBox("Show Hostiles", showHostiles);
            final JCheckBox friendlyCb = new JCheckBox("Show Friendlies", showFriendlies);
            final JCheckBox rainbowCb = new JCheckBox("Rainbow Effect", rainbowEffect);

            JButton saveBtn = new JButton("Apply & Close");
            saveBtn.addActionListener(e -> {
                showPlayers = playerCb.isSelected();
                showHostiles = hostileCb.isSelected();
                showFriendlies = friendlyCb.isSelected();
                rainbowEffect = rainbowCb.isSelected();
                frame.dispose();
            });

            frame.add(new JLabel("  Filter Entities:", SwingConstants.CENTER));
            frame.add(playerCb);
            frame.add(hostileCb);
            frame.add(friendlyCb);
            frame.add(rainbowCb);
            frame.add(saveBtn);

            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
        });
    }
}