"""Deterministic pixel-art material icons and the first survival recipes."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/suitcaseglider'
NAMES={
    'unfired_refractory_brick':('Unfired Refractory Brick','Необожжённый огнеупорный кирпич','Fire in a furnace to harden.','Обожгите в печи для придания прочности.'),
    'refractory_brick':('Refractory Brick','Огнеупорный кирпич','Heat-resistant material for furnace construction.','Жаростойкий материал для строительства печей.'),
    'coke':('Coke','Кокс','Long-burning fuel and metallurgy ingredient.','Долгогорящее топливо и металлургическое сырьё.'),
}

from item_icons import export_icons
export_icons(['unfired_refractory_brick','refractory_brick','coke'])
for name in NAMES:
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

print('Exported 16 px material icons, recipes, recipe-book unlocks, translations and coke fuel tag.')
