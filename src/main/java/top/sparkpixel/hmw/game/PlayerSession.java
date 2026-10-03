package top.sparkpixel.hmw.game;

import java.util.UUID;

/** Per-player in-game data for one game session. */
public final class PlayerSession {

    public final UUID playerId;
    public Team team;
    public boolean alive;

    // stats
    public int kills;
    public int deaths;
    public int missilesPlaced;
    public int shieldsDeployed;
    public int wallsBreached;

    // respawn
    public int respawnTicksLeft = -1;

    // kill attribution
    public UUID lastDamager;
    public long lastDamageAt;

    public PlayerSession(UUID playerId, Team team) {
        this.playerId = playerId;
        this.team = team;
        this.alive = true;
    }

    public boolean hasRecentDamager(long now, long windowMs) {
        return this.lastDamager != null && now - this.lastDamageAt <= windowMs;
    }
}
