package top.sparkpixel.hmw.game;

/** Lifecycle of one Game instance. */
public enum GamePhase {
    /** In the map lobby; team pads + options signs + start button are active. */
    WAITING,
    /** Start pressed: countdown running while the arena regenerates in place. */
    STARTING,
    /** The battle itself. */
    RUNNING,
    /** Win decided: fireworks + return to hub. */
    ENDING
}
