package dev.salum.suitcaseglider;

/** Pure rules shared by the game controller and regression tests. */
public final class FlightPolicy {
    private FlightPolicy() {}
    public static boolean dangerous(double fallen, double clearance, double velocityY,
                                    boolean grounded, boolean safeLanding, boolean flying, int jumpBonus) {
        double total = Math.max(0, fallen) + Math.max(0, clearance);
        return !grounded && !safeLanding && !flying && velocityY < -0.03
                && total >= 3.75 && Math.ceil(total - 3.0 - Math.max(0, jumpBonus)) > 0;
    }
    public static double sink(int soaring, boolean deploying) {
        return (deploying ? .055 : .12) / (1 + .25 * Math.max(0, Math.min(3, soaring)));
    }
    public static boolean airbornePhase(int phase) { return phase == 1 || phase == 2; }
}
