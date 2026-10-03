package top.sparkpixel.hmw.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.missile.MissileType;

/** Fired after a player successfully places a missile. */
public final class MissilePlaceEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final Player player;
    private final MissileType type;
    private final Location location;

    public MissilePlaceEvent(Game game, Player player, MissileType type, Location location) {
        this.game = game;
        this.player = player;
        this.type = type;
        this.location = location;
    }

    public Game getGame() {
        return this.game;
    }

    public Player getPlayer() {
        return this.player;
    }

    public MissileType getType() {
        return this.type;
    }

    public Location getLocation() {
        return this.location;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
