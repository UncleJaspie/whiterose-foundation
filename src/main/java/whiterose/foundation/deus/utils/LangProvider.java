package whiterose.foundation.deus.utils;

import net.minecraft.client.Minecraft;

public class LangProvider {
    
    public String get(String eng) {
        switch (Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode()) {
        default:
            return eng;
        }
    }

}