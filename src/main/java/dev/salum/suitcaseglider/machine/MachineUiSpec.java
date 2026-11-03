package dev.salum.suitcaseglider.machine;

/** Shared slot layout, so client hitboxes, server containers and textures cannot drift apart. */
public record MachineUiSpec(int inputs,int outputs,String symbol) {
    public static MachineUiSpec get(String kind){return switch(kind){
        case "air_compressor" -> new MachineUiSpec(0,0,"compressor");
        case "diesel_generator" -> new MachineUiSpec(0,0,"generator");
        case "universal_mixer" -> new MachineUiSpec(3,1,"mixer");
        case "fermenter" -> new MachineUiSpec(3,2,"fermenter");
        case "quenching_bath" -> new MachineUiSpec(2,1,"bath");
        case "air_separator" -> new MachineUiSpec(1,2,"fan");
        case "crusher" -> new MachineUiSpec(1,1,"crusher");
        case "lathe","rolling_mill","industrial_press" -> new MachineUiSpec(1,1,kind);
        default -> throw new IllegalArgumentException("Unknown machine UI "+kind);
    };}
    public int visibleSlots(){return inputs+outputs;}
    public int storageSlot(int visible){return visible<inputs?visible:3+visible-inputs;}
    public int x(int visible){return visible<inputs?(inputs==1?44:28+visible*18):132;}
    public int y(int visible){return visible<inputs?34:outputs==1?34:24+(visible-inputs)*26;}
}
