package dev.salum.suitcaseglider.cable;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Bounded A* over actual supporting surfaces, never over unloaded terrain. */
public final class CableRoutes {
    public static final int MAX_LENGTH=32,MAX_VISITS=4096;
    public record Surface(BlockPos support,double y) {
        public Vec3 point(){return new Vec3(support.getX()+.5,y+.045,support.getZ()+.5);}
    }
    public interface Terrain {
        Surface surface(int x,int z,double nearY);
        boolean clear(Vec3 a,Vec3 b);
    }
    public static List<Vec3> air(Vec3 a,Vec3 b,Terrain terrain){
        if(a.distanceTo(b)>MAX_LENGTH)return List.of();
        int n=Math.max(8,(int)Math.ceil(a.distanceTo(b)*8));var out=new ArrayList<Vec3>();
        for(int i=0;i<=n;i++){double t=(double)i/n;out.add(a.lerp(b,t).add(0,-4*t*(1-t)*Math.min(1.25,a.distanceTo(b)*.10),0));}
        return valid(out,terrain)&&length(out)<=MAX_LENGTH?out:List.of();
    }
    private record Search(Surface node,double score){}
    public static List<Vec3> floor(Vec3 a,Vec3 b,Terrain terrain){
        if(a.distanceTo(b)>MAX_LENGTH)return List.of();
        Surface start=terrain.surface((int)Math.floor(a.x),(int)Math.floor(a.z),a.y),end=terrain.surface((int)Math.floor(b.x),(int)Math.floor(b.z),b.y);
        if(start==null||end==null)return List.of();
        var open=new PriorityQueue<Search>(Comparator.comparingDouble(Search::score));var costs=new HashMap<Surface,Double>();var prev=new HashMap<Surface,Surface>();var closed=new HashSet<Surface>();
        costs.put(start,0d);open.add(new Search(start,0));
        while(!open.isEmpty()&&closed.size()<MAX_VISITS){
            Surface s=open.remove().node();if(!closed.add(s))continue;
            if(s.equals(end)){
                var nodes=new ArrayList<Surface>();for(Surface p=end;p!=null;p=prev.get(p))nodes.add(p);Collections.reverse(nodes);
                var points=new ArrayList<Vec3>();points.add(a);points.add(new Vec3(a.x,start.point().y,a.z));points.add(start.point());
                for(int i=1;i<nodes.size();i++)points.addAll(step(nodes.get(i-1),nodes.get(i)));
                points.add(new Vec3(b.x,end.point().y,b.z));points.add(b);
                return valid(points,terrain)&&length(points)<=MAX_LENGTH?points:List.of();
            }
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                int x=s.support.getX()+d[0],z=s.support.getZ()+d[1];
                if(Math.abs(x-start.support.getX())>MAX_LENGTH||Math.abs(z-start.support.getZ())>MAX_LENGTH)continue;
                Surface next=terrain.surface(x,z,s.y+.99);if(next==null||Math.abs(next.y-s.y)>1.001)continue;
                var segment=new ArrayList<Vec3>();segment.add(s.point());segment.addAll(step(s,next));if(!valid(segment,terrain))continue;
                double g=costs.get(s)+length(segment);if(g>MAX_LENGTH||g>=costs.getOrDefault(next,Double.POSITIVE_INFINITY))continue;
                costs.put(next,g);prev.put(next,s);open.add(new Search(next,g+next.point().distanceTo(end.point())));
            }
        }return List.of();
    }
    private static List<Vec3> step(Surface a,Surface b){
        Vec3 p=a.point(),q=b.point();if(Math.abs(p.y-q.y)<.001)return List.of(q);
        Vec3 dir=new Vec3(q.x-p.x,0,q.z-p.z).normalize();
        // Rise on the low side of a block edge, so the cable hugs the riser without cutting through it.
        double shift=p.y<q.y?-.03:.03;Vec3 edge=p.lerp(q,.5).add(dir.scale(shift));
        return List.of(new Vec3(edge.x,p.y,edge.z),new Vec3(edge.x,q.y,edge.z),q);
    }
    public static boolean valid(List<Vec3> p,Terrain terrain){for(int i=1;i<p.size();i++)if(!terrain.clear(p.get(i-1),p.get(i)))return false;return !p.isEmpty();}
    public static double length(List<Vec3> p){double length=0;for(int i=1;i<p.size();i++)length+=p.get(i-1).distanceTo(p.get(i));return length;}
}
