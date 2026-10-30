"""Export the approved Notion catalog without substituting future gameplay conditions."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
RES=ROOT/'src/main/resources'
catalog=json.loads((ROOT/'docs/advancements-notion-source.json').read_text(encoding='utf-8-sig'))
assert len(catalog)==24 and len({a['id'] for a in catalog})==24

def inventory(tag):
    return {'trigger':'minecraft:inventory_changed','conditions':{'items':[{'tag':tag}]}}

def event(action,minimum=1):
    return {'trigger':'cogwild:industrial_progress','conditions':{'action':action,'minimum':minimum}}

events={
    'steel_ingot':('steel_smelted',1),
    'brass_ingot':('brass_smelted',1),
    'compressed_air':('compressed_air_tank_filled',1),
    'construction_yard':('construction_yard_formed',1),
    'first_mech':('mech_assembled',1),
    'mech_ant':('mech_ant_assembled',1),
    'mech_bee':('mech_bee_assembled',1),
    'robobuffalo':('robobuffalo_assembled',1),
    'first_oil':('oil_extracted',1),
    'diesel':('diesel_produced',1000),
    'biodiesel':('biodiesel_produced',1),
    'nitrogen':('nitrogen_produced',1),
    'full_distillation':('distillation_cycle_completed',1),
    'lubricant':('lubricant_produced',1),
    'diesel_generator':('diesel_generator_started',1),
    'aviation_fuel':('aviation_fuel_produced',1),
    'boost_fuel':('boost_fuel_produced',1),
    'hardened_steel':('hardened_steel_produced',1),
}
adv_dir=RES/'data/cogwild/advancements';adv_dir.mkdir(parents=True,exist_ok=True)
models=RES/'assets/suitcaseglider/models/item'
for a in catalog:
    name=a['id'].split(':')[1]
    if name=='root':criteria={'steel':inventory('forge:ingots/steel')}
    elif name=='first_machine':criteria={'machine':inventory('cogwild:advancement/first_machines')}
    elif name=='rare_ores':criteria={'tungsten':inventory('forge:ores/tungsten'),'zinc':inventory('forge:ores/zinc')}
    elif name=='crusher':criteria={'crusher':inventory('cogwild:advancement/crushers'),'processed_ore':event('ore_crushed')}
    elif name=='first_blueprint':criteria={
        'crafted':event('blueprint_crafted'),
        'traded':{'trigger':'minecraft:villager_trade','conditions':{'item':{'tag':'cogwild:mech_blueprints'}}},
    }
    elif name=='mechanical_menagerie':criteria={n:event(n+'_assembled') for n in ['mech_ant','robobuffalo','mech_bee']}
    else:criteria={'complete':event(*events[name])}
    display={'icon':{'item':'suitcaseglider:advancement_icon_'+name},
             'title':{'translate':'advancement.cogwild.'+name+'.title'},
             'description':{'translate':'advancement.cogwild.'+name+'.description'},
             'frame':a['frame'],'show_toast':True,'announce_to_chat':True,'hidden':a['hidden']}
    if not a['parent']:display['background']='minecraft:textures/gui/advancements/backgrounds/stone.png'
    advancement={'display':display,'criteria':criteria,'requirements':[list(criteria)] if name=='first_blueprint' else [[c] for c in criteria],'rewards':{'experience':a['experience']}}
    if a['parent']:advancement['parent']=a['parent']
    (adv_dir/(name+'.json')).write_text(json.dumps(advancement,ensure_ascii=False,indent=2),encoding='utf-8')
    (models/('advancement_icon_'+name+'.json')).write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'suitcaseglider:item/advancement/'+name}}))
    a['criteria']=criteria

(RES/'data/cogwild/advancement_catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2),encoding='utf-8')
tags=RES/'data/cogwild/tags/items';(tags/'advancement').mkdir(parents=True,exist_ok=True)
for name,values in [('advancement/first_machines',['suitcaseglider:crusher']),('advancement/crushers',['suitcaseglider:crusher']),('mech_blueprints',[])]:
    (tags/(name+'.json')).write_text(json.dumps({'replace':False,'values':values},indent=2))
for path in (RES/'assets/suitcaseglider/lang').glob('*.json'):
    lang=json.loads(path.read_text(encoding='utf-8-sig'))
    for a in catalog:
        stem='advancement.cogwild.'+a['id'].split(':')[1]
        lang[stem+'.title']=a['title']
        lang[stem+'.description']=a['description']+'\n'+a['condition']
        lang['item.suitcaseglider.advancement_icon_'+a['id'].split(':')[1]]=a['title']
    path.write_text(json.dumps(lang,ensure_ascii=False,indent=2),encoding='utf-8')
print('Exported',len(catalog),'advancements;',sum(a['experience'] for a in catalog),'XP;',sum(a['hidden'] for a in catalog),'hidden')
