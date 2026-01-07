package whiterose.foundation.deus.modules;

import java.util.ArrayList;
import java.util.List;

import whiterose.foundation.deus.api.config.Cfg;
import whiterose.foundation.deus.api.gui.InputType;
import whiterose.foundation.deus.api.module.Category;
import whiterose.foundation.deus.api.module.CheatModule;
import whiterose.foundation.deus.api.module.PerformMode;
import whiterose.foundation.deus.gui.click.elements.Button;
import whiterose.foundation.deus.gui.click.elements.Panel;
import whiterose.foundation.deus.gui.click.elements.ScrollSlider;
import whiterose.foundation.deus.gui.swing.UserInput;
import whiterose.foundation.deus.utils.TickHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;

public class XenAura extends CheatModule {
    
    @Cfg("friendsList") private List<String> friendsList;
    @Cfg("ignoreWalls") private boolean ignoreWalls;
    @Cfg("criticals") private boolean criticals;
    @Cfg("handshake") private boolean handshake;
    @Cfg("customnpc") private boolean customnpc;
    @Cfg("villagers") private boolean villagers;
    @Cfg("monsters") private boolean monsters;
    @Cfg("animals") private boolean animals;
    @Cfg("players") private boolean players;
    @Cfg("pointed") private boolean pointed;
    @Cfg("radius") private int radius;
    @Cfg("delay") private int delay;
    
    public XenAura() {
        super("XenAura", Category.PLAYER, PerformMode.TOGGLE);
        friendsList = new ArrayList<String>();
        ignoreWalls = true;
        criticals = true;
        players = true;
        radius = 6;
    }
    
    private void attack(Entity e) {
        EntityPlayer pl = utils.player();
        if (criticals && pl.isCollidedVertically) {
            utils.sendPacket(new C04PacketPlayerPosition(pl.posX, pl.posY + 0.0624D, pl.posY + 1.0D, pl.posZ, true));
            utils.sendPacket(new C04PacketPlayerPosition(pl.posX, pl.posY, pl.posY + 1.0D, pl.posZ, false));
            utils.sendPacket(new C04PacketPlayerPosition(pl.posX, pl.posY + 0.000111D, pl.posY + 1.0D, pl.posZ, false));
            utils.sendPacket(new C04PacketPlayerPosition(pl.posX, pl.posY, pl.posY + 1.0D, pl.posZ, false));
        }
        utils.sendPacket(new C02PacketUseEntity(e, Action.ATTACK));
        if (handshake) {
            pl.swingItem();
        }
    }
    
    @Override public int tickDelay() {
        return delay;
    }
    
    @Override public void onTick(boolean inGame) {
        if (inGame) {
            utils.nearEntityes(radius)
            .filter(e -> e instanceof EntityLivingBase && !e.isDead)
            .filter(e -> (players && utils.isPlayer(e) && !friendsList.contains(e.getCommandSenderName())) || (monsters && utils.isMonster(e)) || (animals && utils.isAnimal(e)) || (customnpc && utils.isCustom(e)) || (villagers && utils.isVillager(e)))
            .filter(e -> ignoreWalls ? true : utils.player().canEntityBeSeen(e))
            .filter(e -> pointed ? e == utils.pointedEntity() : true)
            .forEach(this::attack);
        }
    }
    
    @Override public String moduleDesc() {
        return lang.get("Hitting entities in radius");
    }
    
    @Override public Panel settingPanel() {
        return new Panel(
            new ScrollSlider("Radius", radius, 6) {
                @Override public void onScroll(int dir, boolean withShift) {
                    radius = processSlider(dir, withShift);
                }
                @Override public String elementDesc() {
                    return lang.get("Kill aura radius");
                }
            },
            new ScrollSlider("Delay", delay, 0, TickHelper.ONE_SEC) {
                @Override public void onScroll(int dir, boolean withShift) {
                    delay = processSlider(dir, withShift);
                }
                @Override public String elementDesc() {
                    return lang.get("Hitting delay");
                }
            },
            new Button("Friends") {
                @Override public void onLeftClick() {
                    new UserInput(lang.get("Friends"), friendsList, InputType.CUSTOM).showFrame();
                }
                @Override public String elementDesc() {
                    return lang.get("Whitelist of friends by nicknames");
                }
            },
            new Button("HandShake", handshake) {
                @Override public void onLeftClick() {
                    buttonValue(handshake = !handshake);
                }
                @Override public String elementDesc() {
                    return lang.get("Wave of the hand on hit");
                }
            },
            new Button("IgnoreWalls", ignoreWalls) {
                @Override public void onLeftClick() {
                    buttonValue(ignoreWalls = !ignoreWalls);
                }
                @Override public String elementDesc() {
                    return lang.get("Ignore walls");
                }
            },
            new Button("Pointed", pointed) {
                @Override public void onLeftClick() {
                    buttonValue(pointed = !pointed);
                }
                @Override public String elementDesc() {
                    return lang.get("Striking only by looking at the object");
                }
            },
            new Button("Criticals", criticals) {
                @Override public void onLeftClick() {
                    buttonValue(criticals = !criticals);
                }
                @Override public String elementDesc() {
                    return lang.get("Strike with crits (can lagging)");
                }
            },
            new Button("Players", players) {
                @Override public void onLeftClick() {
                    buttonValue(players = !players);
                }
                @Override public String elementDesc() {
                    return lang.get("Damage to players");
                }
            },
            new Button("Animals", animals) {
                @Override public void onLeftClick() {
                    buttonValue(animals = !animals);
                }
                @Override public String elementDesc() {
                    return lang.get("Damage to animals");
                }
            },
            new Button("Monsters", monsters) {
                @Override public void onLeftClick() {
                    buttonValue(monsters = !monsters);
                }
                @Override public String elementDesc() {
                    return lang.get("Damage to monsters");
                }
            },
            new Button("Villagers", villagers) {
                @Override public void onLeftClick() {
                    buttonValue(villagers = !villagers);
                }
                @Override public String elementDesc() {
                    return lang.get("Damage to villagers");
                }
            },
            new Button("CustomNPC", customnpc) {
                @Override public void onLeftClick() {
                    buttonValue(customnpc = !customnpc);
                }
                @Override public String elementDesc() {
                    return lang.get("Damage to custom npc");
                }
            }
        );
    }
    
}