package top.sparkpixel.hmw.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.Team;

/** Fired when a match ends; winner is null on forced ends. */
public final class GameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final Team winner;

    public GameEndEvent(Game game, Team winner) {
        this.game = game;
        this.winner = winner;
    }

    public Game getGame() {
        return this.game;
    }

    /** Winning team, or null when the game was force-ended. */
    public Team getWinner() {
        return this.winner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
