package top.sparkpixel.hmw.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.sparkpixel.hmw.game.Game;

import java.util.UUID;

/** Fired when one player kills another inside a running match. */
public final class PlayerKillPlayerEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final UUID killer;
    private final UUID victim;

    public PlayerKillPlayerEvent(Game game, UUID killer, UUID victim) {
        this.game = game;
        this.killer = killer;
        this.victim = victim;
    }

    public Game getGame() {
        return this.game;
    }

    public UUID getKiller() {
        return this.killer;
    }

    public UUID getVictim() {
        return this.victim;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
