(()=>{
if(Project.name!=='Blast Furnace'||Outliner.elements.length)throw Error('Use empty Fermenter');
Project.box_uv=false;Project.texture_width=2048;Project.texture_height=2048;
const root=new Group({name:'blast_furnace',origin:[0,0,0]}).init(),groups={},parts=[];
function group(name,origin=[0,0,0],p=root){const g=new Group({name,origin}).addTo(p).init();groups[name]=g;return g;}
const parent=g=>typeof g==='string'?groups[g]:g;
function box(name,a,b,material='vessel',g='vessel_shell',rotation){const e=new Cube({name,from:a,to:b,origin:a.map((x,i)=>(x+b[i])/2),box_uv:false,rotation:rotation||[0,0,0]}).addTo(parent(g)).init();parts.push({e,material});return e;}
function mesh(name,vertices,polys,material,g){const m=new Mesh({name,origin:[0,0,0],vertices:{}}).addTo(parent(g)).init(),keys=m.addVertices(...vertices);for(const p of polys)m.addFaces(new MeshFace(m,{vertices:p.map(i=>keys[i]),uv:{}}));parts.push({e:m,material});return m;}
function axisPoint(axis,a,u,v){return axis==='x'?[a,u,v]:axis==='y'?[v,a,u]:[u,v,a];}
function revolve(name,axis,rings,center,material,g,N=24,caps=true){
 const radial=axis==='x'?[center[1],center[2]]:axis==='y'?[center[2],center[0]]:[center[0],center[1]],vs=[],fs=[];
 for(const [a,r] of rings)for(let i=0;i<N;i++){const t=i*2*Math.PI/N;vs.push(axisPoint(axis,a,radial[0]+r*Math.cos(t),radial[1]+r*Math.sin(t)));}
 for(let ring=0;ring<rings.length-1;ring++)for(let i=0;i<N;i++){const j=(i+1)%N,a=ring*N,b=(ring+1)*N;fs.push([a+i,a+j,b+j,b+i]);}
 if(caps){vs.push(axisPoint(axis,rings[0][0],...radial),axisPoint(axis,rings.at(-1)[0],...radial));
 const a=vs.length-2,b=a+1,last=(rings.length-1)*N;
 for(let i=0;i<N;i++){const j=(i+1)%N;fs.push([a,j,i],[b,last+i,last+j]);}}
 return mesh(name,vs,fs,material,g);
}
function cylinder(name,axis,a,b,r,center,material,g,N=16){return revolve(name,axis,[[a,r],[b,r]],center,material,g,N);}
function bevel(name,a,b,bevel,material,g,axis='y'){
 const uv=axis==='x'?[a[1],a[2],b[1],b[2]]:axis==='y'?[a[2],a[0],b[2],b[0]]:[a[0],a[1],b[0],b[1]],i=axis==='x'?0:axis==='y'?1:2;
 const [u,v,U,V]=uv,d=Math.min(bevel,(U-u)/3,(V-v)/3),profile=[[u+d,v],[U-d,v],[U,v+d],[U,V-d],[U-d,V],[u+d,V],[u,V-d],[u,v+d]],vs=[],fs=[],N=8;
 for(const pos of [a[i],b[i]])for(const [u,v]of profile)vs.push(axisPoint(axis,pos,u,v));
 for(let j=0;j<N;j++){const k=(j+1)%N;fs.push([j,k,N+k,N+j]);}
 for(let j=1;j<N-1;j++)fs.push([0,j+1,j],[N,N+j,N+j+1]);
 return mesh(name,vs,fs,material,g);
}



function ring(name,axis,c,a,b,R,r,mat,g,N=24){return revolve(name,axis,[[a,R],[b,R],[b,r],[a,r],[a,R]],c,mat,g,N,false);}
function fastener(name,axis,c,a,b,r,g){const s=b>a?1:-1,q=a+s*.08;cylinder(name+' washer',axis,Math.min(a,q),Math.max(a,q),r*1.4,c,'dark_steel',g,12);cylinder(name+' hex',axis,Math.min(q,b),Math.max(q,b),r,c,'steel',g,6);}
function path(name,pts,r,mat,g,N=12){
 const vs=[],fs=[];let prev=null;
 for(let j=0;j<pts.length;j++){
  const t=new THREE.Vector3(...pts[Math.min(j+1,pts.length-1)]).sub(new THREE.Vector3(...pts[Math.max(0,j-1)])).normalize();let u=prev?prev.clone().addScaledVector(t,-prev.dot(t)).normalize():new THREE.Vector3(0,0,1).cross(t).normalize();if(u.length()<.01)u=new THREE.Vector3(1,0,0);const v=new THREE.Vector3().crossVectors(t,u).normalize();prev=u;
  for(let i=0;i<N;i++)vs.push(new THREE.Vector3(...pts[j]).addScaledVector(u,r*Math.cos(i*2*Math.PI/N)).addScaledVector(v,r*Math.sin(i*2*Math.PI/N)).toArray());
 }
 for(let j=0;j<pts.length-1;j++)for(let i=0;i<N;i++){const k=(i+1)%N;fs.push([j*N+i,j*N+k,(j+1)*N+k,(j+1)*N+i]);}fs.push(Array.from({length:N},(_,i)=>N-1-i),Array.from({length:N},(_,i)=>(pts.length-1)*N+i));return mesh(name,vs,fs,mat,g);
}
for(const n of ['foundation','hearth','shaft','straps','air_ring','tuyeres','hopper','preheater','blower','gas_offtake','console'])group(n);
const lidL=group('hopper_lid_left',[18,71,25],groups.hopper),lidR=group('hopper_lid_right',[18,71,25],groups.hopper),fan=group('blower_rotor',[40.5,11.8,15.4],groups.blower),heat=group('heat_windows',[18,9,12],groups.hearth);
// All installed parts, including service equipment, fit 48 x 48 x 72.
for(let x=0;x<3;x++)for(let z=0;z<3;z++)bevel('Foundation slab '+x+' '+z,[x*16+.07,0,z*16+.07],[(x+1)*16-.07,3.8,(z+1)*16-.07],.45,'frame','foundation');
for(const x of [1,44])box('Base rail '+x,[x,3.8,3],[x+3,5,45],'dark_steel','foundation');
revolve('Refractory hearth','y',[[4,12.8],[6,13.5],[19.8,13.5],[21.5,12.8]],[18,0,25],'vessel','hearth',16);
ring('Hearth foot binding','y',[18,0,25],4.3,6,13.9,12.9,'dark_steel','hearth',16);
ring('Hearth crown binding','y',[18,0,25],19,21,13.9,12.9,'dark_steel','hearth',16);
const profile=[[20,11.6],[23,13.5],[32,14.4],[47,10.3],[56,8.5],[58,8.5]];
revolve('Octagonal furnace shaft','y',profile,[18,0,25],'dark_steel','shaft',8);
for(const [y,r]of [[22,13.4],[32,14.45],[46.9,10.4],[56.2,8.8]])ring('Circumferential seam '+y,'y',[18,0,25],y,y+.5,r+.12,r-.25,'frame','shaft',8);
// Continuous bolted staves follow the taper instead of floating cuboids.
for(let i=0;i<8;i++){
 const t=(i+.5)*Math.PI/4,w=.75;
 const vs=[];for(const [y,r]of profile)for(const a of [t-w/(r+1),t+w/(r+1)])vs.push([18+(r+.28)*Math.sin(a),y,25+(r+.28)*Math.cos(a)]);
 const polys=[];for(let j=0;j<profile.length-1;j++)polys.push([j*2,j*2+1,j*2+3,j*2+2]);mesh('Tapered reinforcing stave '+i,vs,polys,'frame','straps');
 for(const [y,r]of [[24,13.65],[31,14.6],[45,10.9],[54,8.9]]){const p=[18+(r+.3)*Math.sin(t),y,25+(r+.3)*Math.cos(t)];const e=bevel('Stave rivet '+i+' '+y,p.map((v,k)=>v-(k===1?.28:.25)),p.map((v,k)=>v+(k===1?.28:.25)),.12,'steel','straps');}
}
ring('Neck collar','y',[18,0,25],56.8,59,9.3,7.4,'frame','shaft',8);
cylinder('Sealed throat','y',59,62.7,6.7,[18,0,25],'dark_steel','hopper',8);
ring('Throat flange','y',[18,0,25],61,62,8,6,'frame','hopper',8);
// Four-sided flared loading bunker with a closed split cover.
const hv=[];for(const [y,r]of [[62,6],[65,11.8],[70.5,11.8]])for(const [x,z]of [[-1,-1],[1,-1],[1,1],[-1,1]])hv.push([18+x*r,y,25+z*r]);
mesh('Closed loading bunker',hv,[[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7],[4,5,9,8],[5,6,10,9],[6,7,11,10],[7,4,8,11]],'frame','hopper');
bevel('Sliding lid L',[5.5,70.5,12.5],[17.95,72,37.5],.35,'frame',lidL);bevel('Sliding lid R',[18.05,70.5,12.5],[30.5,72,37.5],.35,'frame',lidR);
for(const x of [7,29])for(const z of [14,36])fastener('Lid bolt','y',[x,0,z],71.9,72,.3,'hopper');
for(const y of [63.1,65.2])bevel('Bunker lock '+y,[15.6,y,12.8],[20.4,y+1.25,13.4],.18,'brass','hopper');
ring('Octagonal hot blast manifold','y',[18,0,25],28.6,32,16.4,14.75,'frame','air_ring',8);
for(const t of [-Math.PI/4,Math.PI/4,Math.PI*3/4,Math.PI*5/4]){
 const x=18+16*Math.sin(t),z=25+16*Math.cos(t);bevel('Manifold brass clamp',[x-1.2,28.2,z-1.2],[x+1.2,32.4,z+1.2],.25,'brass','air_ring');}
for(const x of [11,25]){
 path('Hot blast downpipe '+x,[[x,29.8,12],[x,26,12],[x,23,14],[x,20,14.2]],1.35,'steel','tuyeres');
 ring('Tuyere clamp '+x,'y',[x,0,14.2],20.3,21.4,1.8,1.32,'brass','tuyeres',12);
 bevel('Tuyere shield '+x,[x-2,17.8,12.7],[x+2,20.1,15.1],.3,'dark_steel','tuyeres');
}
for(const [j,x]of [[0,12],[1,24]]){
 bevel('Tapping port frame '+j,[x-2.6,6.4,11],[x+2.6,11.8,12.6],.35,'frame','hearth');
 box('Dark tapping opening '+j,[x-1.6,7.2,10.9],[x+1.6,10.5,11.1],'rubber','hearth');
 box('Hot tapping window '+j,[x-1.45,7.3,10.78],[x+1.45,10.4,10.9],'yellow',heat);
 path('Tapping trough bottom '+j,[[x,5,3],[x,5.4,6],[x,7.1,11]],1.1,'toolsteel','hearth',4);
 for(const s of [-1,1])path('Trough cheek '+j+' '+s,[[x+s*1.25,5.8,3],[x+s*1.25,6.2,6],[x+s*1.25,7.9,11]],.45,'frame','hearth',4);
}
// Compact framed recuperator, supported directly by its plinth.
for(const x of [35,43.5])box('Preheater leg '+x,[x,4,35],[x+1.5,26,44.5],'dark_steel','preheater');
bevel('Preheater backing',[35,25,35],[45,51,44.5],.7,'dark_steel','preheater');
for(const y of [25,49])bevel('Preheater crossmember '+y,[34.6,y,33.9],[45.4,y+2,45],.35,'frame','preheater');
for(const x of [35,43.5])box('Preheater front stile '+x,[x,27,33.9],[x+1.5,49,35.1],'frame','preheater');
for(let i=0;i<6;i++)cylinder('Heat exchanger tube '+i,'y',27,49,.42,[37.1+i*.95,0,34.8],'brass','preheater',8);
box('Preheater central brace',[36.5,36.5,33.5],[43.5,37.5,34.1],'frame','preheater');
bevel('Recuperator warning plate',[36,25.1,33.5],[44,26.8,33.85],.1,'hazard','preheater');
for(const y of [26,50])for(const x of [35.4,44.6])fastener('Heater bolts','z',[x,y,0],33.9,33.5,.28,'preheater');
// Motor, fan housing, and continuous ducts.
bevel('Blower mounting pedestal',[34,4,16],[47,6.8,31],.5,'frame','blower');
cylinder('Motor endbell','z',21,29.8,4.5,[40.5,11.3,0],'blue','blower',12);
for(const z of [22,23.2,24.4,25.6,26.8,28])ring('Motor cooling fin '+z,'z',[40.5,11.3,0],z,z+.45,4.9,4.4,'blue','blower',12);
bevel('Centrifugal blower housing',[35.4,6.8,15],[45.6,17.3,21.5],1.5,'frame','blower','z');
ring('Fan intake surround','z',[40.5,11.8,0],14.5,15,3.8,2.9,'dark_steel','blower',12);
for(let i=-2;i<=2;i++)box('Fan grille '+i,[37.4,11.6+i*.65,14.25],[43.6,11.85+i*.65,14.5],'dark_steel','blower');
for(let i=0;i<6;i++){const e=box('Impeller blade '+i,[40.2,11.5,15.2],[43.1,12,15.65],'toolsteel',fan);e.origin=[40.5,11.8,15.4];e.rotation=[0,0,i*60];}
path('Blower pressure duct',[[44,15,20],[45,18,24],[45,21,32],[42,24,37]],1.2,'steel','blower');
path('Heated air connection',[[35.7,31,38],[32.5,31,38],[29.5,31,36.2]],1.3,'steel','preheater');
path('Top gas offtake',[[24,59,28],[30,59,32],[35,56,39],[39.5,53,39.5],[39.5,51,39.5]],2.1,'dark_steel','gas_offtake');
ring('Offtake brass union','y',[39.5,0,39.5],52.5,54,2.7,2,'brass','gas_offtake',8);
path('Clean gas vent',[[44,46,42],[45,50,43],[45,58,43]],.8,'steel','gas_offtake');
ring('Gas vent lip','y',[45,0,43],57.5,58.2,1.1,.65,'frame','gas_offtake',8);
bevel('Electrical terminal box',[43.7,9,25],[46.3,13,28.4],.3,'dark_steel','blower');
ring('Power socket','x',[0,11,26.7],46.3,46.9,1.1,.65,'brass','blower',8);
// Integrated sloping console, all controls attached to the front fascia.
bevel('Controller pedestal',[1.5,4,6.5],[8.5,15,13],.55,'dark_steel','console');
const panel=box('Sloping operator console',[1.8,13.8,5.6],[8.2,17,7],'frame','console',[-20,0,0]);
box('Controller screen bezel',[2.4,14.4,5.05],[7.6,16.7,5.65],'rubber','console');
box('Status display',[2.8,14.7,4.98],[7.2,16.4,5.06],'green','console');
for(const [x,mat]of [[3,'green'],[5,'red'],[7,'brass']])bevel('Operator button '+mat,[x-.55,12.1,6],[x+.55,13.2,6.45],.12,mat,'console');
const plaque=box('Furnace identification',[2.4,8.4,6.38],[7.6,10.1,6.52],'plate','console');
// Attachment locators are non-rendered and named for later metallurgy code.
for(const [name,p]of Object.entries({feed:[18,71,25],power:[46.9,11,26.7],iron_out:[12,5,3],slag_out:[24,5,3],blower:[40.5,11.8,15],controller:[5,15,5]}))new Locator({name,position:p}).addTo(root).init();
const faces=[],sub=(a,b)=>a.map((v,i)=>v-b[i]),dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],norm=v=>{let l=Math.hypot(...v)||1;return v.map(x=>x/l);};
const materialMap=new Map(parts.map(p=>[p.e,p.material]));const material=e=>materialMap.get(e)||'steel';
for(const e of Outliner.elements){
 if(!e.faces)continue;
 const mat=material(e);
 if(e instanceof Mesh)for(const f of Object.values(e.faces)){
  const pts3=f.vertices.map(k=>e.vertices[k]),u=norm(sub(pts3[1],pts3[0])),n=norm(cross(sub(pts3[1],pts3[0]),sub(pts3[2],pts3[0]))),v=norm(cross(n,u)),pts=pts3.map(p=>[dot(p,u),dot(p,v)]),lo=[Math.min(...pts.map(p=>p[0])),Math.min(...pts.map(p=>p[1]))],hi=[Math.max(...pts.map(p=>p[0])),Math.max(...pts.map(p=>p[1]))],d=dot(pts3[0],n);
  const span=hi.map((a,i)=>Math.max(.001,a-lo[i]));faces.push({e,f,mat,pts,lo,span,u,v,n,d,w:Math.max(3,Math.ceil(span[0]*8)),h:Math.max(3,Math.ceil(span[1]*8))});
 }else if(e instanceof Cube)for(const [side,f]of Object.entries(e.faces)){
  const a=e.from,b=e.to;let p,u,v,w,h;
  if(side==='north'){p=[b[0],b[1],a[2]];u=[-1,0,0];v=[0,-1,0];w=b[0]-a[0];h=b[1]-a[1];}
  if(side==='south'){p=[a[0],b[1],b[2]];u=[1,0,0];v=[0,-1,0];w=b[0]-a[0];h=b[1]-a[1];}
  if(side==='east'){p=[b[0],b[1],b[2]];u=[0,0,-1];v=[0,-1,0];w=b[2]-a[2];h=b[1]-a[1];}
  if(side==='west'){p=[a[0],b[1],a[2]];u=[0,0,1];v=[0,-1,0];w=b[2]-a[2];h=b[1]-a[1];}
  if(side==='up'){p=[a[0],b[1],a[2]];u=[1,0,0];v=[0,0,1];w=b[0]-a[0];h=b[2]-a[2];}
  if(side==='down'){p=[a[0],a[1],b[2]];u=[1,0,0];v=[0,0,-1];w=b[0]-a[0];h=b[2]-a[2];}
  faces.push({e,f,side,mat,p,u,v,span:[w,h],w:Math.max(3,Math.ceil(w*8)),h:Math.max(3,Math.ceil(h*8))});
 }
}
// Entire surface painting uses continuous model coordinates. Adjacent vessel facets do not receive repeated borders or highlights.
const palette={frame:[114,116,120],dark_steel:[65,73,80],steel:[158,164,168],toolsteel:[133,140,144],chrome:[182,190,194],yellow:[224,167,32],hazard:[224,167,32],rubber:[28,31,34],red:[171,46,32],green:[49,128,64],brass:[170,130,66],blue:[73,112,138],plate:[174,178,164],vessel:[115,107,96],glass:[71,123,148],broth:[31,130,162],fluid_light:[96,190,207],dial:[221,218,197],glow_green:[109,192,67],glow_amber:[219,157,46],glow_cyan:[63,163,189]};
const cv=document.createElement('canvas');cv.width=4096;cv.height=4096;const ctx=cv.getContext('2d');ctx.fillStyle='#293944';ctx.fillRect(0,0,4096,4096);
const tex=new Texture({name:'blast_furnace_materials.png',width:4096,height:4096});
function noise(x,y,z){return .42*Math.sin(x*.61+Math.sin(y*.73)+z*.32)+.30*Math.sin(y*.9-z*.7+Math.sin(x*.19))+.17*Math.cos(x*1.8+y*.8+z*1.1);}
function spot(x,y,z,X,Y,Z,sx,sy,sz){return Math.exp(-(((x-X)/sx)**2+((y-Y)/sy)**2+((z-Z)/sz)**2));}
faces.sort((a,b)=>b.h-a.h);let px=3,py=3,H=0;
for(const r of faces){
 if(px+r.w+3>4096){px=3;py+=H+6;H=0;}if(py+r.h+3>4096)throw Error('Material atlas overflow');
 const im=ctx.createImageData(r.w,r.h),base=palette[r.mat];
 for(let j=0;j<r.h;j++)for(let i=0;i<r.w;i++){
  const a=(i+.5)/r.w*r.span[0],b=(j+.5)/r.h*r.span[1];
  const p=r.p?r.p.map((v,k)=>v+r.u[k]*a+r.v[k]*b):r.u.map((v,k)=>v*(r.lo[0]+a)+r.v[k]*(r.lo[1]+b)+r.n[k]*r.d);
  const [x,y,z]=p,n=noise(x,y,z);let light=0,dirt=0,rust=0,wear=0;
  if(['blue','frame','vessel'].includes(r.mat)){
   light=9*Math.tanh((y-10)/6)+4*n;
   dirt=.18*spot(x,y,z,-15,5,-5,9,5,4)+.16*spot(x,y,z,18,4,-5,7,4,4)+.14*spot(x,y,z,1.5,12.8,-5.6,2,2,1);
   rust=.13*spot(x,y,z,17.5,5,-5.4,2,2,2)+.1*spot(x,y,z,-17,4,-5.5,2,2,2);
   // Long shallow scuffs, fading along their length; no stippled points.
   wear=.19*spot(x,y,z,8,11.8,-6.2,5,.10,.7)+.13*spot(x,y,z,-7,7.4,-6.4,3,.09,.7);
  }else if(['steel','toolsteel','chrome'].includes(r.mat)){
   light=5*n+3*Math.sin(x*3.2+y*.2+z*.4);dirt=.14*spot(x,y,z,8,18,-4,7,2,4)+.10*spot(x,y,z,-12,17,0,6,1.2,5);
  }else if(r.mat==='dark_steel'){light=4*n;dirt=.1;}
  else if(r.mat==='yellow'){light=5*n;dirt=.10*spot(x,y,z,0,17,6,25,3,2);}
  else if(r.mat==='brass'){light=6*n;dirt=.12*spot(x,y,z,20,3.5,-6,4,3,3);}
  else light=2*n;
  // Physical outer corners get restrained polished wear; cylindrical facets are deliberately excluded.
  if(r.e instanceof Cube&&!['rubber','red','green'].includes(r.mat)){
   const edge=Math.min(a,b,r.span[0]-a,r.span[1]-b);wear+=.08*Math.exp(-edge*24)*(.6+.4*Math.sin(x*.8+y*.5));
  }
  
  if(r.mat==='frame'){dirt+=.15*spot(x,y,z,0,3,-5.7,17,4,4)+.1*spot(x,y,z,-12,16,0,3,7,5);rust+=.08*spot(x,y,z,11,2,-5,4,3,3)+.06*spot(x,y,z,-11,2,-5,4,3,3);}
  if(r.mat==='toolsteel'){wear+=.23*spot(x,y,z,0,13.9,-.7,4,.2,4);}
  if(r.mat==='chrome'){light+=8*Math.sin(z*.75);}

  if(['frame','blue','dark_steel','toolsteel'].includes(r.mat)){
   light+=5*noise(x*.33,y*.33,z*.33);
   dirt+=.22*spot(x,y,z,-12,6,-5,4,3,5)+.16*spot(x,y,z,12,5,4,4,3,3)+.14*spot(x,y,z,-5,8,5,8,2,2);
   rust+=.12*spot(x,y,z,-12,5,-5,2,3,3)+.10*spot(x,y,z,13,6,5,2,3,2);
   if(r.e instanceof Cube||r.f.vertices?.length===4){const edge=Math.min(a,b,r.span[0]-a,r.span[1]-b);wear+=.23*Math.exp(-edge*11);light-=11*Math.exp(-Math.pow((edge-.15)/.1,2));}
  }
  
  if(r.mat==='dark_steel'||r.mat==='frame'){
   dirt+=.19*spot(x,y,z,18,55,25,16,8,16)+.16*spot(x,y,z,18,22,25,17,6,17);
   rust+=.18*spot(x,y,z,9,35,12,7,16,4)+.13*spot(x,y,z,26,25,34,5,9,7);
  }
  if(r.mat==='vessel'){
   const row=Math.floor(y/2.1),line=Math.min((y/2.1)%1,1-(y/2.1)%1);
   const seam=((Math.atan2(z-25,x-18)*13.5+(row%2)*2.5)%5+5)%5;
   light-=line<.07||seam<.16?27:0;light+=9*noise(Math.floor(x/4),row,Math.floor(z/4));
   rust=.25;dirt+=.2*spot(x,y,z,18,19,25,16,4,16);
  }
  const c=base.map(v=>v+light);
for(let k=0;k<3;k++){c[k]*=1-dirt;c[k]=c[k]*(1-rust)+[111,66,35][k]*rust;c[k]=c[k]*(1-wear)+[178,182,179][k]*wear;}
  const idx=(j*r.w+i)*4;for(let k=0;k<3;k++)im.data[idx+k]=Math.max(0,Math.min(255,Math.round(c[k])));im.data[idx+3]=255;
 }
 ctx.putImageData(im,px,py);
 // Texture gutters stop neighbouring atlas islands bleeding into narrow metal edges.
 ctx.drawImage(cv,px,py,r.w,1,px,py-2,r.w,2);ctx.drawImage(cv,px,py+r.h-1,r.w,1,px,py+r.h,r.w,2);ctx.drawImage(cv,px,py,1,r.h,px-2,py,2,r.h);ctx.drawImage(cv,px+r.w-1,py,1,r.h,px+r.w,py,2,r.h);
 r.f.texture=tex.uuid;
 if(r.pts)r.f.vertices.forEach((k,i)=>r.f.uv[k]=[px+.25+(r.pts[i][0]-r.lo[0])/r.span[0]*(r.w-.5),py+.25+(r.pts[i][1]-r.lo[1])/r.span[1]*(r.h-.5)]);
 else{r.f.uv=[px+.25,py+.25,px+r.w-.25,py+r.h-.25];r.f.rotation=0;}
 px+=r.w+6;H=Math.max(H,r.h);
}
tex.fromDataURL(cv.toDataURL()).add(false);tex.uv_width=4096;tex.uv_height=4096;



function canvasTexture(name,w,h,paint,emissive=false){const c=document.createElement('canvas');c.width=w;c.height=h;paint(c.getContext('2d'));const t=new Texture({name,width:w,height:h,render_mode:emissive?'emissive':'default'}).fromDataURL(c.toDataURL()).add(false);t.uv_width=w;t.uv_height=h;return t;}

const marks=canvasTexture('blast_furnace_markings.png',1024,256,c=>{
 c.fillStyle='#292c2d';c.fillRect(0,0,1024,80);c.fillStyle='#deb03e';for(let x=-120;x<1100;x+=130){c.beginPath();c.moveTo(x,80);c.lineTo(x+65,80);c.lineTo(x+145,0);c.lineTo(x+80,0);c.closePath();c.fill();}
 c.fillStyle='#bbb8a7';c.fillRect(0,100,1024,156);c.fillStyle='#273039';c.textAlign='center';c.font='bold 55px monospace';c.fillText('COGWILD / BF-01',512,166);c.font='32px monospace';c.fillText('HOT BLAST FURNACE',512,220);
});
plaque.faces.north.texture=marks.uuid;plaque.faces.north.uv=[0,100,1024,256];
for(const p of parts.filter(p=>p.material==='hazard'))for(const f of Object.values(p.e.faces)){f.texture=marks.uuid;if(f.uv instanceof Array)f.uv=[0,0,1024,80];else for(const k of f.vertices)f.uv[k]=[p.e.vertices[k][0]*100%1024,((p.e.vertices[k][1]*20)%80+80)%80];}
function keys(a,g,ch,pts){for(const [time,v]of pts)a.getBoneAnimator(g).addKeyframe({channel:ch,time,interpolation:'linear',data_points:[{x:String(v[0]),y:String(v[1]),z:String(v[2])}]});}
function clip(n,len,loop='loop'){return new Animation({name:'animation.blast_furnace.'+n,length:len,loop,snapping:60}).add(false);}
let a=clip('assembled_idle',3);keys(a,heat,'scale',[[0,[.001,.001,.001]],[3,[.001,.001,.001]]]);
a=clip('processing',2);keys(a,fan,'rotation',[[0,[0,0,0]],[2,[0,0,720]]]);
a=clip('loading',3,'hold');keys(a,heat,'scale',[[0,[.001,.001,.001]],[3,[.001,.001,.001]]]);keys(a,lidL,'position',[[0,[0,0,0]],[.8,[-3,0,0]],[2.2,[-3,0,0]],[3,[0,0,0]]]);keys(a,lidR,'position',[[0,[0,0,0]],[.8,[3,0,0]],[2.2,[3,0,0]],[3,[0,0,0]]]);
for(const t of Texture.all){t.namespace='suitcaseglider';t.folder='machine';}
Canvas.updateAll();unselectAllElements();Project.saved=false;
return {parts:parts.length,bones:Group.all.length,atlasUsed:py+H,clips:Animation.all.map(a=>a.name)};
})()
