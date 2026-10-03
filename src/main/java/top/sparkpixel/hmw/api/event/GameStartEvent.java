package top.sparkpixel.hmw.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.sparkpixel.hmw.game.Game;

/** Fired when a match moves from the countdown into the battle. */
public final class GameStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;

    public GameStartEvent(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return this.game;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
