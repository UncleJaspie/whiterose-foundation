package whiterose.foundation.deus.gui.click.elements;

import whiterose.foundation.deus.api.gui.ElementAligment;
import whiterose.foundation.deus.api.gui.TextElement;
import whiterose.foundation.deus.api.gui.WidgetMode;
import whiterose.foundation.deus.render.Colors;

public class GuiWidget extends TextElement {

    private static int indicatorWidth = 2;
    private ElementAligment aligment;
    private final int bgColor;
    private WidgetMode mode;
    public int delay;
    
    public GuiWidget(String text, WidgetMode mode, ElementAligment indicatorAligment, int bgColor, int delay) {
        super(text, indicatorAligment == ElementAligment.LEFT ? ElementAligment.RIGHT : ElementAligment.LEFT, indicatorWidth + (indicatorAligment == ElementAligment.LEFT ? 1 : 0), 0);
        this.aligment = indicatorAligment;
        this.bgColor = bgColor;
        this.delay = delay;
        this.mode = mode;
    }
    
    @Override public void draw() {
        render.GUI.drawRect(getX(), getY(), getMaxX(), getMaxY(), bgColor);
        int indicatorX = aligment == ElementAligment.LEFT ? getX() : getMaxX() - indicatorWidth;
        render.GUI.drawRect(indicatorX, getY(), indicatorX + indicatorWidth, getMaxY(), mode.getColor());
        render.GUI.deusFont().drawString(getText(), getTextX(), getY(), Colors.WHITE);
    }

}