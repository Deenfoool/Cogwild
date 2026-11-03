"""Runtime materials and a compact vanilla-sized furnace interface. No native model mutations."""
import json,math
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/suitcaseglider';D=ROOT/'src/main/resources/data'
def write(p,d):
    p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2),encoding='utf-8')
names={'limestone':('Limestone','Известняк'),'limestone_flux':('Limestone Flux','Известняковый флюс'),'pig_iron_ingot':('Legacy Pig Iron Ingot','Чугунный слиток (старая версия)'),'steel_ingot':('Steel Ingot','Стальной слиток'),'blast_slag':('Blast Furnace Slag','Доменный шлак'),'slag_bricks':('Slag Bricks','Шлаковый кирпич'),'pig_iron_block':('Legacy Block of Pig Iron','Чугунный блок (старая версия)'),'steel_block':('Block of Steel','Стальной блок')}
for kind,base in [('limestone',(185,180,153)),('slag_bricks',(77,77,70)),('pig_iron_block',(83,91,96)),('steel_block',(123,134,143))]:
    im=Image.new('RGB',(32,32));p=im.load()
    for y in range(32):
        for x in range(32):
            n=4*math.sin(x*.7+y*.23)+3*math.cos(y*.4-x*.28);v=base
            if kind=='slag_bricks' and (y%8==0 or (x+(y//8%2)*8)%16==0):v=(48,47,43)
            if kind in ['pig_iron_block','steel_block'] and min(x,y,31-x,31-y)<2:v=(110,117,117)
            p[x,y]=tuple(max(0,min(255,round(c+n)))for c in v)
    path=A/'textures/block'/f'{kind}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
    write(A/'models/block'/f'{kind}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'suitcaseglider:block/{kind}'}})
    write(A/'models/item'/f'{kind}.json',{'parent':f'suitcaseglider:block/{kind}'})
    write(A/'blockstates'/f'{kind}.json',{'variants':{'':{'model':f'suitcaseglider:block/{kind}'}}})
    write(D/'suitcaseglider/loot_tables/blocks'/f'{kind}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':f'suitcaseglider:{kind}'}]}]})
for kind in ['limestone_flux','pig_iron_ingot','steel_ingot','blast_slag']:
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    if kind in ['pig_iron_ingot','steel_ingot']:
        d.polygon([(3,18),(11,9),(26,10),(30,18),(23,24),(5,24)],fill='#4f5961',outline='#28323a');d.polygon([(4,17),(12,10),(25,11),(28,17),(20,21)],fill='#b9c8d1' if kind=='steel_ingot' else '#a2a6a3');d.line((6,23,23,23),fill='#727c80')
    else:
        base='#c5bfa1'if kind=='limestone_flux'else'#4b4d44';light='#e2dcc0'if kind=='limestone_flux'else'#81816b'
        for x,y,r in [(8,21,5),(16,17,7),(24,22,5),(13,25,6)]:d.polygon([(x-r,y+2),(x-1,y-r),(x+r,y),(x+r-1,y+4),(x-3,y+5)],fill=base,outline='#494b42');d.line((x-r+1,y+1,x-1,y-r+1,x+r-1,y),fill=light,width=2)
    p=A/'textures/item'/f'{kind}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
    write(A/'models/item'/f'{kind}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'suitcaseglider:item/{kind}'}})
def shapeless(name,ingredients,result,count=1):write(D/'suitcaseglider/recipes'/f'{name}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':i if ':' in i else 'suitcaseglider:'+i}for i in ingredients],'result':{'item':'suitcaseglider:'+result,'count':count}})
shapeless('limestone',['minecraft:calcite'],'limestone')
shapeless('limestone_flux',['limestone'],'limestone_flux',4)
shapeless('slag_bricks',['blast_slag']*4,'slag_bricks',4)
shapeless('pig_iron_block',['pig_iron_block'],'steel_block')
shapeless('legacy_pig_iron_to_steel',['pig_iron_ingot'],'steel_ingot')
shapeless('steel_unpack',['steel_block'],'steel_ingot',9)
write(D/'suitcaseglider/recipes/steel_block.json',{'type':'minecraft:crafting_shaped','pattern':['III']*3,'key':{'I':{'item':'suitcaseglider:steel_ingot'}},'result':{'item':'suitcaseglider:steel_block'}})
# Keep the existing recipe ID so worlds/datapacks retain their recipe reference.
write(D/'suitcaseglider/recipes/blast_pig_iron.json',{'type':'suitcaseglider:blast_smelting','ore':[{'tag':'forge:ores/iron'},{'tag':'forge:raw_materials/iron'},{'tag':'forge:ingots/iron'}],'coke':{'item':'suitcaseglider:coke'},'flux':{'item':'suitcaseglider:limestone_flux'},'result':{'item':'suitcaseglider:steel_ingot'},'slag':{'item':'suitcaseglider:blast_slag'},'duration':1200,'temperature':1200,'energy':32})
for kind in ['limestone','slag_bricks','pig_iron_block','steel_block']:
    p=D/'minecraft/tags/blocks/mineable/pickaxe.json';d=json.loads(p.read_text());d['values']=list(dict.fromkeys(d['values']+['suitcaseglider:'+kind]));write(p,d)
for path,value in [('forge/tags/items/ingots/pig_iron.json','pig_iron_ingot'),('forge/tags/items/storage_blocks/pig_iron.json','pig_iron_block'),('forge/tags/items/dusts/limestone.json','limestone_flux'),('forge/tags/items/slag.json','blast_slag'),('forge/tags/items/ingots/steel.json','steel_ingot'),('forge/tags/items/storage_blocks/steel.json','steel_block')]:write(D/path,{'replace':False,'values':['suitcaseglider:'+value]})
# GUI follows the vanilla 176 x 166 footprint and 18-pixel inventory pitch.
im=Image.new('RGBA',(176,166),'#c6c6c6');d=ImageDraw.Draw(im)
d.rectangle((0,0,175,165),fill='#373737');d.rectangle((1,1,173,163),fill='#ffffff');d.rectangle((3,3,172,162),fill='#c6c6c6');d.line((3,162,172,162,172,3),fill='#777777',width=2)
def slot(x,y):
    d.rectangle((x-1,y-1,x+16,y+16),fill='#ffffff');d.line((x-1,y+15,x-1,y-1,x+15,y-1),fill='#373737');d.rectangle((x,y,x+15,y+15),fill='#8b8b8b')
for x,y in [(35,17),(35,53),(62,53),(116,17),(116,53)]:slot(x,y)
for y in [84,102,120,142]:
    for x in range(8,153,18):slot(x,y)
for x,w in [(8,11),(151,12)]:d.rectangle((x,16,x+w,70),fill='#555555');d.rectangle((x+1,17,x+w-1,69),fill='#292a27')
d.polygon([(99,34),(107,40),(99,46)],fill='#777777')
p=A/'textures/gui/blast_furnace.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
texts={
'container.suitcaseglider.blast_furnace':('Blast Furnace','Доменная печь'),
'gui.suitcaseglider.blast.temperature':('Temperature: %s / %s °C','Температура: %s / %s °C'),
'gui.suitcaseglider.blast.energy':('Energy: %s / %s FE; blower: %s FE/t','Энергия: %s / %s FE; нагнетатель: %s FE/т'),
}
for i,(en,ru)in enumerate([('Not assembled','Не сформирована'),('Waiting for charge','Ожидание загрузки'),('Heating','Разогрев'),('Smelting','Плавка'),('No power','Нет питания'),('Output full','Выход заполнен'),('Chunks unloaded','Часть конструкции не загружена')]):texts[f'gui.suitcaseglider.blast.state.{i}']=(en,ru)
for id,en,ru in [('ore','Iron ingot, iron ore or raw iron','Железный слиток, руда или сырое железо'),('coke','Coke','Кокс'),('flux','Limestone flux','Известняковый флюс'),('iron','Steel output','Выход стали'),('slag','Slag output','Выход шлака')]:texts['gui.suitcaseglider.blast.slot.'+id]=(en,ru)
for p in (A/'lang').glob('*.json'):
    lang=json.loads(p.read_text(encoding='utf-8-sig'));ru=p.stem=='ru_ru'
    for kind,n in names.items():lang[('block.'if kind in ['limestone','slag_bricks','pig_iron_block','steel_block']else'item.')+'suitcaseglider.'+kind]=n[ru]
    lang.update({k:v[ru]for k,v in texts.items()});write(p,lang)
print('Blast runtime: materials, recipes, tags and compact GUI exported')
