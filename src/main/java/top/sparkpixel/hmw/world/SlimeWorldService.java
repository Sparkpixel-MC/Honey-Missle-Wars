package top.sparkpixel.hmw.world;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.loaders.file.FileLoader;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SlimeWorld (Advanced Slime Paper) integration.
 *
 * <p>Follows the ASP documentation shipped in the map's docs/ folder:
 * <ul>
 *   <li>{@code readVanillaWorld} + {@code saveWorld} to import the anvil map once (/hmwadmin import)</li>
 *   <li>{@code readWorld(loader, name, readOnly=true, props)} to obtain the template (I/O, async)</li>
 *   <li>{@code SlimeWorld#clone(name, null)} for a volatile per-game copy (never stored)</li>
 *   <li>{@code loadWorld(world, true)} synchronously on the main thread</li>
 *   <li>{@code Bukkit.unloadWorld(name, false)} to discard a game world (never save=true inside onDisable)</li>
 * </ul>
 */
public final class SlimeWorldService {

    public static final String GAME_WORLD_PREFIX = "hmw_game_";
    public static final String WAITING_ROOM_WORLD = "hmw_waiting_room";

    private final HoneyMissileWarsPlugin plugin;
    /** ASP API handle; null when the ASPaperPlugin is not installed (its service provider is missing). */
    private final AdvancedSlimePaperAPI asp;
    private final SlimeLoader loader;

    private final Object templateLock = new Object();
    private SlimeWorld cachedTemplate;
    private final AtomicBoolean importing = new AtomicBoolean(false);
    private CompletableFuture<World> waitingRoomFuture;

    public SlimeWorldService(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
        AdvancedSlimePaperAPI api;
        try {
            api = AdvancedSlimePaperAPI.instance();
        } catch (Throwable ex) {
            api = null;
        }
        this.asp = api;

        String dir = plugin.hmwConfig().slimeDir();
        File slimeDir = new File(dir).isAbsolute() ? new File(dir)
                : new File(plugin.getDataFolder(), dir);
        this.loader = new FileLoader(slimeDir);
    }

    public boolean isAspAvailable() {
        return this.asp != null;
    }

    private SlimePropertyMap gameProperties() {
        SlimePropertyMap props = new SlimePropertyMap();
        props.setValue(SlimeProperties.SPAWN_X, 0);
        props.setValue(SlimeProperties.SPAWN_Y, 72);
        props.setValue(SlimeProperties.SPAWN_Z, 0);
        props.setValue(SlimeProperties.DIFFICULTY, "normal");
        props.setValue(SlimeProperties.ALLOW_ANIMALS, false);
        props.setValue(SlimeProperties.ALLOW_MONSTERS, false);
        props.setValue(SlimeProperties.PVP, true);
        props.setValue(SlimeProperties.DRAGON_BATTLE, false);
        props.setValue(SlimeProperties.ENVIRONMENT, "normal");
        props.setValue(SlimeProperties.WORLD_TYPE, "DEFAULT");
        return props;
    }

    /** True when the base template exists in the loader. */
    public boolean templateExists() {
        try {
            return this.loader.worldExists(this.plugin.hmwConfig().baseTemplate());
        } catch (Exception ex) {
            return false;
        }
    }

    public void logTemplateStatus() {
        if (templateExists()) {
            this.plugin.getLogger().info("Slime template '" + this.plugin.hmwConfig().baseTemplate()
                    + "' found. Games can be created.");
        } else {
            this.plugin.getLogger().warning("No slime template found. Run '/hmwadmin import <world folder>' "
                    + "with the extracted Honey Missile Wars map folder before starting games.");
        }
    }

    /** Cached read-only template; must be called off the main thread (I/O). */
    private SlimeWorld baseTemplate() throws Exception {
        synchronized (this.templateLock) {
            if (this.cachedTemplate != null) {
                return this.cachedTemplate;
            }
            String name = this.plugin.hmwConfig().baseTemplate();
            SlimeWorld world = this.asp.readWorld(this.loader, name, true, gameProperties());
            this.cachedTemplate = world;
            return world;
        }
    }

    /**
     * Imports an anvil world folder as the base template.
     * I/O runs asynchronously; the returned message is a human readable result.
     */
    public CompletableFuture<String> importVanillaWorld(File worldDir, String templateName) {
        CompletableFuture<String> result = new CompletableFuture<>();
        if (!this.importing.compareAndSet(false, true)) {
            result.complete("An import is already running.");
            return result;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                this.plugin.getLogger().info("Reading vanilla world from " + worldDir.getAbsolutePath() + " ...");
                SlimeWorld world = this.asp.readVanillaWorld(worldDir, templateName, this.loader);
                this.plugin.getLogger().info("Saving slime template '" + templateName + "' ...");
                this.asp.saveWorld(world); // blocks until saved (docs: saving_worlds.md)
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    synchronized (this.templateLock) {
                        this.cachedTemplate = null; // force re-read of the fresh template
                    }
                });
                result.complete("Imported '" + worldDir.getName() + "' as slime template '" + templateName + "'.");
            } catch (Exception ex) {
                this.plugin.getLogger().severe("Import failed: " + ex);
                result.complete("Import failed: " + ex.getMessage()
                        + " (is the folder a valid world? is a world with that name already stored?)");
            } finally {
                this.importing.set(false);
            }
        });
        return result;
    }

    /**
     * Creates a fresh volatile game world cloned from the base template.
     * The template read runs async; the clone + {@code loadWorld} run on the main thread.
     */
    public CompletableFuture<World> createGameWorld(String worldName) {
        CompletableFuture<World> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                SlimeWorld template = baseTemplate();
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    try {
                        // clone with a null loader = temporary in-memory world (never persisted)
                        SlimeWorld clone = template.clone(worldName, null);
                        SlimeWorldInstance instance = this.asp.loadWorld(clone, true);
                        World bukkit = instance.getBukkitWorld();
                        applyWorldRules(bukkit);
                        future.complete(bukkit);
                    } catch (Exception ex) {
                        future.completeExceptionally(ex);
                    }
                });
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private void applyWorldRules(World world) {
        // Mirrors the map's chop:util/gamerules
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.DO_LIMITED_CRAFTING, true);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
    }

    /** Discards a game world (changes are never saved - game worlds are disposable). */
    public void disposeGameWorld(String worldName) {
        World bukkit = Bukkit.getWorld(worldName);
        if (bukkit != null) {
            Bukkit.unloadWorld(bukkit, false);
        }
    }

    // ------------------------------------------------------------------
    // waiting room (queue waiting area in its own world)
    // ------------------------------------------------------------------

    /**
     * Loads the waiting room world once and returns a future with the loaded
     * world. The future completes with {@code null} when the waiting room
     * template has not been imported (callers fall back to the vanilla
     * wait-zone world). The template is read read-only, so waiting players can
     * never permanently alter the user's map.
     */
    public synchronized CompletableFuture<World> ensureWaitingRoomWorld() {
        if (this.waitingRoomFuture != null) {
            return this.waitingRoomFuture;
        }
        this.waitingRoomFuture = new CompletableFuture<>();
        if (this.asp == null) {
            this.waitingRoomFuture.complete(null);
            return this.waitingRoomFuture;
        }
        String templateName = this.plugin.hmwConfig().waitingRoomTemplate();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                if (!this.loader.worldExists(templateName)) {
                    this.plugin.getLogger().warning("Waiting room template '" + templateName
                            + "' not found - the queue will wait in the configured wait-zone world. "
                            + "Import your waiting room map with /hmwadmin import <folder> " + templateName);
                    this.waitingRoomFuture.complete(null);
                    return;
                }
                SlimeWorld world = this.asp.readWorld(this.loader, templateName, true, waitingRoomProperties());
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    try {
                        World existing = Bukkit.getWorld(templateName);
                        if (existing != null) {
                            this.waitingRoomFuture.complete(existing);
                            return;
                        }
                        // the read-only template loads under its own name, once per server session
                        SlimeWorldInstance instance = this.asp.loadWorld(world, true);
                        World bukkit = instance.getBukkitWorld();
                        applyWaitingRoomRules(bukkit);
                        this.waitingRoomFuture.complete(bukkit);
                    } catch (Exception ex) {
                        this.waitingRoomFuture.completeExceptionally(ex);
                    }
                });
            } catch (Exception ex) {
                this.waitingRoomFuture.completeExceptionally(ex);
            }
        });
        return this.waitingRoomFuture;
    }

    private SlimePropertyMap waitingRoomProperties() {
        SlimePropertyMap props = new SlimePropertyMap();
        props.setValue(SlimeProperties.DIFFICULTY, "peaceful");
        props.setValue(SlimeProperties.ALLOW_ANIMALS, false);
        props.setValue(SlimeProperties.ALLOW_MONSTERS, false);
        props.setValue(SlimeProperties.PVP, false);
        props.setValue(SlimeProperties.DRAGON_BATTLE, false);
        return props;
    }

    private void applyWaitingRoomRules(World world) {
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
    }

    public void shutdown() {
        String waitingRoom = this.plugin.hmwConfig().waitingRoomTemplate();
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith(GAME_WORLD_PREFIX) || world.getName().equals(waitingRoom)) {
                Bukkit.unloadWorld(world, false);
            }
        }
    }
}
