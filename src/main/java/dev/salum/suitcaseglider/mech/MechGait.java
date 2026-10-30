package dev.salum.suitcaseglider.mech;

/** Speeds derived from the authored planted-foot stroke, not the much faster horse attribute. */
public final class MechGait {
    public static final double STANCE=.74;
    public static final double TEMPO=3;
    public static final double FORWARD=TEMPO*(6.0/16)/(1.12*STANCE);
    public static final double BACKWARD=TEMPO*(5.2/16)/(1.36*STANCE);
    public static double nativeSpeed(double blocksPerSecond,double friction,double inputScale){
        // LivingEntity ground acceleration and drag: a=speed*0.216/f^3, drag=0.91*f.
        return blocksPerSecond/20*(1-.91*friction)*friction*friction*friction/.216/inputScale;
    }
    public static boolean loop(String clip){return clip.equals("walk")||clip.equals("walk_backward")||clip.equals("ridden_walk")||clip.equals("ridden_walk_backward")||clip.startsWith("turn_")||clip.startsWith("pivot_");}
    public static boolean resting(String clip){return clip.equals("idle")||clip.equals("ridden_idle")||clip.equals("shutdown");}
    public static boolean reverse(String clip){return clip.contains("backward");}
}
