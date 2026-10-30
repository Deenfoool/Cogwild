"""Bake the two approved Blockbench projects into textured, animated Forge render data."""
import base64, json, math
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'src/main/resources/assets/suitcaseglider'

def rotate(v,r):
    x,y,z=v
    for axis,angle in enumerate(r):
        c,s=math.cos(math.radians(angle)),math.sin(math.radians(angle))
        if axis==0:y,z=y*c-z*s,y*s+z*c
        elif axis==1:x,z=x*c+z*s,-x*s+z*c
        else:x,y=x*c-y*s,x*s+y*c
    return [x,y,z]

def convert(kind,filename):
    m=json.loads((ROOT/'art_source/blockbench/blocks'/filename).read_text(encoding='utf-8'))
    groups={g['uuid']:g for g in m['groups']};elements={e['uuid']:e for e in m['elements']}
    bones=[dict(name='render_root',parent=-1,origin=[0,0,0],rotation=[0,0,0])];faces=[];ids={};anchors={}
    textures=[]
    for i,t in enumerate(m['textures']):
        resource=f'textures/machine/{kind}_{i}.png';path=OUT/resource;path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(base64.b64decode(t['source'].split(',')[1]))
        textures.append(dict(resource='suitcaseglider:'+resource,width=t.get('uv_width',t['width']),height=t.get('uv_height',t['height']),translucent='glass' in t['name'].lower()))

    def addface(b,vs,uv,texture):
        if len(vs)==3:vs=vs+[vs[-1]];uv=uv+[uv[-1]]
        if len(vs)!=4:raise ValueError('Non-quad polygon needs triangulation')
        a,c,d=vs[:3];u=[c[i]-a[i] for i in range(3)];v=[d[i]-a[i] for i in range(3)]
        n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];length=math.sqrt(sum(x*x for x in n))
        if length<1e-10:return
        parent=b;emissive=False
        while parent>=0:
            emissive|=bones[parent]['name'].startswith('lamp_');parent=bones[parent]['parent']
        faces.append(dict(bone=b,v=vs,uv=uv,n=[x/length for x in n],emissive=emissive,texture=int(texture)))

    def element(e,b):
        if not e.get('export',True):return
        typ=e.get('type','cube');origin=e.get('origin',[0,0,0]);bo=bones[b]['origin'];r=e.get('rotation',[0,0,0])
        if typ=='locator':anchors[e['name']]=dict(bone=b,position=e['position']);return
        def transformed(v,mesh=False):
            v=rotate(v if mesh else [v[i]-origin[i] for i in range(3)],r)
            return [(v[i]+origin[i]-bo[i])/16 for i in range(3)]
        if typ=='mesh':
            for face in e['faces'].values():
                if face.get('texture') is None:continue
                ks=face['vertices'];addface(b,[transformed(e['vertices'][k],True) for k in ks],[face['uv'][k] for k in ks],face['texture'])
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
                addface(b,list(reversed([transformed(v) for v in xyz[side]])),list(reversed(uv)),face['texture'])

    def walk(node,parent=0):
        if isinstance(node,str):element(elements[node],parent);return
        g=groups[node['uuid']]
        if not g.get('export',True):return
        idx=len(bones);ids[g['uuid']]=idx;bones.append(dict(name=g['name'],parent=parent,origin=g.get('origin',[0,0,0]),rotation=g.get('rotation',[0,0,0])))
        for child in node.get('children',[]):walk(child,idx)
    for node in m['outliner']:walk(node)
    animations={}
    for a in m.get('animations',[]):
        tracks=[]
        for uid,anim in a['animators'].items():
            if uid not in ids:continue
            for ch in ['rotation','position','scale']:
                keys=sorted((k for k in anim.get('keyframes',[]) if k['channel']==ch),key=lambda k:k['time'])
                if keys:tracks.append(dict(bone=ids[uid],channel=ch,times=[k['time'] for k in keys],values=[[float(k['data_points'][0].get(ax,1 if ch=='scale' else 0)) for ax in 'xyz'] for k in keys]))
        animations[a['name'].removeprefix(f'animation.{kind}.')]=dict(length=a['length'],loop=a['loop']=='loop',tracks=tracks)
    data=dict(bones=bones,faces=faces,animations=animations,anchors=anchors,textures=textures)
    (OUT/'rigs'/f'{kind}.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    model={'parent':'minecraft:block/block','textures':{'particle':f'suitcaseglider:machine/{kind}_0'}}
    (OUT/'models/block').mkdir(parents=True,exist_ok=True);(OUT/'models/block'/f'{kind}.json').write_text(json.dumps(model))
    (OUT/'blockstates').mkdir(exist_ok=True);(OUT/'blockstates'/f'{kind}.json').write_text(json.dumps({'variants':{'':{'model':f'suitcaseglider:block/{kind}'}}}))
    item={'parent':'builtin/entity','display':{'gui':{'rotation':[20,45,0],'scale':[.75,.75,.75]},'ground':{'translation':[0,2,0],'scale':[.5,.5,.5]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.5,.5,.5]},'thirdperson_righthand':{'rotation':[60,45,0],'scale':[.5,.5,.5]}}}
    (OUT/'models/item'/f'{kind}.json').write_text(json.dumps(item))
    data_dir=OUT.parents[1]/'data/suitcaseglider/loot_tables/blocks';data_dir.mkdir(parents=True,exist_ok=True)
    (data_dir/f'{kind}.json').write_text(json.dumps({'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'},{'condition':'minecraft:block_state_property','block':f'suitcaseglider:{kind}','properties':{'secondary':'false'}}],'entries':[{'type':'minecraft:item','name':f'suitcaseglider:{kind}'}]}]}))
    print(kind, 'bones',len(bones),'faces',len(faces),'animations',list(animations))

convert('lathe','Lathe-Soft-Aged.bbmodel')
convert('crusher','Crusher-Refined-Console-Aged.bbmodel')
for p in (OUT/'lang').glob('*.json'):
    lang=json.loads(p.read_text(encoding='utf-8-sig'));ru=p.stem=='ru_ru'
    lang['block.suitcaseglider.lathe']='Токарный станок' if ru else 'Lathe'
    lang['block.suitcaseglider.crusher']='Дробилка' if ru else 'Crusher'
    p.write_text(json.dumps(lang,ensure_ascii=False,indent=2),encoding='utf-8')
