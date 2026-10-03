package top.sparkpixel.hmw.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.Team;

/**
 * Fired when one of a team's three wall layers is breached by missiles.
 * Stage is 1 (innermost), 2 or 3 (white wall).
 */
public final class WallBreachEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final Team defender;
    private final int stage;

    public WallBreachEvent(Game game, Team defender, int stage) {
        this.game = game;
        this.defender = defender;
        this.stage = stage;
    }

    public Game getGame() {
        return this.game;
    }

    /** The team whose wall was breached. */
    public Team getDefender() {
        return this.defender;
    }

    public int getStage() {
        return this.stage;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
