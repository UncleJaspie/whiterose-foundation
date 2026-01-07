package whiterose.foundation.deus.api.gui;

import whiterose.foundation.deus.render.Colors;

public enum WidgetMode {
    
    INFO(Colors.SKY), SUCCESS(Colors.GREEN), FAIL(Colors.RED);
    
    private int color;
    
    WidgetMode(int color) {
        this.color = color;
    }
    
    public int getColor() {
        return color;
    }
    
}