"""Assembly key, coke oven screen and translations for the working multiblock."""
import json
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources';ASSETS=RES/'assets/suitcaseglider'
def write(p,j):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(j,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
from item_icons import export_icons
export_icons(['guild_assembly_key'])
write(ASSETS/'models/item/guild_assembly_key.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'suitcaseglider:item/guild_assembly_key'}})
write(RES/'data/suitcaseglider/recipes/guild_assembly_key.json',{'type':'minecraft:crafting_shaped','pattern':[' II','CCI',' L '],'key':{'I':{'item':'minecraft:iron_ingot'},'C':{'item':'minecraft:copper_ingot'},'L':{'item':'minecraft:leather'}},'result':{'item':'suitcaseglider:guild_assembly_key'}})
write(RES/'data/suitcaseglider/advancements/recipes/guild_assembly_key.json',{'parent':'minecraft:recipes/root','criteria':{'iron':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['minecraft:iron_ingot']} ]}}},'rewards':{'recipes':['suitcaseglider:guild_assembly_key']}})

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
print('Exported reusable assembly key, recipe, runtime translations and active block models.')
