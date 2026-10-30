import json, math, pathlib, base64
ROOT=pathlib.Path(__file__).resolve().parent.parent
SRC=ROOT/'art_source/blockbench/entities/Mech-Ant-Three-Section-Solid.bbmodel'
OUT=ROOT/'src/main/resources/assets/suitcaseglider'
m=json.loads(SRC.read_text(encoding='utf-8'))
groups={g['uuid']:g for g in m['groups']}; elements={e['uuid']:e for e in m['elements']}
bones=[]; faces=[]; anchors={}; ids={}
def rotate(v,r):
    x,y,z=v
    for axis,angle in enumerate(r):
        c,s=math.cos(math.radians(angle)),math.sin(math.radians(angle))
        if axis==0:y,z=y*c-z*s,y*s+z*c
        elif axis==1:x,z=x*c+z*s,-x*s+z*c
        else:x,y=x*c-y*s,x*s+y*c
    return [x,y,z]
def addface(b,vs,uv,emissive):
    if len(vs)==3:vs=vs+[vs[-1]];uv=uv+[uv[-1]]
    if len(vs)!=4:return
    a,c,d=vs[:3];u=[c[i]-a[i] for i in range(3)];v=[d[i]-a[i] for i in range(3)]
    n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];l=math.sqrt(sum(x*x for x in n)) or 1
    faces.append(dict(bone=b,v=vs,uv=uv,n=[x/l for x in n],emissive=emissive))
def element(e,b):
    typ=e.get('type','cube');name=e.get('name','')
    if typ=='locator':anchors[name]=dict(bone=b,position=e.get('position',e.get('from',[0,0,0])));return
    if e.get('export',True)==False:return
    origin=e.get('origin',[0,0,0]);bo=bones[b]['origin'];r=e.get('rotation',[0,0,0])
    def transformed(v,mesh=False):
        v=rotate(v if mesh else [v[i]-origin[i] for i in range(3)],r)
        return [(v[i]+origin[i]-bo[i])/16 for i in range(3)]
    if typ=='mesh':
        for f in e['faces'].values():
            if f.get('texture') is None:continue
            ks=f['vertices'];addface(b,[transformed(e['vertices'][k],True) for k in ks],[f['uv'][k] for k in ks],False)
    elif typ=='cube':
        inf=e.get('inflate',0);f=[x-inf for x in e['from']];t=[x+inf for x in e['to']]
        xyz={'north':[[t[0],t[1],f[2]],[f[0],t[1],f[2]],[f[0],f[1],f[2]],[t[0],f[1],f[2]]],
             'south':[[f[0],t[1],t[2]],[t[0],t[1],t[2]],[t[0],f[1],t[2]],[f[0],f[1],t[2]]],
             'west':[[f[0],t[1],f[2]],[f[0],t[1],t[2]],[f[0],f[1],t[2]],[f[0],f[1],f[2]]],
             'east':[[t[0],t[1],t[2]],[t[0],t[1],f[2]],[t[0],f[1],f[2]],[t[0],f[1],t[2]]],
             'up':[[t[0],t[1],t[2]],[f[0],t[1],t[2]],[f[0],t[1],f[2]],[t[0],t[1],f[2]]],
             'down':[[t[0],f[1],f[2]],[f[0],f[1],f[2]],[f[0],f[1],t[2]],[t[0],f[1],t[2]]]}
        for side,face in e['faces'].items():
            if face.get('texture') is None:continue
            u0,v0,u1,v1=face['uv'];uv=[[u0,v0],[u1,v0],[u1,v1],[u0,v1]];q=int(face.get('rotation',0)/90)%4;uv=uv[q:]+uv[:q]
            addface(b,list(reversed([transformed(v) for v in xyz[side]])),list(reversed(uv)),bones[b]['name'].startswith('LIGHTS_ON_'))
def walk(node,parent=-1):
    if isinstance(node,str):element(elements[node],parent);return
    g=groups[node['uuid']];idx=len(bones);ids[g['uuid']]=idx
    bones.append(dict(name=g['name'],parent=parent,origin=g.get('origin',[0,0,0]),rotation=g.get('rotation',[0,0,0])))
    for child in node.get('children',[]):walk(child,idx)
for node in m['outliner']:walk(node)
animations={}
for a in m['animations']:
    name=a['name'].removeprefix('animation.mech_ant.')
    if name.endswith('_preview'):continue
    tracks=[]
    for uid,anim in a['animators'].items():
        if uid not in ids:continue
        for ch in ['rotation','position','scale']:
            keys=sorted((k for k in anim.get('keyframes',[]) if k['channel']==ch),key=lambda k:k['time'])
            if keys:tracks.append(dict(bone=ids[uid],channel=ch,times=[k['time'] for k in keys],values=[[float(k['data_points'][0].get(ax,1 if ch=='scale' else 0)) for ax in 'xyz'] for k in keys]))
    animations[name]=dict(length=a['length'],loop=a['loop']=='loop',tracks=tracks)
rig=dict(bones=bones,faces=faces,animations=animations,anchors=anchors)
(OUT/'rigs').mkdir(parents=True,exist_ok=True);(OUT/'textures/entity').mkdir(parents=True,exist_ok=True)
(OUT/'rigs/mech_ant.json').write_text(json.dumps(rig,separators=(',',':')),encoding='utf-8')
color=next(t for t in m['textures'] if t.get('pbr_channel')!='mer')
(OUT/'textures/entity/mech_ant.png').write_bytes(base64.b64decode(color['source'].split(',')[1]))
print(json.dumps(dict(bones=len(bones),faces=len(faces),animations=list(animations),anchors=anchors),ensure_ascii=False))
for f in (OUT/'lang').glob('*.json'):
    lang=json.loads(f.read_text(encoding='utf-8-sig'))
    lang['entity.suitcaseglider.mech_ant']='Мех-муравей' if f.stem=='ru_ru' else 'Mech Ant'
    lang['item.suitcaseglider.mech_ant_spawn_egg']='Яйцо призыва меха-муравья' if f.stem=='ru_ru' else 'Mech Ant Spawn Egg'
    f.write_text(json.dumps(lang,ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'models/item/mech_ant_spawn_egg.json').write_text(json.dumps({'parent':'minecraft:item/template_spawn_egg'}),encoding='utf-8')
