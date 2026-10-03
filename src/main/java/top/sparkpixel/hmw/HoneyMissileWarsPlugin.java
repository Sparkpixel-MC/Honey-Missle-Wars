package top.sparkpixel.hmw;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import top.sparkpixel.hmw.command.HmwAdminCommand;
import top.sparkpixel.hmw.command.HmwCommand;
import top.sparkpixel.hmw.game.GameManager;
import top.sparkpixel.hmw.integration.HmwApi;
import top.sparkpixel.hmw.item.ItemFactory;
import top.sparkpixel.hmw.listener.BlockListener;
import top.sparkpixel.hmw.listener.DamageListener;
import top.sparkpixel.hmw.listener.LobbyListener;
import top.sparkpixel.hmw.listener.MissileListener;
import top.sparkpixel.hmw.listener.PlayerListener;
import top.sparkpixel.hmw.listener.ProjectileListener;
import top.sparkpixel.hmw.missile.LobbyDisplayService;
import top.sparkpixel.hmw.missile.MissileService;
import top.sparkpixel.hmw.missile.StructureService;
import top.sparkpixel.hmw.reward.RewardService;
import top.sparkpixel.hmw.reward.StatsService;
import top.sparkpixel.hmw.world.SlimeWorldService;
import top.sparkpixel.hmw.integration.PlaceholderHook;

/**
 * Plugin entry point. Wires up all services and the global per-tick driver.
 */
public final class HoneyMissileWarsPlugin extends JavaPlugin {

    private static HoneyMissileWarsPlugin instance;

    private HmwConfig config;
    private Messages messages;
    private SlimeWorldService worldService;
    private ItemFactory itemFactory;
    private StructureService structureService;
    private MissileService missileService;
    private LobbyDisplayService lobbyDisplayService;
    private GameManager gameManager;
    private RewardService rewardService;
    private StatsService statsService;

    public static HoneyMissileWarsPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        registerBungeeChannel();

        this.config = new HmwConfig(this);
        this.messages = new Messages(this);
        this.statsService = new StatsService(this);
        this.rewardService = new RewardService(this);
        this.worldService = new SlimeWorldService(this);
        this.itemFactory = new ItemFactory(this);
        this.structureService = new StructureService(this);
        this.missileService = new MissileService(this, this.structureService);
        this.lobbyDisplayService = new LobbyDisplayService(this);
        this.gameManager = new GameManager(this);

        registerListeners();
        registerCommands();

        // Single global ticker; each running game does its own (cheap) per-tick work.
        Bukkit.getScheduler().runTaskTimer(this, this.gameManager::tick, 1L, 1L);

        this.statsService.loadAsync();
        this.rewardService.setupEconomy();
        PlaceholderHook.register(this);

        HmwApi.init(this.gameManager);

        this.worldService.logTemplateStatus();

        getLogger().info("Honey Missile Wars enabled. Original map by Chopper2112, Supersette and kruthers.");
    }

    @Override
    public void onDisable() {
        // Docs (common_issues.md): save worlds with ASP API, then unload with save=false,
        // never Bukkit.unloadWorld(name, true) inside onDisable.
        if (this.gameManager != null) {
            this.gameManager.shutdown();
        }
        if (this.statsService != null) {
            this.statsService.saveNow();
        }
        if (this.worldService != null) {
            this.worldService.shutdown();
        }
        instance = null;
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new LobbyListener(this), this);
        pm.registerEvents(new MissileListener(this), this);
        pm.registerEvents(new ProjectileListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new DamageListener(this), this);
    }

    /**
     * BungeeCord plugin channel. Outgoing: return-server connects. Incoming:
     * arena preferences forwarded by the HMWLobby companion plugin
     * ("Forward" subchannel "HMW", payload = player UUID + arena id).
     */
    private void registerBungeeChannel() {
        var messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, "BungeeCord");
        messenger.registerIncomingPluginChannel(this, "BungeeCord", (channel, player, message) -> {
            try {
                var in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(message));
                if (!"Forward".equals(in.readUTF())) {
                    return;
                }
                in.readUTF(); // target server (we only listen on the game server anyway)
                byte[] data = new byte[in.readShort()];
                in.readFully(data);
                var inner = new java.io.DataInputStream(new java.io.ByteArrayInputStream(data));
                if (!"HMW".equals(inner.readUTF())) {
                    return;
                }
                java.util.UUID playerId = java.util.UUID.fromString(inner.readUTF());
                String arena = inner.readUTF();
                this.gameManager().receiveLobbyForward(playerId, arena);
            } catch (Exception ignored) {
                // not our message format
            }
        });
    }

    private void registerCommands() {
        PluginCommand hmw = getCommand("hmw");
        if (hmw != null) {
            HmwCommand executor = new HmwCommand(this);
            hmw.setExecutor(executor);
            hmw.setTabCompleter(executor);
        }
        PluginCommand admin = getCommand("hmwadmin");
        if (admin != null) {
            HmwAdminCommand executor = new HmwAdminCommand(this);
            admin.setExecutor(executor);
            admin.setTabCompleter(executor);
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.config.reload();
    }

    public HmwConfig hmwConfig() {
        return this.config;
    }

    public Messages messages() {
        return this.messages;
    }

    public SlimeWorldService worldService() {
        return this.worldService;
    }

    public ItemFactory itemFactory() {
        return this.itemFactory;
    }

    public StructureService structureService() {
        return this.structureService;
    }

    public MissileService missileService() {
        return this.missileService;
    }

    public LobbyDisplayService lobbyDisplayService() {
        return this.lobbyDisplayService;
    }

    public GameManager gameManager() {
        return this.gameManager;
    }

    public RewardService rewardService() {
        return this.rewardService;
    }

    public StatsService statsService() {
        return this.statsService;
    }
}
