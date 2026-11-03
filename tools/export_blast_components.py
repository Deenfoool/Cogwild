"""Construction block assets, early motor components and non-steel bootstrap recipes."""
import json, math
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/suitcaseglider'
D=ROOT/'src/main/resources/data/suitcaseglider'
def write(p,obj):
    p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2),encoding='utf-8')
def item(s):return {'item':s if ':' in s else 'suitcaseglider:'+s}
names={
'blast_hearth':('Blast Furnace Hearth','Горн доменной печи'),
'blast_shaft':('Blast Furnace Shaft','Секция шахты доменной печи'),
'blast_duct':('Flanged Air Duct','Фланцевый воздуховод'),
'blast_preheater':('Hot Blast Preheater','Секция воздухонагревателя'),
'blast_blower':('Electric Blower','Электрический нагнетатель'),
'blast_hopper':('Sealed Loading Bunker','Закрытый загрузочный бункер'),
'blast_controller':('Blast Furnace Controller','Контроллер доменной печи'),
'copper_wire':('Copper Wire','Медная проволока'),
'copper_coil':('Copper Coil','Медная катушка'),
'magnet':('Magnet','Магнит'),
'motor_housing':('Motor Housing','Корпус электродвигателя'),
'electric_motor':('Electric Motor','Электродвигатель')}
for kind in list(names)[:7]:
    im=Image.new('RGB',(64,64));px=im.load()
    base=(92,91,89) if kind!='blast_hearth' else (110,82,62)
    for y in range(64):
        for x in range(64):
            wear=15*math.exp(-min(x,y,63-x,63-y)/1.8)
            grime=13*math.exp(-((x-47)/19)**2-((y-51)/22)**2)
            n=2*math.sin(x*.19+y*.12)+1.3*math.sin(x*.43-y*.37)
            px[x,y]=tuple(max(0,min(255,round(c+wear-grime+n)))for c in base)
    dr=ImageDraw.Draw(im)
    dr.rectangle((0,0,63,63),outline=(48,51,52),width=3)
    for x,y in [(5,5),(58,5),(5,58),(58,58)]:dr.rectangle((x-1,y-1,x+1,y+1),fill=(166,164,152))
    if kind in ('blast_duct','blast_blower'):
        dr.rectangle((12,12,51,51),fill=(43,49,50),outline=(150,145,130),width=3)
        for y in range(19,49,6):dr.line((18,y,45,y),fill=(103,110,111),width=2)
    elif kind=='blast_preheater':
        dr.rectangle((10,11,53,52),fill=(42,41,38));
        for x in range(16,51,7):dr.rectangle((x,15,x+3,48),fill=(163,116,58))
    elif kind=='blast_controller':
        dr.rectangle((13,12,50,35),fill=(27,32,29),outline=(154,155,145),width=2)
        for x,h in [(20,5),(28,11),(36,16)]:dr.rectangle((x,31-h,x+4,31),fill=(112,157,70))
        dr.rectangle((18,43,26,51),fill=(47,123,62));dr.rectangle((36,43,44,51),fill=(168,49,34))
    elif kind=='blast_hopper':
        dr.rectangle((12,12,51,51),outline=(45,45,42),width=4);dr.rectangle((24,25,39,36),fill=(165,123,50))
    elif kind=='blast_hearth':
        for y in range(10,60,12):
            dr.line((5,y,58,y),fill=(50,48,43),width=2)
            for x in range(5+(y//12%2)*12,59,24):dr.line((x,y,x,min(58,y+12)),fill=(50,48,43),width=2)
    else:
        dr.rectangle((11,8,16,55),fill=(119,119,114));dr.rectangle((47,8,52,55),fill=(119,119,114));dr.line((17,31,46,31),fill=(47,48,47),width=2)
    p=A/'textures/block'/f'{kind}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
    write(A/'models/block'/f'{kind}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'suitcaseglider:block/{kind}'}})
    variants={f'facing={f}'+(',formed=false'if kind=='blast_controller'else''):{'model':f'suitcaseglider:block/{kind}','y':a}for f,a in [('north',0),('east',90),('south',180),('west',270)]}
    if kind=='blast_controller':
        variants={k+',lit='+lit:v for k,v in variants.items() for lit in ['false','true']}
        write(A/'models/block/blast_invisible.json',{'parent':'minecraft:block/block','textures':{'particle':'suitcaseglider:block/blast_shaft'}})
        variants.update({f'facing={f},formed=true,lit={lit}':{'model':'suitcaseglider:block/blast_invisible'}for f in ['north','east','south','west']for lit in ['false','true']})
    write(A/'blockstates'/f'{kind}.json',{'variants':variants})
    write(A/'models/item'/f'{kind}.json',{'parent':f'suitcaseglider:block/{kind}'})
    write(D/'loot_tables/blocks'/f'{kind}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':f'suitcaseglider:{kind}'}]}]})
write(A/'blockstates/blast_formed_part.json',{'variants':{'':{'model':'suitcaseglider:block/blast_invisible'}}})
from item_icons import export_icons
export_icons(list(names)[7:])
for kind in list(names)[7:]:
    write(A/'models/item'/f'{kind}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'suitcaseglider:item/{kind}'}})

def shaped(id,pattern,key,count=1):write(D/'recipes'/f'{id}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:item(v)for k,v in key.items()},'result':{'item':'suitcaseglider:'+id,'count':count}})
shaped('copper_wire',['CCC'],{'C':'minecraft:copper_ingot'},6)
shaped('copper_coil',['WWW','WIW','WWW'],{'W':'copper_wire','I':'minecraft:iron_ingot'})
shaped('magnet',[' I ','IRI'],{'I':'minecraft:iron_ingot','R':'minecraft:raw_iron'})
shaped('motor_housing',['III','I I','III'],{'I':'minecraft:iron_ingot'})
shaped('electric_motor',[' C ','MHM',' C '],{'C':'copper_coil','M':'magnet','H':'motor_housing'})
shaped('blast_hearth',['RIR','IRI','RIR'],{'R':'refractory_bricks','I':'minecraft:iron_ingot'},4)
shaped('blast_shaft',['IRI','R R','IRI'],{'R':'refractory_bricks','I':'minecraft:iron_ingot'},4)
shaped('blast_duct',['III','C C','III'],{'I':'minecraft:iron_ingot','C':'minecraft:copper_ingot'},4)
shaped('blast_preheater',['IDI','CDC','IDI'],{'I':'minecraft:iron_ingot','D':'blast_duct','C':'minecraft:copper_ingot'},2)
shaped('blast_blower',['III','IMD','III'],{'I':'minecraft:iron_ingot','M':'electric_motor','D':'blast_duct'})
shaped('blast_hopper',['IRI','IHI',' I '],{'I':'minecraft:iron_ingot','R':'refractory_bricks','H':'minecraft:hopper'})
shaped('blast_controller',['ICI','RLR','III'],{'I':'minecraft:iron_ingot','C':'copper_coil','R':'refractory_bricks','L':'minecraft:lever'})
extra={
'message.suitcaseglider.blast.need_key':('Use the Guild Assembly Key on the front-left controller.','Используйте монтажный ключ на переднем левом контроллере.'),
'message.suitcaseglider.blast.formed':('Blast furnace assembled.','Доменная печь сформирована.'),
'message.suitcaseglider.blast.assembled':('Right-click to open; sneak + assembly key to dismantle.','ПКМ — открыть; Shift + монтажный ключ — разобрать.'),
'message.suitcaseglider.blast.unloaded':('Load the entire construction before assembly.','Перед сборкой загрузите всю конструкцию.'),
'message.suitcaseglider.blast.mismatch':('At %s, %s, %s: expected %s (empty containers only).','В точке %s, %s, %s ожидается %s. Контейнеры должны быть пустыми.')}
for p in (A/'lang').glob('*.json'):
    lang=json.loads(p.read_text(encoding='utf-8-sig'));ru=p.stem=='ru_ru'
    for k,n in names.items():lang[('block.'if k.startswith('blast_')else'item.')+'suitcaseglider.'+k]=n[ru]
    for k,v in extra.items():lang[k]=v[ru]
    write(p,lang)
for tag in ['mineable/pickaxe','needs_iron_tool']:
    p=ROOT/'src/main/resources/data/minecraft/tags/blocks'/f'{tag}.json';data=json.loads(p.read_text())if p.exists()else{'replace':False,'values':[]}
    data['values']=list(dict.fromkeys(data['values']+['suitcaseglider:'+k for k in list(names)[:7]]+['suitcaseglider:blast_formed_part']));write(p,data)
print('Blast furnace: 7 modules, 5 motor components and 12 recipes exported')
