package dev.salum.suitcaseglider.cable;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class CableItem extends Item {
    public CableItem(Properties p){super(p);}
    private void message(Player p,String key,Object...args){p.displayClientMessage(Component.translatable("message.suitcaseglider.cable."+key,args),true);}
    private InteractionResult toggle(Level l,Player p,ItemStack stack){if(!l.isClientSide){var tag=stack.getOrCreateTag();tag.putBoolean("Floor",!tag.getBoolean("Floor"));tag.remove("Start");message(p,tag.getBoolean("Floor")?"floor":"air");}return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){ItemStack stack=p.getItemInHand(hand);if(p.isShiftKeyDown()){toggle(l,p,stack);return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}return InteractionResultHolder.pass(stack);}
    @Override public InteractionResult useOn(UseOnContext c){
        Player p=c.getPlayer();if(p==null||!p.mayBuild())return InteractionResult.FAIL;
        if(p.isShiftKeyDown())return toggle(c.getLevel(),p,c.getItemInHand());
        if(!(c.getLevel() instanceof ServerLevel l))return InteractionResult.SUCCESS;
        var stack=c.getItemInHand();var tag=stack.getOrCreateTag();BlockPos end=CableWorld.endpoint(l,c.getClickedPos());
        if(end==null){message(p,"endpoint");return InteractionResult.FAIL;}
        if(!tag.contains("Start")||!tag.getString("Dimension").equals(l.dimension().location().toString())){tag.putLong("Start",end.asLong());tag.putString("Dimension",l.dimension().location().toString());message(p,"start",end.getX(),end.getY(),end.getZ());return InteractionResult.SUCCESS;}
        BlockPos start=BlockPos.of(tag.getLong("Start"));if(!l.hasChunkAt(start)||CableWorld.endpoint(l,start)==null){tag.remove("Start");message(p,"lost");return InteractionResult.FAIL;}
        var world=CableWorld.get(l);if(!world.available(start,end)){message(p,"duplicate");return InteractionResult.FAIL;}
        var points=world.route(l,start,end,tag.getBoolean("Floor"));if(points.isEmpty()){message(p,"route");return InteractionResult.FAIL;}
        int cost=Math.max(1,(int)Math.ceil(CableRoutes.length(points)));int count=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(item.is(this))count+=item.getCount();}
        if(!p.isCreative()&&count<cost){message(p,"cost",cost);return InteractionResult.FAIL;}
        boolean floor=tag.getBoolean("Floor");tag.remove("Start");
        if(!p.isCreative()){int remaining=cost;for(int i=0;i<p.getInventory().getContainerSize()&&remaining>0;i++){var item=p.getInventory().getItem(i);if(item.is(this)){int n=Math.min(remaining,item.getCount());item.shrink(n);remaining-=n;}}}
        world.add(new CableWorld.Link(UUID.randomUUID(),start,end,floor,cost,List.copyOf(points)));CableNetwork.broadcast(l);message(p,"connected",cost);return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){lines.add(Component.translatable("message.suitcaseglider.cable."+(s.getOrCreateTag().getBoolean("Floor")?"floor":"air")));lines.add(Component.translatable("message.suitcaseglider.cable.help"));}
}
