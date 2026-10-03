package top.sparkpixel.hmw.reward;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import top.sparkpixel.hmw.HmwConfig;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.game.Team;

/**
 * Reward payout with Vault economy and/or vanilla XP. All amounts come from
 * config.yml (rewards section). Vault is optional - when absent only XP is
 * granted (or nothing, if currency is VAULT).
 */
public final class RewardService {

    private final HoneyMissileWarsPlugin plugin;
    private Economy economy;
    private boolean vaultPresent;

    public RewardService(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Called on enable; safe to call again after a reload. */
    public void setupEconomy() {
        this.vaultPresent = Bukkit.getPluginManager().getPlugin("Vault") != null;
        if (!this.vaultPresent) {
            this.plugin.getLogger().info("Vault not found - economy rewards disabled (XP rewards still work).");
            return;
        }
        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            this.plugin.getLogger().warning("Vault is installed but no economy provider registered.");
            this.vaultPresent = false;
            return;
        }
        this.economy = provider.getProvider();
        this.plugin.getLogger().info("Economy hooked: " + this.economy.getName());
    }

    private boolean payMoney(Player player, double amount, String reasonKey, String reasonFallback) {
        if (this.vaultPresent && this.economy != null) {
            this.economy.depositPlayer(player, amount);
            player.sendMessage(this.plugin.messages().format(reasonKey, reasonFallback,
                    "{amount}", String.format("%.2f", amount)));
            return true;
        }
        return false;
    }

    private void payXp(Player player, double coins) {
        double xp = coins * this.plugin.hmwConfig().xpPerCoin();
        if (xp > 0) {
            player.giveExp((int) Math.round(xp));
        }
    }

    private boolean wantsMoney() {
        HmwConfig currency = this.plugin.hmwConfig();
        return currency.rewardsEnabled()
                && (currency.currency() == HmwConfig.RewardCurrency.VAULT
                || currency.currency() == HmwConfig.RewardCurrency.BOTH);
    }

    private boolean wantsXp() {
        HmwConfig currency = this.plugin.hmwConfig();
        return currency.rewardsEnabled()
                && (currency.currency() == HmwConfig.RewardCurrency.XP
                || currency.currency() == HmwConfig.RewardCurrency.BOTH);
    }

    public void awardKill(Player player) {
        if (!this.plugin.hmwConfig().rewardsEnabled()) {
            return;
        }
        this.plugin.statsService().trackKill(player.getUniqueId());
        double amount = this.plugin.hmwConfig().rewardKill();
        if (wantsMoney()) {
            payMoney(player, amount, "reward.kill", "<green>+{amount} coins</green> <gray>(kill)</gray>");
        }
        if (wantsXp()) {
            payXp(player, amount);
        }
    }

    public void awardMissile(Player player) {
        if (!this.plugin.hmwConfig().rewardsEnabled()) {
            return;
        }
        this.plugin.statsService().trackMissile(player.getUniqueId());
        double amount = this.plugin.hmwConfig().rewardMissile();
        if (wantsMoney()) {
            payMoney(player, amount, "reward.missile", "<green>+{amount} coins</green> <gray>(missile placed)</gray>");
        }
        if (wantsXp()) {
            payXp(player, amount);
        }
    }

    public void awardShield(Player player) {
        if (!this.plugin.hmwConfig().rewardsEnabled()) {
            return;
        }
        this.plugin.statsService().trackShield(player.getUniqueId());
        double amount = this.plugin.hmwConfig().rewardShield();
        if (wantsMoney()) {
            payMoney(player, amount, "reward.shield", "<green>+{amount} coins</green> <gray>(shield deployed)</gray>");
        }
        if (wantsXp()) {
            payXp(player, amount);
        }
    }

    /** Paid to every online member of the attacking team when a wall layer breaks. */
    public void awardBreach(Team attackingTeam) {
        if (!this.plugin.hmwConfig().rewardsEnabled()) {
            return;
        }
        // Team is resolved by the game via teamPlayers; this service only knows the
        // team, so it looks the players up through the games registry.
        for (var game : this.plugin.gameManager().games()) {
            for (Player member : game.teamPlayers(attackingTeam)) {
                double amount = this.plugin.hmwConfig().rewardBreach();
                if (wantsMoney()) {
                    payMoney(member, amount, "reward.breach",
                            "<green>+{amount} coins</green> <gray>(wall breached)</gray>");
                }
                if (wantsXp()) {
                    payXp(member, amount);
                }
            }
        }
    }

    /** Win + participation payout at game end. */
    public void awardEnd(Player player, boolean won, boolean participated) {
        if (!this.plugin.hmwConfig().rewardsEnabled() || !participated) {
            return;
        }
        if (won) {
            double win = this.plugin.hmwConfig().rewardWin();
            if (wantsMoney()) {
                payMoney(player, win, "reward.win", "<green>+{amount} coins</green> <gold>(victory!)</gold>");
            }
            if (wantsXp()) {
                payXp(player, win);
            }
        }
        double participation = this.plugin.hmwConfig().rewardParticipation();
        if (wantsMoney()) {
            payMoney(player, participation, "reward.participation",
                    "<green>+{amount} coins</green> <gray>(participation)</gray>");
        }
        if (wantsXp()) {
            payXp(player, participation);
        }
    }
}
