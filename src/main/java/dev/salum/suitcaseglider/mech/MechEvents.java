package dev.salum.suitcaseglider.mech;
import dev.salum.suitcaseglider.SuitcaseGlider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class MechEvents {
    @SubscribeEvent public static void dismount(EntityMountEvent event){
        if(event.isDismounting()&&event.getEntityBeingMounted() instanceof MechAntEntity ant&&event.getEntityMounting() instanceof Player player&&ant.deferDismount(player))event.setCanceled(true);
    }
}
