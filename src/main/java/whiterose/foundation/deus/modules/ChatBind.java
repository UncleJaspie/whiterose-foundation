package whiterose.foundation.deus.modules;

import java.util.ArrayList;
import java.util.List;

import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.gui.InputType;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.api.module.PerformSource;
import whiterose.foundation.deus.gui.click.elements.Button;
import whiterose.foundation.deus.gui.click.elements.Panel;
import whiterose.foundation.deus.gui.swing.UserInput;

public class ChatBind extends CheatModule {
    
    @Cfg("commands") private List<String> commands;
    
    public ChatBind() {
        super("ChatBind", Category.MISC, PerformMode.SINGLE);
        commands = new ArrayList<String>();
        commands.add("/home");
    }
    
    @Override public void onPerform(PerformSource src) {
        commands.forEach(utils::serverChatMessage);
    }
    
    @Override public String moduleDesc() {
        return lang.get("Execution of the set commands by keybind");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
            new Button("Commands") {
                @Override public void onLeftClick() {
                    new UserInput(lang.get("Commands"), commands, InputType.CUSTOM).showFrame();
                }
                @Override public String elementDesc() {
                    return lang.get("Command list");
                }
            }
        );
    }

}
