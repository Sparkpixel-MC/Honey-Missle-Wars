package top.sparkpixel.hmw.item;

import top.sparkpixel.hmw.missile.MissileType;

import java.util.List;

/**
 * The six selectable missile pools (chop:game/items/*). One pool entry is
 * rolled per cycle and given to every player that is on a team.
 */
public enum MissileSet {

    ALL("All Missiles", List.of(
            MissileType.BOMBER, MissileType.CLEARER, MissileType.FLAT_HEAD, MissileType.BRIDGE,
            MissileType.BLOCKER, MissileType.SHIELD_MISSILE, MissileType.TANK, MissileType.THUNDER_STRIKE,
            MissileType.MINIBUS, MissileType.TRANSPORT, MissileType.DROPPER,
            MissileType.HONEY_JUGGERNAUT, MissileType.HONEY_TOMAHAWK, MissileType.HONEY_SHIELDBUSTER,
            MissileType.HONEY_LIGHTNING, MissileType.HONEY_GUARDIAN,
            Special.FIREBALL, Special.ARROW, Special.SHIELD, Special.GRENADE,
            MissileType.BULLDOZER, MissileType.TREBUCHET, MissileType.COUNTDOWN, MissileType.UNDERTAKER,
            MissileType.SCORPION, MissileType.TORPEDO, MissileType.HIT_AND_RUN,
            MissileType.ANNIHILATOR, MissileType.DOUBLE_DIP, MissileType.EL_DERECHO,
            MissileType.EPSILON, MissileType.KODACHI)),
    CLASSIC("Classic", List.of(
            MissileType.SHIELDBUSTER, MissileType.TOMAHAWK, MissileType.GUARDIAN, MissileType.JUGGERNAUT,
            MissileType.LIGHTNING, Special.FIREBALL, Special.ARROW, Special.SHIELD)),
    HONEY_CLASSIC("Classic Honey", List.of(
            MissileType.HONEY_SHIELDBUSTER, MissileType.HONEY_TOMAHAWK, MissileType.HONEY_GUARDIAN,
            MissileType.HONEY_JUGGERNAUT, MissileType.HONEY_LIGHTNING,
            Special.FIREBALL, Special.ARROW, Special.SHIELD)),
    EXPLOSIVE("Explosive", List.of(
            MissileType.DROPPER, MissileType.CLEARER, MissileType.BOMBER, MissileType.FLAT_HEAD,
            MissileType.BLOCKER, Special.ARROW, Special.SHIELD, Special.GRENADE)),
    RUSH("Rush", List.of(
            MissileType.THUNDER_STRIKE, MissileType.BRIDGE, MissileType.TANK, MissileType.MINIBUS,
            MissileType.TRANSPORT, Special.FIREBALL, Special.ARROW, Special.GRENADE)),
    SWITCH("Switch", List.of(
            MissileType.BULLDOZER, MissileType.TREBUCHET, MissileType.UNDERTAKER, MissileType.SCORPION,
            MissileType.COUNTDOWN, Special.ARROW, Special.SHIELD, Special.GRENADE, Special.FIREBALL));

    /** Non-missile pool entries (chop:get/item/*). */
    public enum Special {
        FIREBALL, ARROW, SHIELD, GRENADE
    }

    private final String displayName;
    private final List<Object> pool;

    MissileSet(String displayName, List<Object> pool) {
        this.displayName = displayName;
        this.pool = List.copyOf(pool);
    }

    public String displayName() {
        return this.displayName;
    }

    /** Mixed pool of MissileType and Special entries. */
    public List<Object> pool() {
        return this.pool;
    }

    public Object random(java.util.Random random) {
        return this.pool.get(random.nextInt(this.pool.size()));
    }
}
