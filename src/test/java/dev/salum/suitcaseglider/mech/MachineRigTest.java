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
    @Test void everyMachineHasResourcesValidClipsAndFiniteAnimatedBounds(){
        assertEquals(10,dev.salum.suitcaseglider.machine.MachineCatalog.ALL.size());
        for(var entry:dev.salum.suitcaseglider.machine.MachineCatalog.ALL.entrySet()){
            var spec=entry.getValue();var rig=MechRig.load("/assets/suitcaseglider/rigs/"+entry.getKey()+".json");
            assertTrue(spec.width()>=2&&spec.width()<=3);assertTrue(spec.layers()>=1&&spec.layers()<=3);
            assertTrue(rig.clips.containsKey(spec.off()),entry.getKey());assertTrue(rig.clips.containsKey(spec.on()),entry.getKey());
            if(!spec.start().isEmpty())assertTrue(rig.clips.containsKey(spec.start()));if(!spec.stop().isEmpty())assertTrue(rig.clips.containsKey(spec.stop()));
            for(var t:rig.textures)assertNotNull(MechRig.class.getResourceAsStream("/assets/"+t.resource().replace(':','/')));
            for(var f:rig.faces){assertTrue(f.texture()>=0&&f.texture()<rig.textures.size());for(var uv:f.uv())for(float n:uv)assertTrue(Float.isFinite(n));}
            for(var clip:rig.clips.entrySet())for(int i=0;i<=4;i++){
                var pose=rig.pose(clip.getKey(),clip.getValue().length()*i/4,clip.getKey(),0,1);
                for(var matrix:pose.bones){float[] m=new float[16];matrix.get(m);for(float v:m)assertTrue(Float.isFinite(v),entry.getKey()+clip.getKey());}
            }
        }
    }
    @Test void fermenterAndMixerPreserveTheirFullLayoutsAndOperatingRig(){
        var catalog=dev.salum.suitcaseglider.machine.MachineCatalog.ALL;
        assertEquals(2,catalog.get("fermenter").depth());assertEquals(3,catalog.get("fermenter").layers());
        assertEquals(1,catalog.get("universal_mixer").depth());assertEquals(2,catalog.get("universal_mixer").layers());
        var fermenter=MechRig.load("/assets/suitcaseglider/rigs/fermenter.json");
        assertTrue(fermenter.anchors.keySet().containsAll(java.util.List.of("fluid_min","fluid_max","power_input")));
        assertTrue(fermenter.faces.stream().noneMatch(f->fermenter.bones.get(f.bone()).name().equals("EDITOR_FLUID_PREVIEW")));
        var pose=fermenter.pose("processing",1,"processing",0,1);assertTrue(pose.anchor("fluid_max").y-pose.anchor("fluid_min").y>1);
        var mixer=MechRig.load("/assets/suitcaseglider/rigs/universal_mixer.json");
        assertTrue(mixer.clips.keySet().containsAll(java.util.List.of("loading","processing","unloading","start","stop")));
        var closed=mixer.pose("loading",0,"loading",0,1);var open=mixer.pose("loading",1.5,"loading",0,1);
        assertTrue(open.anchor("item_input").y>closed.anchor("item_input").y);
    }
    @Test void compactGeneratorKeepsFourOutputsAndTwentyUnitHeight(){
        var spec=dev.salum.suitcaseglider.machine.MachineCatalog.get("diesel_generator");assertEquals(20,spec.height(),.001);
        var rig=MechRig.load("/assets/suitcaseglider/rigs/diesel_generator.json");assertEquals(4,rig.anchors.keySet().stream().filter(n->n.startsWith("power_output_")).count());
        var pose=rig.pose("shutdown",0,"shutdown",0,1);float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(var face:rig.faces)for(var v:face.vertices()){var p=pose.bones[face.bone()].transformPosition(new org.joml.Vector3f(v));min=Math.min(min,p.y);max=Math.max(max,p.y);}
        assertEquals(1.25,max-min,.001);
    }
    @Test void bathUsesNativeWaterVolumeAndCompressorsUseTriangulatedCaps(){
        var bath=MechRig.load("/assets/suitcaseglider/rigs/quenching_bath.json");
        var empty=bath.pose("idle_empty",0,"idle_empty",0,1);var filled=bath.pose("idle_filled",0,"idle_filled",0,1);
        assertTrue(filled.anchor("fluid_max").y-filled.anchor("fluid_min").y>.4);assertTrue(empty.anchor("fluid_max").y-empty.anchor("fluid_min").y<.005);
        assertEquals(3,dev.salum.suitcaseglider.machine.MachineCatalog.get("air_compressor").width());
        var compressor=MechRig.load("/assets/suitcaseglider/rigs/air_compressor.json");assertTrue(compressor.faces.stream().anyMatch(f->java.util.Arrays.equals(f.vertices()[2],f.vertices()[3])));
    }
    @Test void crusherPreviewInputCubeIsNotBakedIntoGeometry(){
        var rig=MechRig.load("/assets/suitcaseglider/rigs/crusher.json");int preview=rig.boneIndex("PROCESS_INPUT_BLOCK");
        assertFalse(rig.faces.stream().anyMatch(f->rig.descendant(f.bone(),preview)));
    }
}
