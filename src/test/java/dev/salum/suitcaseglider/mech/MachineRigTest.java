package dev.salum.suitcaseglider.mech;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MachineRigTest {
    @Test void bothApprovedModelsKeepTheirTexturesBonesAndClips(){
        for(String name:new String[]{"lathe","crusher"}){
            var rig=MechRig.load("/assets/suitcaseglider/rigs/"+name+".json");assertEquals(name.equals("lathe")?10:4,rig.clips.size());
            assertEquals(name.equals("lathe")?3:6,rig.textures.size());assertTrue(rig.faces.size()>700);
            assertTrue(rig.bones.stream().anyMatch(b->b.name().equals("PORT_INPUT_X_NEG")));
            assertTrue(rig.bones.stream().anyMatch(b->b.name().equals("PORT_OUTPUT_X_POS")));
            assertTrue(rig.bones.stream().anyMatch(b->b.name().equals("PORT_POWER_Z_POS")));
            for(var texture:rig.textures)assertNotNull(MechRig.class.getResourceAsStream("/assets/"+texture.resource().replace(':','/')));
            for(var face:rig.faces){assertTrue(face.texture()>=0&&face.texture()<rig.textures.size());for(var vertex:face.vertices())for(float n:vertex)assertTrue(Float.isFinite(n));}
            for(var entry:rig.clips.entrySet())for(int i=0;i<=8;i++){
                var pose=rig.pose(entry.getKey(),entry.getValue().length()*i/8,entry.getKey(),0,1);
                for(var matrix:pose.bones){float[] values=new float[16];matrix.get(values);for(float n:values)assertTrue(Float.isFinite(n),entry.getKey());}
            }
        }
    }
    @Test void crusherPreviewInputCubeIsNotBakedIntoGeometry(){
        var rig=MechRig.load("/assets/suitcaseglider/rigs/crusher.json");int preview=rig.boneIndex("PROCESS_INPUT_BLOCK");
        assertFalse(rig.faces.stream().anyMatch(f->rig.descendant(f.bone(),preview)));
    }
}
