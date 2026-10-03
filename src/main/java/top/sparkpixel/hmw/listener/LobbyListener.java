package top.sparkpixel.hmw.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.arena.ArenaVariant;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.GamePhase;
import top.sparkpixel.hmw.game.PlayerSession;
import top.sparkpixel.hmw.game.Team;
import top.sparkpixel.hmw.item.MissileSet;

/**
 * Lobby interactions: team pads, start button, option signs, settings-room and
 * tunnel teleports, speed pads. Ports chop:lobbyloop + chop:lobby/options/*.
 */
public final class LobbyListener implements Listener {

    private final HoneyMissileWarsPlugin plugin;

    public LobbyListener(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // movement hot zones
    // ------------------------------------------------------------------

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Game game = this.plugin.gameManager().gameOf(player);
        if (game == null) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        if (game.phase() != GamePhase.WAITING) {
            return;
        }

        // team pads (block under the feet, ~ ~-.5 ~ in the datapack)
        Block below = to.getBlock().getRelative(BlockFace.DOWN);
        PlayerSession session = game.session(player);
        if (session != null) {
            Material floor = below.getType();
            if (floor == Material.RED_TERRACOTTA) {
                if (session.team != Team.RED) {
                    game.joinTeam(player, Team.RED);
                }
            } else if (floor == Material.LIME_TERRACOTTA) {
                if (session.team != Team.GREEN) {
                    game.joinTeam(player, Team.GREEN);
                }
            } else if (floor == Material.LIGHT_GRAY_TERRACOTTA) {
                if (session.team != null) {
                    game.leaveTeam(player);
                }
            } else if (floor == Material.BLUE_STAINED_GLASS) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SPEED, 20, 7, true, false, false));
            } else if (floor == Material.YELLOW_STAINED_GLASS) {
                player.spawnParticle(Particle.DUST, to.clone().add(0, 0.2, 0), 8,
                        0.2, 0.1, 0.2, 0.5, new Particle.DustOptions(org.bukkit.Color.fromRGB(0xFFA500), 1.0f));
            }
        }

        // settings room / lobby teleports
        int x = to.getBlockX();
        int y = to.getBlockY();
        int z = to.getBlockZ();
        if ((x == 6 || x == -6) && (y == 72 || y == 73) && (z == 8 || z == 9)) {
            teleportWithFeedback(player, new Location(game.world(), 0.5, 68, -22.5, 180f, 0f));
        } else if ((x == 7 || x == -7) && (y == 68 || y == 69) && z == -28) {
            teleportWithFeedback(player, new Location(game.world(), 0.5, 72, 0.5,
                    player.getLocation().getYaw(), 0f));
        } else if (y >= 46 && y <= 56 && z >= -8 && z <= 5) {
            if (x >= -90 && x <= -77) {
                tunnel(player, game, 168);
            } else if (x >= 77 && x <= 90) {
                tunnel(player, game, -168);
            }
        }
    }

    private void tunnel(Player player, Game game, int dx) {
        player.teleport(new Location(game.world(),
                player.getLocation().getX() + dx, 60, player.getLocation().getZ(),
                player.getLocation().getYaw(), player.getLocation().getPitch()));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.LEVITATION, 40, 9, true, false, false));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.spawnParticle(Particle.PORTAL, player.getLocation(), 60, 0.5, 1, 0.5, 0.5);
    }

    private void teleportWithFeedback(Player player, Location target) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.0f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0, 0.1, 0), 100,
                1, 0, 1, 1, new Particle.DustOptions(org.bukkit.Color.fromRGB(0xFFFF00), 1.0f));
        player.teleport(target);
    }

    // ------------------------------------------------------------------
    // start button + option signs
    // ------------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        Game game = this.plugin.gameManager().gameOf(player);
        if (game == null) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // start button (0 73 9 spruce_button)
        if (block.getType() == Material.SPRUCE_BUTTON
                && block.getX() == 0 && block.getY() == 73 && block.getZ() == 9) {
            event.setCancelled(true);
            if (game.phase() == GamePhase.WAITING) {
                String error = game.startCountdown();
                if (error != null) {
                    player.sendActionBar(this.plugin.messages().mini(error));
                }
            } else if (game.phase() == GamePhase.STARTING) {
                game.cancelCountdown();
            }
            return;
        }

        // option signs in the settings room
        if (game.phase() == GamePhase.WAITING
                && (block.getState() instanceof Sign sign)) {
            if (block.getY() < 60 || block.getZ() > -15) {
                return; // only the settings-room signs
            }
            event.setCancelled(true);
            handleOptionSign(game, player, sign);
        }
    }

    private void handleOptionSign(Game game, Player player, Sign sign) {
        String firstLine = stripCodes(sign.getSide(Side.FRONT).getLine(0)).toLowerCase().trim();
        if (firstLine.isEmpty()) {
            return;
        }
        for (SignOption option : SignOption.values()) {
            boolean matched = false;
            for (String alias : option.aliases) {
                if (firstLine.contains(alias)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                continue;
            }
            option.cycle(game);
            writeSign(sign, option.label, option.valueText(game));
            this.plugin.lobbyDisplayService().playOptionFeedback(player);
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    option.description(), net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            if (option == SignOption.MISSILE_SET) {
                this.plugin.lobbyDisplayService().refresh(game, game.settings().missileSet);
            }
            return;
        }
    }

    private String stripCodes(String text) {
        return text.replace("\u00a7", "&").replaceAll("&[0-9a-fk-or]", "").trim();
    }

    private void writeSign(Sign sign, String label, String value) {
        var side = sign.getSide(Side.FRONT);
        side.setLine(0, "\u00a7l" + label);
        side.setLine(1, value);
        side.setLine(2, "");
        side.setLine(3, "\u00a7o(right-click)");
        sign.update();
    }

    /** The 12 lobby options (chop:lobby/options/*). */
    private enum SignOption {
        ARENA("Arena:", "The arena layout used for the next game") {
            @Override
            void cycle(Game game) {
                ArenaVariant[] values = ArenaVariant.values();
                game.settings().arena = values[(game.settings().arena.ordinal() + 1) % values.length];
            }

            @Override
            String valueText(Game game) {
                return game.settings().arena.displayName();
            }
        },
        MISSILE_SET("Missile Set:", "The missiles and items that can be received") {
            @Override
            void cycle(Game game) {
                MissileSet[] values = MissileSet.values();
                // display order in the map: ALL first, then CLASSIC..SWITCH
                MissileSet current = game.settings().missileSet;
                int next = current.ordinal() - 1;
                if (next < 0) {
                    next = values.length - 1;
                }
                game.settings().missileSet = values[next];
            }

            @Override
            String valueText(Game game) {
                return game.settings().missileSet.displayName();
            }
        },
        ITEM_RATE("Item Rate:", "The rate at which players are given items") {
            @Override
            void cycle(Game game) {
                game.settings().itemRate = (game.settings().itemRate + 1) % 4;
            }

            @Override
            String valueText(Game game) {
                return switch (game.settings().itemRate) {
                    case 3 -> "Fast";
                    case 1 -> "Slow";
                    case 0 -> "Ultra Slow";
                    default -> "Normal";
                };
            }
        },
        RESPAWN_TIME("Respawn Time:", "How long players wait to respawn after dying") {
            @Override
            void cycle(Game game) {
                game.settings().respawnTime = (game.settings().respawnTime + 1) % 4;
            }

            @Override
            String valueText(Game game) {
                return switch (game.settings().respawnTime) {
                    case 0 -> "\u00a74Instant";
                    case 1 -> "3 sec";
                    case 2 -> "5 sec";
                    default -> "10 sec";
                };
            }
        },
        JUMP_BOOST("Jump Boost:", "Helps you jump onto missiles") {
            @Override
            void cycle(Game game) {
                game.settings().jumpBoost = (game.settings().jumpBoost + 1) % 4;
            }

            @Override
            String valueText(Game game) {
                return game.settings().jumpBoost == 0 ? "\u00a7cDisabled"
                        : String.valueOf(game.settings().jumpBoost * 2);
            }
        },
        SPEED_BOOST("Speed Boost:", "Move faster in the arena") {
            @Override
            void cycle(Game game) {
                game.settings().speedBoost = (game.settings().speedBoost + 1) % 4;
            }

            @Override
            String valueText(Game game) {
                return game.settings().speedBoost == 0 ? "\u00a7cDisabled"
                        : switch (game.settings().speedBoost) {
                            case 1 -> "1";
                            case 2 -> "2";
                            default -> "4";
                        };
            }
        },
        PICKAXE("Pickaxe:", "A permanent pickaxe tool to break harder blocks") {
            @Override
            void cycle(Game game) {
                game.settings().pickaxe = !game.settings().pickaxe;
            }

            @Override
            String valueText(Game game) {
                return game.settings().pickaxe ? "\u00a7aEnabled" : "\u00a7cDisabled";
            }
        },
        FALL_DAMAGE("Fall Damage:", "Toggle fall damage") {
            @Override
            void cycle(Game game) {
                game.settings().fallDamage = !game.settings().fallDamage;
            }

            @Override
            String valueText(Game game) {
                return game.settings().fallDamage ? "\u00a7aEnabled" : "\u00a7cDisabled";
            }
        },
        EXPLODING_ARROWS("TNT Arrows:", "Arrows explode on impact",
                "exploding arrows", "tnt arrows", "explosive arrows") {
            @Override
            void cycle(Game game) {
                game.settings().explodingArrows = !game.settings().explodingArrows;
            }

            @Override
            String valueText(Game game) {
                return game.settings().explodingArrows ? "\u00a7aEnabled" : "\u00a7cDisabled";
            }
        },
        ELYTRA("Elytra:", "Everyone gets a 3-second elytra") {
            @Override
            void cycle(Game game) {
                game.settings().elytra = !game.settings().elytra;
            }

            @Override
            String valueText(Game game) {
                return game.settings().elytra ? "\u00a7aEnabled" : "\u00a7cDisabled";
            }
        },
        ITEM_STACKING("Item Stacking:", "Allow holding more than one of each item",
                "missile stacking", "stacking") {
            @Override
            void cycle(Game game) {
                game.settings().missileStacking = !game.settings().missileStacking;
            }

            @Override
            String valueText(Game game) {
                return game.settings().missileStacking ? "\u00a7aUnlimited Items" : "\u00a7cLimited";
            }
        },
        WALL_MISSILES("Wall Missiles:", "Allow placing missiles inside the enemy walls/portal") {
            @Override
            void cycle(Game game) {
                game.settings().wallMissiles = !game.settings().wallMissiles;
            }

            @Override
            String valueText(Game game) {
                return game.settings().wallMissiles ? "\u00a7aEnabled" : "\u00a7cDisabled";
            }
        };

        final String label;
        private final String description;
        private final String[] aliases;

        SignOption(String label, String description, String... aliases) {
            this.label = label;
            this.description = description;
            String[] all = new String[aliases.length + 1];
            all[0] = label.toLowerCase().replace(":", "");
            System.arraycopy(aliases, 0, all, 1, aliases.length);
            this.aliases = all;
        }

        String description() {
            return this.description;
        }

        abstract void cycle(Game game);

        abstract String valueText(Game game);
    }
}
