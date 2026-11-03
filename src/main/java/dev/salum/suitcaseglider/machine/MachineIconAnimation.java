package dev.salum.suitcaseglider.machine;

/** Client screen-local clock. Freeze the last displayed frame rather than snapping to frame zero. */
public final class MachineIconAnimation {
    public static final int FRAMES=24;
    private final double framesPerTick;
    private int ticks,lastFrame;
    public MachineIconAnimation(boolean fan){framesPerTick=fan?2.4:2;}
    public void tick(boolean running){if(running)ticks=(ticks+1)%60;}
    public int frame(boolean running,float partial){
        if(running)lastFrame=(int)Math.floor((ticks+Math.max(0,Math.min(1,partial)))*framesPerTick)%FRAMES;
        return lastFrame;
    }
}
