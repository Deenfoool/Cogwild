"""Cogwild's 16 px item art. Vanilla 1.20.1 material families, native pixel tooling.

Existing editable Python icon sources share this module so recipe/asset exporters
cannot silently restore the older 32 px drawings. No filtering or antialiasing.
Vanilla sprites are read from the local game jar as shape/palette references;
the jar and unmodified reference sprites are not shipped with the mod.
"""
import io,os,zipfile,argparse
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parent.parent
DEST=ROOT/'src/main/resources/assets/suitcaseglider/textures/item'
NAMES=['steel_ingot','pig_iron_ingot','unfired_refractory_brick','refractory_brick','coke','limestone_flux','blast_slag','copper_wire','copper_coil','magnet','motor_housing','electric_motor','guild_assembly_key','power_cable','creosote_bucket','glider_frame','glider_fabric']
STEEL=['#30363c','#4d575e','#69777f','#8f9da5','#b4c1c5','#dae1df']
IRON=['#292b2b','#505454','#777c7a','#a8aeab','#d4d9d5','#f0f2e9']
COPPER=['#4c271c','#783c26','#a65332','#cd784a','#eda16d','#ffd0a2']

def game_jar():
    p=Path(os.environ.get('COGWILD_MINECRAFT_JAR',Path.home()/'.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'))
    if not p.is_file():raise FileNotFoundError('Set COGWILD_MINECRAFT_JAR to a Minecraft 1.20.1 client jar')
    return p

def pixels(im):return [im.getpixel((x,y)) for y in range(im.height) for x in range(im.width)]

def rgba(hex):return tuple(bytes.fromhex(hex.lstrip('#')))+(255,)
def shade(rgb):return rgb[0]*.2126+rgb[1]*.7152+rgb[2]*.0722
def vanilla(name):
    with zipfile.ZipFile(game_jar())as z:return Image.open(io.BytesIO(z.read('assets/minecraft/textures/item/'+name+'.png'))).convert('RGBA')
def family(name,palette):
    """Keep the native silhouette and clustered lighting; replace the material ramp."""
    im=vanilla(name);values=sorted(set(round(shade(p),3) for p in pixels(im) if p[3]))
    for y in range(16):
        for x in range(16):
            p=im.getpixel((x,y))
            if p[3]:
                rank=values.index(round(shade(p),3));i=round(rank*(len(palette)-1)/max(1,len(values)-1));im.putpixel((x,y),rgba(palette[i]))
            else:im.putpixel((x,y),(0,0,0,0))
    return im
def canvas():
    im=Image.new('RGBA',(16,16));return im,ImageDraw.Draw(im)

def coil():
    im,d=canvas()
    # Diagonal bobbin, with a hollow iron core visible between copper windings.
    d.polygon([(3,8),(8,2),(12,2),(15,5),(15,8),(9,14),(5,14),(1,10)],fill=IRON[0])
    d.polygon([(3,8),(8,3),(11,3),(14,6),(9,11),(5,12)],fill=COPPER[2])
    d.polygon([(4,10),(9,5),(11,4),(12,5),(7,10),(5,12)],fill=COPPER[4])
    d.line([(6,6),(9,3),(12,3),(14,5)],fill=COPPER[5])
    d.line([(8,9),(13,4)],fill=COPPER[1],width=1)
    d.line([(9,11),(14,6)],fill=COPPER[4],width=1)
    d.line([(10,12),(14,8)],fill=COPPER[1],width=1)
    d.polygon([(2,8),(5,7),(9,10),(9,13),(6,14),(2,11)],fill=IRON[2])
    d.line([(2,8),(5,8),(8,10)],fill=IRON[4])
    d.polygon([(4,9),(6,9),(7,10),(7,12),(5,12),(4,11)],fill=IRON[0])
    d.line([(3,11),(5,13),(7,13)],fill=IRON[1])
    return im

def magnet():
    im,d=canvas()
    # A compact horseshoe with stepped metal thickness, rather than flat coloured bars.
    d.line([(4,2),(3,8),(5,12),(9,13),(12,10),(13,3)],fill=IRON[0],width=5)
    d.line([(4,2),(3,8),(5,11),(9,12),(11,10),(12,3)],fill=IRON[2],width=3)
    d.line([(2,5),(2,8),(4,11),(7,12)],fill=IRON[4])
    d.line([(5,10),(8,11),(10,9)],fill=IRON[1])
    d.rectangle((3,2,5,5),fill='#af302b');d.line((3,2,3,5),fill='#ed6550');d.point((5,5),fill='#712020')
    d.rectangle((11,3,13,6),fill='#305aa2');d.line((11,3,11,5),fill='#7399ce');d.line((13,4,13,6),fill='#223665')
    d.line((3,1,5,1),fill=IRON[4]);d.line((11,2,13,2),fill=IRON[4])
    return im

def motor(complete):
    im,d=canvas()
    d.polygon([(2,5),(7,2),(12,2),(15,5),(15,11),(10,15),(3,15),(1,12),(1,7)],fill=IRON[0])
    d.polygon([(2,5),(7,3),(11,3),(14,5),(9,8),(3,7)],fill=IRON[3])
    d.polygon([(10,8),(14,6),(14,10),(10,14)],fill=IRON[1])
    d.rectangle((2,7,9,12),fill=IRON[2]);d.line((3,13,8,13),fill=IRON[1])
    d.line((3,6,8,7),fill=IRON[4]);d.line((2,8,2,11),fill=IRON[3])
    for x in [6,8,10]:d.line([(x,4),(x+2,5),(x+2,9)],fill=IRON[1])
    for p in [(3,7),(8,12),(12,5)]:d.point(p,fill=IRON[4])
    d.rectangle((4,8,7,11),fill=IRON[0])
    if complete:
        d.line([(4,8),(6,8),(7,9)],fill=COPPER[4]);d.line([(4,9),(4,11),(6,11)],fill=COPPER[2])
        d.polygon([(1,11),(4,9),(6,10),(3,13),(1,13)],fill=COPPER[1]);d.line([(1,11),(4,10),(5,10)],fill=COPPER[5]);d.line([(1,12),(3,12),(5,10)],fill=COPPER[3])
        d.rectangle((7,1,10,2),fill=IRON[0]);d.line((7,1,9,1),fill=IRON[3])
    return im

def key():
    # Author-provided wrench: keep its ring, handle and jaw instead of regenerating it.
    return Image.open(ROOT/'art_source/item_icons/guild_assembly_key.png').convert('RGBA')

def bucket():
    im=vanilla('water_bucket')
    # Preserve the native iron rim/walls. Only water-blue pixels become dark amber creosote.
    blues=[p for p in pixels(im) if p[3] and p[2]>p[0]+35 and p[2]>p[1]+10]
    values=sorted(set(round(shade(p),3)for p in blues));ramp=['#2d1b0e','#4a2c12','#70451d','#98672b','#bb8b40','#d5b367']
    for y in range(16):
        for x in range(16):
            p=im.getpixel((x,y))
            if p in blues:
                i=round(values.index(round(shade(p),3))*(len(ramp)-1)/max(1,len(values)-1));im.putpixel((x,y),rgba(ramp[i]))
    return im

def suitcase():
    # Preserve the approved original layers, including their original alpha values.
    return tuple(Image.open(ROOT/'art_source/item_icons'/(name+'.png')).convert('RGBA')
                 for name in ('glider_frame', 'glider_fabric'))


def icons():
    brick=['#5f442e','#8d6844','#b38e5d','#d0b17b','#e1cb99','#efe0b6']
    wet=['#4c4538','#6d6250','#8e8067','#ab9c7d','#b9ad91','#c6bea6']
    coke=family('coal',['#111113','#1d1d20','#2c2c30','#3c3c40','#505052','#69696a'])
    # Porous coke has a few deliberate dark cavities while retaining coal's chunky clusters.
    for x,y in [(6,5),(10,7),(5,10),(9,11)]:
        if coke.getpixel((x,y))[3]:coke.putpixel((x,y),rgba('#171719'))
    cable=family('string',['#17191a','#282c2d','#3a4041','#535a59','#737b76'])
    cd=ImageDraw.Draw(cable);cd.rectangle((8,0,9,1),fill=COPPER[3]);cd.point((8,0),fill=COPPER[5])
    frame,cloth=suitcase()
    return {'steel_ingot':family('iron_ingot',STEEL),'pig_iron_ingot':family('iron_ingot',['#292729','#443e40','#625b5b','#807978','#9a9390','#b0aaa1']),
        'refractory_brick':family('brick',brick),'unfired_refractory_brick':family('brick',wet),'coke':coke,
        'limestone_flux':family('glowstone_dust',['#777160','#9e957b','#bdb194','#d3c8aa','#e5dcc1','#f2ecd8']),
        'blast_slag':family('gunpowder',['#30312c','#494b40','#636558','#7e8070','#9b9b87','#b5b39c']),
        'copper_wire':family('string',COPPER),'copper_coil':coil(),'magnet':magnet(),'motor_housing':motor(False),'electric_motor':motor(True),
        'guild_assembly_key':key(),'power_cable':cable,'creosote_bucket':bucket(),'glider_frame':frame,'glider_fabric':cloth}

def export_icons(names=None):
    all_icons=icons();selected=NAMES if names is None else names;DEST.mkdir(parents=True,exist_ok=True)
    for name in selected:
        im=all_icons[name];original=name in ('glider_frame','glider_fabric')
        assert im.size==((32,32) if original or name=='guild_assembly_key' else (16,16))
        if not original:assert set(pixels(im.getchannel('A')))<={0,255}
        im.save(DEST/(name+'.png'))
    return all_icons

def preview(path):
    ims=icons();f=ims.pop('glider_frame');cloth=ims.pop('glider_fabric');ims.pop('pig_iron_ingot')
    # Show both the actual default colour and a dyed variant, using the same grayscale layer.
    def dressed(color):
        result=f.copy();tint=cloth.copy()
        for y in range(tint.height):
            for x in range(tint.width):
                r,g,b,a=tint.getpixel((x,y));tint.putpixel((x,y),(r*color[0]//255,g*color[1]//255,b*color[2]//255,a))
        result.alpha_composite(tint);return result
    ims['glider']=dressed((220,214,190));ims['glider_dyed']=dressed((59,181,196))
    labels={'steel_ingot':'Сталь','unfired_refractory_brick':'Сырой кирпич','refractory_brick':'Огнеупорный кирпич','coke':'Кокс','limestone_flux':'Известняковый флюс','blast_slag':'Шлак','copper_wire':'Медная проволока','copper_coil':'Медная катушка','magnet':'Магнит','motor_housing':'Корпус двигателя','electric_motor':'Электродвигатель','guild_assembly_key':'Монтажный ключ','power_cable':'Силовой кабель','creosote_bucket':'Креозот','glider':'Чемодан-планер','glider_dyed':'Окрашенный планер'}
    font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf' if Path('C:/Windows/Fonts/arial.ttf').exists() else 'DejaVuSans.ttf',16)
    sheet=Image.new('RGB',(800,((len(ims)+3)//4)*180+128),'#292c30');d=ImageDraw.Draw(sheet)
    d.text((18,10),'ВАНИЛЬНЫЕ ОРИЕНТИРЫ / MINECRAFT 1.20.1',font=font,fill='#c9c9c9')
    for i,name in enumerate(['iron_ingot','brick','coal','gunpowder','string','water_bucket']):
        sheet.paste(vanilla(name).resize((64,64),Image.Resampling.NEAREST),(24+i*128,39),vanilla(name).resize((64,64),Image.Resampling.NEAREST))
    for i,(name,im)in enumerate(ims.items()):
        x=i%4*200;y=128+i//4*180;d.rectangle((x+5,y+3,x+194,y+174),fill='#34383d')
        large=im.resize((112,112),Image.Resampling.NEAREST);sheet.paste(large,(x+44,y+13),large)
        label=labels.get(name,name);box=d.textbbox((0,0),label,font=font);d.text((x+(200-box[2])/2,y+143),label,font=font,fill='#e0ddd4')
    path=Path(path);path.parent.mkdir(parents=True,exist_ok=True);sheet.save(path)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--preview');args=p.parse_args();export_icons()
    if args.preview:preview(args.preview)
    print('Exported item layers: 16 px materials, the author-provided 32 px assembly key and original suitcase layers.')
