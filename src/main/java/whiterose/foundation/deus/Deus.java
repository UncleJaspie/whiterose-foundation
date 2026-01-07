package whiterose.foundation.deus;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import whiterose.foundation.deus.api.DeusAPI;
import whiterose.foundation.deus.handlers.ModuleHandler;

@Mod(modid = DeusAPI.mod_id, name = DeusAPI.mod_name, version = DeusAPI.mod_version)

public class Deus {
    
    @EventHandler public void init(FMLInitializationEvent e) {
        if (e == null) {
            starter(null);
        }
    }
    
    @EventHandler public void starter(FMLLoadCompleteEvent e) {
        new ModuleHandler();
    }
    
}