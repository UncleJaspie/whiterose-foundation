package whiterose.foundation.deus.modules;

import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class AntiKnockBack extends CheatModule {
    
    public AntiKnockBack() {
        super("AntiKnockBack", Category.MOVE, PerformMode.TOGGLE);
    }
    
    @Override public boolean doReceivePacket(Packet packet) {
        if (packet instanceof S12PacketEntityVelocity) {
            if (utils.isInGame() && utils.myId() == ((S12PacketEntityVelocity)packet).func_149412_c()) {
                return false;
            }
        }
        return true;
    }
    
    @Override public String moduleDesc() {
        return lang.get("Cancels knockback on the player");
    }

}