"""Native 176x166 container textures matching the approved Cogwild metal UI concept."""
from pathlib import Path
from PIL import Image, ImageDraw
import math
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'src/main/resources/assets/suitcaseglider/textures/gui/machines'
PROFILES={'lathe':(1,1),'crusher':(1,1),'rolling_mill':(1,1),'quenching_bath':(2,1),'air_separator':(1,2),'air_compressor':(0,0),'industrial_press':(1,1),'diesel_generator':(0,0),'fermenter':(3,2),'universal_mixer':(3,1)}
def slot(d,x,y):
    d.rectangle((x-1,y-1,x+16,y+16),fill='#e5e1d7');d.line((x-1,y+15,x-1,y-1,x+15,y-1),fill='#4b4d4d');d.rectangle((x,y,x+15,y+15),fill='#90938f')
def gear(d,cx,cy):
    for k in range(8):
        a=k*math.pi/4;x=round(cx+6*math.cos(a));y=round(cy+6*math.sin(a));d.rectangle((x-1,y-1,x+1,y+1),fill='#424b50')
    d.ellipse((cx-6,cy-6,cx+6,cy+6),fill='#424b50');d.ellipse((cx-4,cy-4,cx+4,cy+4),fill='#8e969a');d.arc((cx-4,cy-4,cx+4,cy+4),170,290,fill='#c1c6c6');d.ellipse((cx-2,cy-2,cx+2,cy+2),fill='#4e585e')
def generate(kind,inputs,outputs):
    im=Image.new('RGBA',(176,166),(0,0,0,0));d=ImageDraw.Draw(im)
    d.rectangle((1,1,174,164),fill='#333c43');d.rectangle((2,2,173,163),fill='#8a96a2');d.line((3,3,172,3),fill='#c1c8ce');d.line((3,3,3,162),fill='#b6bfc7');d.line((172,4,172,162,4,162),fill='#4c5968')
    d.rectangle((4,4,171,17),fill='#909da9');d.line((5,18,170,18),fill='#9a7c24');d.line((5,19,170,19),fill='#e0bb3b')
    d.rectangle((5,20,170,72),fill='#b8b9b4');d.line((5,20,170,20),fill='#e0dfd5');d.line((5,72,170,72),fill='#777e7f')
    d.rectangle((5,73,170,160),fill='#b8b9b4')
    for x,y in ((3,3),(169,3),(3,158),(169,158)):
        d.rectangle((x,y,x+3,y+3),fill='#515f6c');d.rectangle((x,y,x+2,y+2),fill='#b3bbc0');d.point((x,y),fill='#e1e4e0')
    d.rectangle((156,5,164,13),fill='#354136');d.rectangle((157,6,163,12),fill='#596c4d')
    d.rectangle((9,22,19,69),fill='#555a58');d.rectangle((10,23,18,68),fill='#303b3f');d.line((11,23,17,23),fill='#1e2528')
    if inputs:
        for i in range(inputs):slot(d,44 if inputs==1 else 28+i*18,34)
        for i in range(outputs):slot(d,132,34 if outputs==1 else 24+i*26)
        d.rectangle((85,53,115,58),fill='#4c534e');d.rectangle((86,54,114,57),fill='#87877b');d.polygon([(115,51),(120,56),(115,61)],fill='#4c534e');d.polygon([(115,53),(118,56),(115,59)],fill='#b3a267')
        if kind=='crusher':gear(d,93,35);gear(d,108,35)
        elif kind=='air_separator':
            d.rounded_rectangle((86,22,114,50),radius=4,fill='#434d53');d.ellipse((89,25,111,47),fill='#27353e')
            for a in range(0,360,90):
                r=math.radians(a);cx,cy=100,36
                d.polygon([(cx,cy),(round(cx+9*math.cos(r)),round(cy+9*math.sin(r))),(round(cx+7*math.cos(r+.65)),round(cy+7*math.sin(r+.65)))],fill='#a6b0b4')
            d.ellipse((98,34,102,38),fill='#d5d6cf')
            d.line((119,36,124,36,124,31,128,31),fill='#b39231',width=2);d.line((124,36,124,56,128,56),fill='#b39231',width=2)
        elif kind=='rolling_mill':
            for y in (28,40):d.rounded_rectangle((88,y,113,y+8),radius=3,fill='#485156');d.line((91,y+1,110,y+1),fill='#c5c8c5')
        elif kind=='industrial_press':
            d.rectangle((89,24,113,28),fill='#4e595e');d.rectangle((98,29,104,38),fill='#737f83');d.rectangle((94,38,108,41),fill='#d1b444');d.rectangle((90,46,113,49),fill='#4c575c');d.line((89,25,89,48,114,48,114,25),fill='#6d7a83',width=2)
        elif kind=='quenching_bath':
            d.rectangle((87,34,114,47),fill='#546b7c');d.rectangle((89,35,112,40),fill='#418aa2');d.line((87,32,87,48,114,48,114,32),fill='#dad7c4',width=2);d.line((94,22,92,26,95,29),fill='#818c8e');d.line((104,22,102,26,105,29),fill='#818c8e')
        elif kind in ('universal_mixer','fermenter'):
            d.rectangle((89,24,113,48),fill='#667780');d.rectangle((92,26,110,46),fill='#b6b6aa');d.line((101,23,101,48),fill='#4e585c',width=2)
            for y in (30,39):d.line((94,y,108,y),fill='#71848b',width=2)
        else:
            d.rectangle((88,29,95,44),fill='#4a575d');d.rectangle((95,32,110,41),fill='#899497');d.line((97,33,109,33),fill='#d2d5ce');d.line((89,47,114,47),fill='#4a575d',width=2)
    for y in range(3):
        for x in range(9):slot(d,8+x*18,84+y*18)
    for x in range(9):slot(d,8+x*18,142)
    DEST.mkdir(parents=True,exist_ok=True);im.save(DEST/(kind+'.png'))
if __name__=='__main__':
    for kind,(inputs,outputs) in PROFILES.items():generate(kind,inputs,outputs)
    print('Exported 10 native 176x166 machine screens')
