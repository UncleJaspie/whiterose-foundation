package whiterose.foundation.deus.render;

import whiterose.foundation.deus.api.DeusAPI;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

public class GuiRender extends Gui {
    
    private DeusFont deusFont;
    
    public GuiRender() {
        deusFont = new DeusFont();
    }
    
    public FontRenderer mcFont() {
        return DeusAPI.utils.mc().fontRenderer;
    }
    
    public DeusFont deusFont() {
        return deusFont;
    }
    
    public void drawGradientRect(int x, int y, int xMax, int yMax, int col1, int col2) {
        super.drawGradientRect(x, y, xMax, yMax, col1, col2);
    }
    
    public void drawBorderRect(int x, int y, int xMax, int yMax, int borderSize, int rectColor, int borderColor) {
        super.drawRect(x, y, xMax, yMax, rectColor);
        super.drawRect(x - borderSize, y - borderSize, xMax + borderSize, y, borderColor);
        super.drawRect(x - borderSize, yMax, xMax + borderSize, yMax + borderSize, borderColor);
        super.drawRect(x - borderSize, y, x, yMax, borderColor);
        super.drawRect(xMax, y, xMax + borderSize, yMax, borderColor);
    }

    public void drawDesc(String text) {
        int width = deusFont().textWidth(text);
        int height = deusFont().fontHeight();
        int xStart = GuiScaler.mouseX() - 6;
        int yStart = GuiScaler.mouseY() - 16;
        xStart = xStart + width + 10 >= GuiScaler.scaledScreenWidth() ? GuiScaler.scaledScreenWidth() - width - 10 : xStart;
        yStart = yStart <= 10 ? 10 : yStart;
        drawRect(xStart + 4, yStart + 4, xStart + width + 4, yStart + height + 4, Colors.TRANSPARENT_DARK);
        drawBorderRect(xStart, yStart, xStart + width, yStart + height, 1, Colors.BLACK, Colors.ORANGE);
        deusFont().drawString(text, xStart, yStart, Colors.WHITE);
    }
    
}