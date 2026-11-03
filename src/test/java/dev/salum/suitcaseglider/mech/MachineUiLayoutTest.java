package dev.salum.suitcaseglider.mech;
import dev.salum.suitcaseglider.machine.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MachineUiLayoutTest {
    @Test void allTenLayoutsFitVanillaContainerWithoutOverlappingSlots(){
        for(String kind:MachineCatalog.ALL.keySet()){
            var spec=MachineUiSpec.get(kind);assertTrue(spec.inputs()<=3&&spec.outputs()<=2);
            java.util.Set<Integer> storage=new java.util.HashSet<>();
            for(int i=0;i<spec.visibleSlots();i++){
                assertTrue(storage.add(spec.storageSlot(i)));assertTrue(spec.x(i)>=24&&spec.x(i)+16<=166);assertTrue(spec.y(i)>=22&&spec.y(i)+16<=70);
                for(int j=0;j<i;j++)assertTrue(Math.abs(spec.x(i)-spec.x(j))>=18||Math.abs(spec.y(i)-spec.y(j))>=18,kind);
            }
        }
    }
    @Test void separatorReallyHasTwoIndependentOutputsAndUtilitiesDoNotInventMaterialSlots(){
        var separator=MachineUiSpec.get("air_separator");assertEquals(1,separator.inputs());assertEquals(2,separator.outputs());assertEquals(3,separator.storageSlot(1));assertEquals(4,separator.storageSlot(2));
        assertEquals(0,MachineUiSpec.get("air_compressor").visibleSlots());assertEquals(0,MachineUiSpec.get("diesel_generator").visibleSlots());
    }
}
