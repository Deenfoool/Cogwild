"""Bake tileable refractory masonry and editable Java block models for the oven parts."""
import json
import math
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/suitcaseglider'
TEX=ASSETS/'textures/block'
TEX.mkdir(parents=True,exist_ok=True)

def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Continuous low-frequency shading, alternating brick courses, worn mortar edges.
im=Image.new('RGB',(64,64));p=im.load()
for y in range(64):
    row=y//16
    for x in range(64):
        u=(x+(16 if row%2 else 0))%32;v=y%16
        joint=u<2 or v<2
        shade=4*math.sin(2*math.pi*x/64)+3*math.cos(2*math.pi*y/64)+2*math.sin((u+v)*.3)
        edge=4*math.exp(-min(u,v,31-u,15-v)*.7)
        base=(65,62,56) if joint else (132+((x+(16 if row%2 else 0))//32%2)*5,120,101)
        p[x,y]=tuple(int(max(0,min(255,c+shade+edge))) for c in base)
im.save(TEX/'refractory_bricks.png')

for name,base in [('kiln_steel',(84,90,94)),('kiln_edge',(139,143,144)),('kiln_dark',(31,36,40)),('kiln_brass',(161,120,57)),('kiln_red',(150,52,32))]:
    im=Image.new('RGB',(64,64));p=im.load()
    for y in range(64):
        for x in range(64):
            shade=6*math.sin(x*.09)+4*math.cos(y*.08)+1.3*math.sin(x*.8+y*.2)
            grime=13*math.exp(-((x-13)/22)**2-((y-53)/20)**2)
            edge=6*math.exp(-min(x,y,63-x,63-y)*.6)
            p[x,y]=tuple(int(max(0,min(255,c+shade-grime+edge))) for c in base)
    im.save(TEX/(name+'.png'))

# Separate, crisp instrument and door labels. Dial is circular; its outer backing is steel.
im=Image.new('RGB',(128,128),'#545a5e');d=ImageDraw.Draw(im)
d.ellipse((3,3,124,124),fill='#252e35',outline='#b3b0a4',width=4)
d.ellipse((12,12,115,115),fill='#c7bea3',outline='#7d817d',width=2)
for i in range(25):
    a=math.radians(135+i*11.25);r=36 if i%6==0 else 42
    d.line((64+math.cos(a)*r,64+math.sin(a)*r,64+math.cos(a)*49,64+math.sin(a)*49),fill='#5b352d' if i>19 else '#343b3e',width=2)
d.line((64,64,88,39),fill='#a13f28',width=3);d.ellipse((59,59,69,69),fill='#343b3e')
d.text((53,85),'C',fill='#343b3e');im.save(TEX/'kiln_temperature_dial.png')
im=Image.new('RGB',(256,128),'#b9b09a');d=ImageDraw.Draw(im)
font_path='C:/Windows/Fonts/consolab.ttf'
font=ImageFont.truetype(font_path,25) if Path(font_path).exists() else ImageFont.load_default()
for i,text in enumerate(['COKE','CHARGE','DISCHARGE','CW / K-01']):
    d.rectangle((0,i*32,255,i*32+31),outline='#4a514f',width=2)
    d.text((128,i*32+16),text,font=font,anchor='mm',fill='#303c40')
im.save(TEX/'kiln_markings.png')

def uv(a,b,side):
    x,y,z=a;X,Y,Z=b
    return {'north':[16-X,16-Y,16-x,16-y],'south':[x,16-Y,X,16-y],
            'east':[16-Z,16-Y,16-z,16-y],'west':[z,16-Y,Z,16-y],
            'up':[x,z,X,Z],'down':[x,16-Z,X,16-z]}[side]

def box(name,a,b,texture='steel',north=None):
    faces={side:{'uv':uv(a,b,side),'texture':'#'+texture} for side in ['north','south','east','west','up','down']}
    if north:faces['north']={'texture':'#'+north[0],'uv':north[1]}
    return {'name':name,'from':a,'to':b,'faces':faces}

textures={'particle':'suitcaseglider:block/refractory_bricks','brick':'suitcaseglider:block/refractory_bricks'}
textures.update({k:'suitcaseglider:block/'+v for k,v in [('steel','kiln_steel'),('edge','kiln_edge'),('dark','kiln_dark'),('brass','kiln_brass'),('red','kiln_red'),('dial','kiln_temperature_dial'),('markings','kiln_markings')]})

names={
 'refractory_bricks':('Refractory Masonry','Огнеупорная кладка','Heat-resistant masonry for the coke oven shell.','Жаростойкая кладка для корпуса коксовой печи.'),
 'coke_oven_controller':('Coke Oven Controller','Контроллер коксовой печи','Construction part: front centre of the 3x3x3 oven.','Деталь конструкции: центр передней стороны печи 3×3×3.'),
 'coke_oven_input':('Coke Oven Charging Hatch','Загрузочный люк коксовой печи','Construction part: above the controller.','Деталь конструкции: над контроллером.'),
 'coke_oven_output':('Coke Oven Discharge Hatch','Выгрузной люк коксовой печи','Construction part: below the controller.','Деталь конструкции: под контроллером.'),
}

for kind in names:
    if kind=='refractory_bricks':
        model={'parent':'minecraft:block/cube_all','textures':{'all':textures['brick']}}
        variants={'':{'model':'suitcaseglider:block/'+kind}}
    else:
        parts=[box('Brick body',[0,0,2],[16,16,16],'brick')]
        for a,b in [([0,0,0],[2,16,2]),([14,0,0],[16,16,2]),([2,0,0],[14,2,2]),([2,14,0],[14,16,2])]:parts.append(box('Continuous brick surround',a,b,'brick'))
        parts.append(box('Recessed steel cover',[2,2,.5],[14,14,2]))
        for a,b in [([1.4,1.4,.25],[2.2,14.6,1.9]),([13.8,1.4,.25],[14.6,14.6,1.9]),([2.2,1.4,.25],[13.8,2.2,1.9]),([2.2,13.8,.25],[13.8,14.6,1.9])]:parts.append(box('Cast steel frame',a,b,'edge'))
        for x in [1.7,14.1]:
            for y in [1.7,14.1]:parts.append(box('Frame captive bolt',[x-.22,y-.22,.10],[x+.22,y+.22,.25],'dark'))
        if kind.endswith('controller'):
            parts.extend([box('Inset instrument mount',[8.3,8.1,.26],[12.9,12.7,.5],'edge'),box('Round temperature dial',[8.5,8.3,.20],[12.7,12.5,.26],'steel',('dial',[0,0,16,16])),box('Operating lever base',[3.1,6.0,.27],[6,9,.5],'dark'),box('Lever pivot',[4,7,.10],[5.2,8,.27],'brass'),box('Mechanical lever',[4.35,4.6,.02],[4.85,7.5,.10],'edge'),box('Red lever grip',[4.0,4.2,0],[5.2,5.0,.1],'red'),box('Factory plate',[7,4,.35],[12.7,5.7,.5],'edge',('markings',[0,12,16,16])),box('Controller nameplate',[3,10.4,.35],[7.6,12.6,.5],'edge',('markings',[0,0,16,4]))])
        else:
            parts.append(box('Door soot gasket',[2.9,3.4,.35],[13.1,12.8,.5],'dark'))
            parts.append(box('Closed cast hatch',[3.3,3.8,.20],[12.7,12.4,.35]))
            for y in [5,10.4]:parts.append(box('Hinge',[2.6,y,.10],[3.7,y+1.1,.42],'edge'))
            parts.extend([box('Latch receiver',[11.3,7,.10],[13.3,9.3,.45],'dark'),box('Latch handle',[11.8,6.6,0],[12.5,9.3,.15],'brass')])
            line=1 if kind.endswith('input') else 2
            parts.append(box('Door nameplate',[4.5,10.2,.13],[10.7,11.7,.20],'edge',('markings',[0,line*4,16,(line+1)*4])))
            if kind.endswith('input'):
                parts.append(box('Sealed inspection slit',[4.8,7,.15],[9.7,8.0,.20],'dark'))
            else:
                parts.extend([box('Lower discharge sill',[3.1,3.4,.1],[12.9,4.0,1.1],'edge'),box('Reserved drain capped boss',[6.8,5,.08],[8.9,6.7,.20],'dark'),box('Reserved drain cap',[7.2,5.3,.02],[8.5,6.4,.08],'brass')])
        model={'parent':'minecraft:block/block','ambientocclusion':True,'textures':textures,'elements':parts}
        variants={'facing='+f:{'model':'suitcaseglider:block/'+kind,'y':angle} for f,angle in [('north',0),('east',90),('south',180),('west',270)]}
    write(ASSETS/'models/block'/f'{kind}.json',model)
    write(ASSETS/'models/item'/f'{kind}.json',{'parent':'suitcaseglider:block/'+kind})
    write(ASSETS/'blockstates'/f'{kind}.json',{'variants':variants})
    write(RES/'data/suitcaseglider/loot_tables/blocks'/f'{kind}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'suitcaseglider:'+kind}]}]})

spec=json.loads((ROOT/'docs/design/coke-oven-v0.1.json').read_text(encoding='utf-8'))
for r in spec['recipes']:
    if r['id'] not in names:continue
    recipe={'type':'minecraft:crafting_shaped','pattern':r['pattern'],'key':{k:{'item':v} for k,v in r['key'].items()},'result':r['output']}
    write(RES/'data/suitcaseglider/recipes'/f"{r['id']}.json",recipe)
    trigger='refractory_brick' if r['id']=='refractory_bricks' else 'refractory_bricks'
    write(RES/'data/suitcaseglider/advancements/recipes'/f"{r['id']}.json",{'parent':'minecraft:recipes/root','criteria':{'material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['suitcaseglider:'+trigger]}]}}},'rewards':{'recipes':['suitcaseglider:'+r['id']]}})
for locale,ru in [('en_us',False),('ru_ru',True)]:
    path=ASSETS/'lang'/f'{locale}.json';lang=json.loads(path.read_text(encoding='utf-8'))
    for name,strings in names.items():
        lang['block.suitcaseglider.'+name]=strings[1 if ru else 0]
        lang['tooltip.suitcaseglider.'+name]=strings[3 if ru else 2]
    write(path,lang)
path=RES/'data/minecraft/tags/blocks/mineable/pickaxe.json'
old=json.loads(path.read_text()) if path.exists() else {'replace':False,'values':[]}
old['values']=list(dict.fromkeys(old['values']+['suitcaseglider:'+name for name in names]));write(path,old)
print('Exported four blocks, four item models, four recipes, loot, mining tag, translations and eight textures.')
