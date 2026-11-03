"""Charcoal storage block, creosote bucket and coke-oven fluid/fuel labels."""
import json,math
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';A=RES/'assets/suitcaseglider'
def write(p,j):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(j,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
im=Image.new('RGB',(32,32));p=im.load()
for y in range(32):
 for x in range(32):
  u=x%16;v=y%16;s=4*math.sin(u*.6)+3*math.cos(v*.8);joint=min(u,v)<1
  base=(21,20,19) if joint else (47,42,36)
  p[x,y]=tuple(max(0,int(c+s)) for c in base)
d=ImageDraw.Draw(im)
for x,y in [(4,3),(19,5),(6,20),(22,20)]:d.line([(x,y),(x+4,y+3),(x+2,y+7),(x+6,y+9)],fill='#221e1a');d.line([(x+1,y),(x+5,y+3)],fill='#625a4b')
im.save(A/'textures/block/charcoal_block.png')
write(A/'models/block/charcoal_block.json',{'parent':'minecraft:block/cube_all','textures':{'all':'suitcaseglider:block/charcoal_block'}})
write(A/'models/item/charcoal_block.json',{'parent':'suitcaseglider:block/charcoal_block'})
write(A/'blockstates/charcoal_block.json',{'variants':{'':{'model':'suitcaseglider:block/charcoal_block'}}})
write(A/'models/block/creosote.json',{'textures':{'particle':'minecraft:block/water_still'}})
write(A/'blockstates/creosote.json',{'variants':{'':{'model':'suitcaseglider:block/creosote'}}})
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.polygon([(5,9),(8,5),(24,5),(27,9),(24,28),(8,28)],fill='#26313b')
d.polygon([(7,10),(25,10),(22,26),(10,26)],fill='#89999f');d.polygon([(7,10),(11,12),(13,25),(10,25)],fill='#c5cacc');d.polygon([(20,12),(25,10),(22,25),(18,25)],fill='#596e78')
d.ellipse((6,6,25,14),fill='#b9bfc1',outline='#354551',width=2);d.ellipse((9,8,22,12),fill='#70401a');d.arc((10,8,22,12),180,300,fill='#b7823f',width=1)
d.arc((6,10,25,27),0,180,fill='#364b56',width=2);d.arc((8,11,23,25),0,180,fill='#d4d7cf',width=1)
im.save(A/'textures/item/creosote_bucket.png')
write(A/'models/item/creosote_bucket.json',{'parent':'minecraft:item/generated','textures':{'layer0':'suitcaseglider:item/creosote_bucket'}})
write(RES/'data/suitcaseglider/loot_tables/blocks/charcoal_block.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'suitcaseglider:charcoal_block'}]}]})
write(RES/'data/suitcaseglider/recipes/charcoal_block.json',{'type':'minecraft:crafting_shaped','pattern':['CCC','CCC','CCC'],'key':{'C':{'item':'minecraft:charcoal'}},'result':{'item':'suitcaseglider:charcoal_block'}})
write(RES/'data/suitcaseglider/recipes/charcoal_from_block.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'suitcaseglider:charcoal_block'}],'result':{'item':'minecraft:charcoal','count':9}})
for name,trigger in [('charcoal_block','minecraft:charcoal'),('charcoal_from_block','suitcaseglider:charcoal_block')]:
 write(RES/'data/suitcaseglider/advancements/recipes'/f'{name}.json',{'parent':'minecraft:recipes/root','criteria':{'material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[trigger]}]}}},'rewards':{'recipes':['suitcaseglider:'+name]}})
for kind in ['items','blocks']:
 write(RES/f'data/forge/tags/{kind}/storage_blocks/charcoal.json',{'replace':False,'values':['suitcaseglider:charcoal_block']})
write(RES/'data/forge/tags/fluids/creosote.json',{'replace':False,'values':['suitcaseglider:creosote','suitcaseglider:flowing_creosote']})
p=RES/'data/minecraft/tags/blocks/mineable/pickaxe.json';j=json.loads(p.read_text());j['values']=list(dict.fromkeys(j['values']+['suitcaseglider:charcoal_block']));write(p,j)
strings={
 'block.suitcaseglider.charcoal_block':('Block of Charcoal','Блок древесного угля'),
 'block.suitcaseglider.creosote':('Creosote','Креозот'),
 'fluid_type.suitcaseglider.creosote':('Creosote','Креозот'),
 'item.suitcaseglider.creosote_bucket':('Creosote Bucket','Ведро креозота'),
 'gui.suitcaseglider.kiln.creosote':('Creosote: %s / %s mB. Click with an empty bucket.','Креозот: %s / %s мБ. Нажмите с пустым ведром на курсоре.'),
 'gui.suitcaseglider.kiln.state.7':('Not enough fuel for the cycle','Недостаточно топлива на цикл'),
 'tooltip.suitcaseglider.coke_oven_output':('Coke output, fuel-block input and creosote drain. Use an empty bucket.','Выход кокса, загрузка топливных блоков и слив креозота. ПКМ пустым ведром — набрать.'),
}
for loc,ru in [('en_us',False),('ru_ru',True)]:
 p=A/'lang'/f'{loc}.json';j=json.loads(p.read_text(encoding='utf-8'))
 for k,v in strings.items():j[k]=v[ru]
 write(p,j)
print('Exported charcoal block, reversible recipes, creosote bucket and fluid resources.')
