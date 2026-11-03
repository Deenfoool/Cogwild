"""Assembly key, coke oven screen and translations for the working multiblock."""
import json
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources';ASSETS=RES/'assets/suitcaseglider'
def write(p,j):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(j,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.polygon([(3,3),(8,1),(13,3),(12,7),(9,5),(7,7),(9,10),(13,9),(16,5),(19,8),(18,14),(14,17),(12,17),(7,29),(3,30),(1,26),(9,14),(5,12),(2,8)],fill='#26313a')
d.polygon([(4,3),(8,2),(11,3),(10,5),(7,4),(5,7),(9,12),(14,11),(17,7),(18,9),(17,13),(13,16),(10,15),(4,27),(2,26),(10,13),(6,11),(3,7)],fill='#a2adb0')
d.line([(4,4),(4,7),(7,11)],fill='#d9dcce',width=1)
d.polygon([(9,15),(12,17),(7,27),(3,26)],fill='#6e5140')
for y in [19,22,25]:d.line([(6-(y-19)//3,y),(9-(y-19)//3,y+1)],fill='#aa8660')
d.rectangle((10,10,13,12),fill='#bc923e');d.line((10,10,13,10),fill='#edd184')
im.save(ASSETS/'textures/item/guild_assembly_key.png')
write(ASSETS/'models/item/guild_assembly_key.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'suitcaseglider:item/guild_assembly_key'}})
write(RES/'data/suitcaseglider/recipes/guild_assembly_key.json',{'type':'minecraft:crafting_shaped','pattern':[' II','CCI',' L '],'key':{'I':{'item':'minecraft:iron_ingot'},'C':{'item':'minecraft:copper_ingot'},'L':{'item':'minecraft:leather'}},'result':{'item':'suitcaseglider:guild_assembly_key'}})
write(RES/'data/suitcaseglider/advancements/recipes/guild_assembly_key.json',{'parent':'minecraft:recipes/root','criteria':{'iron':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['minecraft:iron_ingot']} ]}}},'rewards':{'recipes':['suitcaseglider:guild_assembly_key']}})

im=Image.new('RGBA',(256,256),(0,0,0,0));d=ImageDraw.Draw(im)
d.rectangle((0,0,175,189),fill='#373c3d');d.rectangle((1,1,173,187),fill='#f2eee2');d.rectangle((4,4,171,186),fill='#8e9390');d.rectangle((4,4,170,185),fill='#c4c5ba')
d.rectangle((6,18,169,33),fill='#b4b6ab')
def slot(x,y):
 d.rectangle((x-1,y-1,x+16,y+16),fill='#f1eee3');d.rectangle((x-1,y-1,x+15,y+15),fill='#565c5b');d.rectangle((x,y,x+15,y+15),fill='#8c928d')
slot(44,40);slot(116,40)
for row in range(3):
 for col in range(9):slot(8+col*18,108+row*18)
for col in range(9):slot(8+col*18,166)
p=ASSETS/'textures/gui/coke_oven.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)

strings={
 'item.suitcaseglider.guild_assembly_key':('Guild Assembly Key','Монтажный ключ Гильдии'),
 'tooltip.suitcaseglider.guild_assembly_key':('Use on a multiblock controller to check and form its structure.','ПКМ по контроллеру: проверить и сформировать постройку.'),
 'container.suitcaseglider.coke_oven':('Coke Oven','Коксовая печь'),
 'message.suitcaseglider.kiln.need_key':('Form this structure with a Guild Assembly Key.','Сформируйте постройку монтажным ключом Гильдии.'),
 'message.suitcaseglider.kiln.invalid':('Structure incomplete: %s incorrect cells.','Ошибка конструкции: неверных ячеек — %s.'),
 'message.suitcaseglider.kiln.unloaded':('Structure crosses an unloaded chunk.','Часть конструкции находится в незагруженном чанке.'),
 'message.suitcaseglider.kiln.cell':('Check block at %s, %s, %s (brick, hatch facing or hollow centre).','Проверьте блок %s, %s, %s: кладку, поворот люка или пустой центр.'),
 'message.suitcaseglider.kiln.formed':('Coke oven formed. Right-click to open.','Коксовая печь сформирована. ПКМ — открыть интерфейс.'),
 'gui.suitcaseglider.kiln.start':('Start','Запустить'),
 'gui.suitcaseglider.kiln.stop':('Stop','Остановить'),
 'gui.suitcaseglider.kiln.state.0':('Structure damaged','Корпус не сформирован'),
 'gui.suitcaseglider.kiln.state.1':('Stopped','Остановлено'),
 'gui.suitcaseglider.kiln.state.2':('Waiting for coal','Ожидание угля'),
 'gui.suitcaseglider.kiln.state.3':('Coking','Коксование'),
 'gui.suitcaseglider.kiln.state.4':('Output full','Выход заполнен'),
 'gui.suitcaseglider.kiln.state.5':('Paused','Приостановлено'),
 'gui.suitcaseglider.kiln.state.6':('Waiting for chunks','Ожидание чанков'),
 'tooltip.suitcaseglider.coke':('Fuel and metallurgy ingredient. Produced in a coke oven.','Топливо и металлургическое сырьё. Производится в коксовой печи.'),
}
for locale,ru in [('en_us',False),('ru_ru',True)]:
 p=ASSETS/'lang'/f'{locale}.json';j=json.loads(p.read_text(encoding='utf-8'))
 for k,v in strings.items():j[k]=v[ru]
 write(p,j)

# Keep inactive blocks unchanged; working hatches show a warm slit while LIT.
im=Image.new('RGB',(64,64));p=im.load()
for y in range(64):
 for x in range(64):p[x,y]=(int(175+55*(1-y/64)),int(56+70*(1-y/64)),18)
im.save(ASSETS/'textures/block/kiln_heat.png')
for kind in ['coke_oven_controller','coke_oven_input','coke_oven_output']:
 p=ASSETS/'models/block'/f'{kind}.json';j=json.loads(p.read_text());j['textures']['heat']='suitcaseglider:block/kiln_heat'
 for e in j['elements']:
  if e['name'] in ['Sealed inspection slit']:e['faces']['north']['texture']='#heat'
 write(ASSETS/'models/block'/f'{kind}_lit.json',j)
 write(ASSETS/'blockstates'/f'{kind}.json',{'variants':{f'facing={face},lit={str(lit).lower()}':{'model':'suitcaseglider:block/'+kind+('_lit' if lit else ''),'y':angle} for face,angle in [('north',0),('east',90),('south',180),('west',270)] for lit in [False,True]}})
print('Exported reusable assembly key, recipe, GUI, runtime translations and active block models.')
