package whiterose.foundation.deus.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import whiterose.foundation.deus.render.Renderer;
import whiterose.foundation.deus.utils.LangProvider;
import whiterose.foundation.deus.utils.Utils;

public interface DeusAPI {

    String mod_id = "deus";
    String mod_name = "Whiterose Foundation Deus";
    String mod_version = "1.0.9";
    String mod_author = "Whiterose";
    String format_prefix = "§8[§4" + mod_name + "§8]§r ";

    Utils utils = new Utils();
    Renderer render = new Renderer();
    LangProvider lang = new LangProvider();
    Logger logger = LogManager.getLogger(mod_name);
    
}