package top.sparkpixel.hmw.game;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-game scoreboard: vanilla red/green teams (friendly fire off), a sidebar
 * and the below-name health display used since the original start2.
 */
public final class GameScoreboard {

    private final Scoreboard scoreboard;
    private final Objective sidebar;
    private final Objective health;
    private final Team redTeam;
    private final Team greenTeam;
    private final List<String> lastLines = new ArrayList<>();

    public GameScoreboard() {
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.sidebar = this.scoreboard.registerNewObjective("hmw_sidebar", Criteria.DUMMY,
                Component.text("Honey Missile Wars", net.kyori.adventure.text.format.NamedTextColor.GOLD,
                        net.kyori.adventure.text.format.TextDecoration.BOLD));
        this.sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.health = this.scoreboard.registerNewObjective("hmw_health", Criteria.HEALTH,
                Component.text("\u2764 ", net.kyori.adventure.text.format.NamedTextColor.RED));
        this.redTeam = this.scoreboard.registerNewTeam("red");
        this.redTeam.setColor(ChatColor.RED);
        this.redTeam.setAllowFriendlyFire(false);
        this.greenTeam = this.scoreboard.registerNewTeam("green");
        this.greenTeam.setColor(ChatColor.GREEN);
        this.greenTeam.setAllowFriendlyFire(false);
    }

    public Scoreboard scoreboard() {
        return this.scoreboard;
    }

    public void assign(Player player, top.sparkpixel.hmw.game.Team team) {
        player.setScoreboard(this.scoreboard);
        if (team != null) {
            (team == top.sparkpixel.hmw.game.Team.RED ? this.redTeam : this.greenTeam).addEntry(player.getName());
            (team == top.sparkpixel.hmw.game.Team.RED ? this.greenTeam : this.redTeam).removeEntry(player.getName());
        }
    }

    public void unassign(Player player) {
        this.redTeam.removeEntry(player.getName());
        this.greenTeam.removeEntry(player.getName());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void showHealthDisplay() {
        this.health.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    public void hideHealthDisplay() {
        this.scoreboard.clearSlot(DisplaySlot.BELOW_NAME);
    }

    /** Replaces the sidebar lines (top first). Max 15 lines, each max 30 chars. */
    public void sidebar(String... lines) {
        // only reset entries we set ourselves - a global reset would also wipe the
        // below-name health scores (they share the scoreboard entry space)
        for (String entry : this.lastLines) {
            this.scoreboard.resetScores(entry);
        }
        this.lastLines.clear();
        int score = lines.length;
        for (String line : lines) {
            if (line.length() > 30) {
                line = line.substring(0, 30);
            }
            this.sidebar.getScore(line).setScore(score--);
            this.lastLines.add(line);
        }
    }
}
