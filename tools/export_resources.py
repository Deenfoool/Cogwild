"""Deterministic pixel-art material icons and the first survival recipes."""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/suitcaseglider'
NAMES={
    'unfired_refractory_brick':('Unfired Refractory Brick','Необожжённый огнеупорный кирпич','Fire in a furnace to harden.','Обожгите в печи для придания прочности.'),
    'refractory_brick':('Refractory Brick','Огнеупорный кирпич','Heat-resistant material for furnace construction.','Жаростойкий материал для строительства печей.'),
    'coke':('Coke','Кокс','Long-burning fuel and a future metallurgy ingredient.','Долгогорящее топливо и сырьё для будущей металлургии.'),
}

def polygon(draw,points,fill):draw.polygon(points,fill=fill)

def brick(unfired):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    polygon(d,[(2,12),(20,5),(30,10),(30,21),(12,28),(2,23)],'#343234')
    polygon(d,[(3,13),(20,6),(29,11),(12,18)],'#b9ad95' if unfired else '#e1cba0')
    polygon(d,[(3,14),(11,19),(11,26),(3,22)],'#756e66' if unfired else '#a77b55')
    polygon(d,[(12,19),(29,12),(29,20),(12,27)],'#958878' if unfired else '#bd9a70')
    # Broad material variation with small coherent clay/sinter bands.
    for a,b,color in [((5,13),(20,7),'#d1c7b1' if unfired else '#f1deba'),((13,19),(28,13),'#b7a78e' if unfired else '#d4b88b'),((13,25),(27,20),'#817766' if unfired else '#a98560')]:d.line([a,b],fill=color,width=1)
    d.line([(5,21),(10,24)],fill='#68635d' if unfired else '#966b4d')
    d.line([(17,17),(19,20),(22,19)],fill='#82786a' if unfired else '#a98b65')
    if unfired:
        d.line([(8,13),(13,11),(19,9)],fill='#a69a85')
        d.line([(17,22),(22,20)],fill='#c2b199')
    else:
        for x,y in [(9,12),(21,10),(25,16),(16,23)]:d.point((x,y),fill='#f0d9ae')
    return im

def coke():
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    polygon(d,[(2,19),(5,12),(10,10),(14,4),(23,5),(28,11),(30,20),(25,26),(13,29),(5,26)],'#151d29')
    for points,color in [([(5,13),(11,11),(16,15),(13,21),(3,20)],'#3f4b59'),([(14,5),(23,6),(20,13),(16,14),(11,10)],'#65717b'),([(23,7),(27,12),(29,19),(24,21),(20,14)],'#35424e'),([(15,16),(20,15),(24,22),(20,26),(13,27),(10,23)],'#4c5863'),([(5,22),(10,24),(12,27),(6,25)],'#26323f')]:polygon(d,points,color)
    for line,col in [([(14,6),(19,7),(21,6)],'#859095'),([(6,14),(9,13)],'#7b858a'),([(15,17),(18,17),(21,23)],'#707c85'),([(11,12),(15,15),(12,21)],'#202d3a'),([(22,12),(24,16),(23,20)],'#202d3a'),([(6,24),(8,25)],'#55626e')]:d.line(line,fill=col)
    for x,y in [(16,9),(20,10),(7,17),(18,20),(24,23)]:d.rectangle((x,y,x+1,y+1),fill='#26323d')
    return im

icons={'unfired_refractory_brick':brick(True),'refractory_brick':brick(False),'coke':coke()}
for name,im in icons.items():
    path=ASSETS/'textures/item'/f'{name}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
    path=ASSETS/'models/item'/f'{name}.json';path.write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'suitcaseglider:item/{name}'}},indent=2)+'\n')

recipes=RES/'data/suitcaseglider/recipes'
craft={'type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:clay_ball'}]*4+[{'tag':'minecraft:sand'}]*4+[{'item':'minecraft:gravel'}],'result':{'item':'suitcaseglider:unfired_refractory_brick','count':8}}
firing={'type':'minecraft:smelting','ingredient':{'item':'suitcaseglider:unfired_refractory_brick'},'result':'suitcaseglider:refractory_brick','experience':.1,'cookingtime':200}
for name,recipe in [('unfired_refractory_brick',craft),('refractory_brick',firing)]:
    (recipes/f'{name}.json').write_text(json.dumps(recipe,indent=2)+'\n')
    trigger='minecraft:clay_ball' if name.startswith('unfired') else 'suitcaseglider:unfired_refractory_brick'
    advancement={'parent':'minecraft:recipes/root','criteria':{'material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[trigger]}]}}},'rewards':{'recipes':[f'suitcaseglider:{name}']}}
    path=RES/'data/suitcaseglider/advancements/recipes'/f'{name}.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(advancement,indent=2)+'\n')

for locale,ru in [('en_us',False),('ru_ru',True)]:
    path=ASSETS/'lang'/f'{locale}.json';lang=json.loads(path.read_text(encoding='utf-8-sig'))
    for name,strings in NAMES.items():
        lang[f'item.suitcaseglider.{name}']=strings[1 if ru else 0]
        lang[f'tooltip.suitcaseglider.{name}']=strings[3 if ru else 2]
    path.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
path=RES/'data/forge/tags/items/fuels/coke.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps({'replace':False,'values':['suitcaseglider:coke']},indent=2)+'\n')

# A review image; the game receives the original crisp 32x32 assets.
preview=Image.new('RGBA',(3*224,264),'#252b32');draw=ImageDraw.Draw(preview)
for i,(name,im) in enumerate(icons.items()):
    preview.alpha_composite(im.resize((192,192),Image.Resampling.NEAREST),(i*224+16,16))
    draw.text((i*224+12,224),['Unfired brick','Refractory brick','Coke'][i],fill='#e6ded0')
path=ROOT/'../../outputs/cogwild-0.6.2/material-icons.png';path.parent.mkdir(parents=True,exist_ok=True);preview.save(path)
print('Exported 3 icons, 2 recipes, 2 recipe-book unlocks, translations and coke fuel tag.')
