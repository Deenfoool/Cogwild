"""Bake all approved Blockbench projects into textured, animated Forge render data."""
import base64, json, math, struct, zlib
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

CATALOG={}

def convert(kind,filename,names,clips,rig_only=False):
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
        if len(vs)>4:
            for i in range(1,len(vs)-1):addface(b,[vs[0],vs[i],vs[i+1]],[uv[0],uv[i],uv[i+1]],texture)
            return
        if len(vs)!=4:raise ValueError('Invalid polygon')
        a,c,d=vs[:3];u=[c[i]-a[i] for i in range(3)];v=[d[i]-a[i] for i in range(3)]
        n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];length=math.sqrt(sum(x*x for x in n))
        if length<1e-10:return
        parent=b;emissive=m['textures'][int(texture)].get('render_mode')=='emissive'
        while parent>=0:
            emissive|=bones[parent]['name'].startswith('lamp_') or (kind=='blast_furnace' and bones[parent]['name']=='heat_windows');parent=bones[parent]['parent']
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
        animations[a['name'].split('.')[-1]]=dict(length=a['length'],loop=a['loop']=='loop',tracks=tracks)
    def world(b,p):
        while b>=0:
            bone=bones[b];parent=bone['parent'];po=bones[parent]['origin'] if parent>=0 else [0,0,0]
            p=rotate(p,bone['rotation']);p=[p[i]+bone['origin'][i]-po[i] for i in range(3)];b=parent
        return p
    points=[world(f['bone'],[c*16 for c in v]) for f in faces for v in f['v']]
    lo=[min(p[i] for p in points) for i in range(3)];hi=[max(p[i] for p in points) for i in range(3)]
    width=max(2,min(3,math.ceil((hi[0]-lo[0]-.01)/16)))
    depth=2 if kind=='fermenter' else 1
    offset=[(width-1)/2-(hi[0]+lo[0])/32,-lo[1]/16,(depth-1)/2-(hi[2]+lo[2])/32]
    def anchor_world(name):
        a=anchors[name];o=bones[a['bone']]['origin'];v=world(a['bone'],[a['position'][i]-o[i] for i in range(3)])
        return [v[i]/16+offset[i] for i in range(3)]
    power=next((n for n in ['power_input','PORT_POWER_Z_POS','power_output_1_380v'] if n in anchors),None)
    if power:port=anchor_world(power)
    else:
        power_bone=next((i for i,b in enumerate(bones) if b['name']=='PORT_POWER_Z_POS'),None)
        port=[(width-1)/2,.5,(hi[2]-lo[2])/32+.08] if power_bone is None else [v/16+offset[i] for i,v in enumerate(world(power_bone,[0,0,0]))]
    if kind=='lathe':port=[15.9/16,8.25/16,.59]
    if kind=='crusher':port=[1.15625,.46875,.59]
    if kind=='rolling_mill':port=[.5,.65,(hi[2]-lo[2])/32+.08]
    CATALOG[kind]=dict(width=width,depth=depth,height=round(hi[1]-lo[1],5),offset=offset,power=port,off=clips[0],on=clips[1],start=clips[2],stop=clips[3])
    data=dict(bones=bones,faces=faces,animations=animations,anchors=anchors,textures=textures)
    (OUT/'rigs'/f'{kind}.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    if rig_only:
        print(kind,'bones',len(bones),'faces',len(faces),'animations',list(animations))
        return
    color=(100,112,50) if kind=='diesel_generator' else (70,96,116) if kind in ('quenching_bath','air_separator') else (112,119,123)
    raw=b''.join(b'\x00'+bytes(max(0,min(255,c+(y//4-2)*3)) for x in range(16) for c in color) for y in range(16))
    def chunk(tag,data):return struct.pack('>I',len(data))+tag+data+struct.pack('>I',zlib.crc32(tag+data)&0xffffffff)
    particle=OUT/'textures/block'/f'{kind}_particle.png';particle.parent.mkdir(parents=True,exist_ok=True)
    particle.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
    model={'parent':'minecraft:block/block','textures':{'particle':f'suitcaseglider:block/{kind}_particle'}}
    (OUT/'models/block').mkdir(parents=True,exist_ok=True);(OUT/'models/block'/f'{kind}.json').write_text(json.dumps(model))
    (OUT/'blockstates').mkdir(exist_ok=True);(OUT/'blockstates'/f'{kind}.json').write_text(json.dumps({'variants':{'':{'model':f'suitcaseglider:block/{kind}'}}}))
    item={'parent':'builtin/entity','display':{'gui':{'rotation':[20,45,0],'scale':[.75,.75,.75]},'ground':{'translation':[0,2,0],'scale':[.5,.5,.5]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.5,.5,.5]},'thirdperson_righthand':{'rotation':[60,45,0],'scale':[.5,.5,.5]}}}
    (OUT/'models/item'/f'{kind}.json').write_text(json.dumps(item))
    data_dir=OUT.parents[1]/'data/suitcaseglider/loot_tables/blocks';data_dir.mkdir(parents=True,exist_ok=True)
    (data_dir/f'{kind}.json').write_text(json.dumps({'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'},{'condition':'minecraft:block_state_property','block':f'suitcaseglider:{kind}','properties':{'secondary':'false','layer':'0'}}],'entries':[{'type':'minecraft:item','name':f'suitcaseglider:{kind}'}]}]}))
    print(kind, 'bones',len(bones),'faces',len(faces),'animations',list(animations))


MODELS=[
 ('lathe','Lathe-Soft-Aged.bbmodel',('Lathe','Токарный станок'),('unpowered','powered_idle','power_on','power_off')),
 ('crusher','Crusher-Refined-Console-Aged.bbmodel',('Crusher','Дробилка'),('unpowered','powered_idle','','')),
 ('rolling_mill','Two-High-Rolling-Mill.bbmodel',('Rolling Mill','Прокатный стан'),('unpowered','processing','','')),
 ('quenching_bath','Quenching-Bath.bbmodel',('Quenching Bath','Закалочная ванна'),('idle_empty','idle_filled','filling','draining')),
 ('air_separator','Air-Separator.bbmodel',('Air Separator','Воздушный сепаратор'),('unpowered','processing','starting','stopping')),
 ('air_compressor','Air-Compressor.bbmodel',('Air Compressor','Воздушный компрессор'),('idle','running','starting','stopping')),
 ('industrial_press','Industrial-Press.bbmodel',('Industrial Press','Промышленный пресс'),('idle','processing','starting','stopping')),
 ('fermenter','Fermenter.bbmodel',('Fermenter','Ферментер'),('shutdown','processing','','')),
 ('universal_mixer','Universal-Mixer.bbmodel',('Universal Mixer','Универсальный смеситель'),('shutdown','processing','start','stop')),
 ('diesel_generator','Diesel-Generator.bbmodel',('Diesel Generator','Дизельный генератор'),('shutdown','running','starting','stopping')),
]
def main():
    for kind,filename,names,clips in MODELS:convert(kind,filename,names,clips)
    (OUT/'rigs/machines.json').write_text(json.dumps(CATALOG,indent=2),encoding='utf-8')
    for p in (OUT/'lang').glob('*.json'):
        lang=json.loads(p.read_text(encoding='utf-8-sig'));ru=p.stem=='ru_ru'
        for kind,_,names,_ in MODELS:lang['block.suitcaseglider.'+kind]=names[1 if ru else 0]
        p.write_text(json.dumps(lang,ensure_ascii=False,indent=2),encoding='utf-8')
    for kind,_,_,_ in MODELS:
        if kind in ('lathe','crusher'):continue
        recipe={'type':'minecraft:crafting_shaped','pattern':['III','CRC','IPI'],'key':{'I':{'item':'minecraft:iron_ingot'},'C':{'item':'minecraft:copper_ingot'},'R':{'item':'minecraft:water_bucket' if kind=='quenching_bath' else 'minecraft:furnace' if kind=='diesel_generator' else 'minecraft:piston'},'P':{'item':'minecraft:redstone'}},'result':{'item':'suitcaseglider:'+kind}}
        folder=ROOT/'src/main/resources/data/suitcaseglider/recipes';folder.mkdir(parents=True,exist_ok=True)
        (folder/(kind+'.json')).write_text(json.dumps(recipe,indent=2),encoding='utf-8')
    print(json.dumps(CATALOG,indent=2))

if __name__=='__main__':
    main()
