package whiterose.foundation.deus.gui.click;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import whiterose.foundation.deus.api.DeusAPI;
import whiterose.foundation.deus.api.gui.ElementAligment;
import whiterose.foundation.deus.api.gui.GuiElement;
import whiterose.foundation.deus.api.gui.PanelLayout;
import whiterose.foundation.deus.api.gui.PanelSorting;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.gui.click.elements.Button;
import whiterose.foundation.deus.gui.click.elements.Panel;
import whiterose.foundation.deus.handlers.ModuleHandler;
import whiterose.foundation.deus.render.Colors;
import whiterose.foundation.deus.render.GuiScaler;
import whiterose.foundation.deus.utils.Config;
import whiterose.foundation.deus.utils.Keys;
import net.minecraft.client.gui.GuiScreen;

public class DeusGuiScreen extends GuiScreen implements DeusAPI {
    
    private Panel mainPanel, tempSetting;
    private List<GuiElement> elements;
    
    public DeusGuiScreen(ModuleHandler HAND) {
        elements = new CopyOnWriteArrayList<GuiElement>();
        mainPanel = new Panel(new Button(mod_name, HAND.DeusGUI().getKeyName(), ElementAligment.CENTER, Colors.ORANGE, Colors.ORANGE, Colors.ORANGE, Colors.WHITE, Colors.WHITE, Colors.WHITE) {
            @Override public void playClick() {}
            @Override public void onHovered() {
                hideElements();
            }
            @Override public void onKeyTyped(char symb, int key) {
                HAND.bind(HAND.DeusGUI(), this, key);
            }
            @Override public String elementDesc() {
                return lang.get("Version") + " " + mod_version;
            }
        }, PanelLayout.HORIZONTAL, PanelSorting.DEFAULT);
        for (Category CAT : Category.values()) {
            List<CheatModule> categoryed = HAND.categoryedModules().filter(m -> m.getCategory() == CAT).collect(Collectors.toList());
            if (!categoryed.isEmpty()) {
                Panel categoryPanel = new Panel(PanelLayout.VERTICAL, PanelSorting.ALPHABET);
                categoryed.forEach(MOD -> {
                    Panel settingsPanel = MOD.settingPanel();
                    Button modButton = new Button(MOD.getName(), MOD.getKeyName()) {
                        @Override public void onInit() {
                            setSelected(HAND.isEnabled(MOD));
                        }
                        @Override public void onKeyTyped(char symb, int key) {
                            HAND.bind(MOD, this, key);
                        }
                        @Override public void onLeftClick() {
                            HAND.perform(MOD, this);
                        }
                        @Override public String elementDesc() {
                            return MOD.moduleDesc();
                        }
                        @Override public void onDraw() {
                            super.onDraw();
                            if (settingsPanel != null) {
                                render.GUI.drawRect(getMaxX() -1, getY() + 3, getMaxX(), getMaxY() - 3, isShowing(settingsPanel) ? Colors.TRANSPARENT : Colors.ORANGE);
                            }
                        }
                        @Override public void onHovered() {
                            if (tempSetting != null && tempSetting != settingsPanel) {
                                hideElement(tempSetting);
                            }
                            if (settingsPanel != null && !isShowing(settingsPanel)) {
                                tempSetting = settingsPanel;
                                boolean widthUnfit = getMaxX() + settingsPanel.getWidth() > GuiScaler.scaledScreenWidth();
                                settingsPanel.setShadowAligment(widthUnfit ? ElementAligment.LEFT : ElementAligment.RIGHT);
                                settingsPanel.setPos(widthUnfit ? getX() - settingsPanel.getWidth() : getMaxX(), getY() + settingsPanel.getHeight() > GuiScaler.scaledScreenHeight() ? getMaxY() - settingsPanel.getHeight() : getY());
                                showElement(settingsPanel);
                            }
                        }
                    };
                    categoryPanel.add(modButton);
                });
                categoryPanel.pack();
                mainPanel.add(new Button(CAT.toString()) {
                    @Override public void playClick() {}
                    @Override public void onHovered() {
                        if(!isShowing(categoryPanel)) {
                            hideElements();
                            setSelected(true);
                            categoryPanel.setPos(getX(), getMaxY());
                            showElement(categoryPanel);
                        }
                    }
                });
            }
        }
        mainPanel.pack();
        showElement(mainPanel);
    }
    
    private boolean isShowing(GuiElement element) {
        return elements.contains(element);
    }
    
    private void showElement(GuiElement element) {
        elements.add(element);
        element.onShow();
    }
    
    private void hideElement(GuiElement element) {
        elements.remove(element);
        element.onHide();
    }
    
    private void hideElements() {
        elements.stream().filter(e -> e != mainPanel).forEach(this::hideElement);
        mainPanel.setSelected(false);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float ticks) {
        drawGradientRect(0, 0, width, height, Colors.WHITE_BG, Colors.TRANSPARENT);
        GL11.glPushMatrix();
        GuiScaler.setGuiScale();
        Iterator<GuiElement> iterator = elements.iterator();
        while (iterator.hasNext()) {
            iterator.next().draw();
        }
        if (Keys.isShiftDown()) {
            Iterator<GuiElement> descIterator = elements.iterator();
            while (descIterator.hasNext()) {
                descIterator.next().drawDesc();
            }
        }
        GL11.glPopMatrix();
    }
    
    @Override public void keyTyped(char ch, int key) {
        super.keyTyped(ch, key);
        if (Keys.isAvalible(key)) {
            elements.forEach(e -> e.keyTyped(ch, key));
        }
    }
    
    @Override protected void mouseClicked(int mouseX, int mouseY, int key) {
        if(!elements.stream().anyMatch(p -> p.click(key))) {
            utils.closeGuis();
        };
    }
    
    @Override public void handleMouseInput() {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int dir = scroll > 0 ? 1 : -1;
            elements.forEach(e -> e.scroll(dir, isShiftKeyDown()));
        }
    }
    
    @Override public void initGui() {
        mainPanel.setPos(GuiScaler.scaledScreenWidth() / 2 - mainPanel.getWidth() / 2, 0);
        hideElements();
    }
    
    @Override public boolean doesGuiPauseGame() {
        return false;
    }
    
    @Override public void onGuiClosed() {
        Config.save();
    }

}