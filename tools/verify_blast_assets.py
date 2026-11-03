"""Check native-model/export parity after hand edits; never regenerate the model."""
import base64, json, math
from pathlib import Path
from export_machines import rotate

ROOT=Path(__file__).resolve().parent.parent
A=ROOT/'src/main/resources/assets/suitcaseglider'
source=ROOT/'art_source/blockbench/blocks/Blast-Furnace.bbmodel'
m=json.loads(source.read_text(encoding='utf-8'))
r=json.loads((A/'rigs/blast_furnace.json').read_text(encoding='utf-8'))
points=[]
for face in r['faces']:
    texture=r['textures'][face['texture']]
    for uv in face['uv']:
        assert all(math.isfinite(v) for v in uv)
        assert 0<=uv[0]<=texture['width'] and 0<=uv[1]<=texture['height'],uv
    for vertex in face['v']:
        p=[v*16 for v in vertex];b=face['bone']
        while b>=0:
            bone=r['bones'][b];parent=bone['parent'];o=r['bones'][parent]['origin'] if parent>=0 else [0,0,0]
            p=rotate(p,bone['rotation']);p=[p[i]+bone['origin'][i]-o[i] for i in range(3)];b=parent
        assert all(math.isfinite(v) for v in p)
        points.append(p)
lo=[min(p[i] for p in points) for i in range(3)]
hi=[max(p[i] for p in points) for i in range(3)]
assert all(v>=-.001 for v in lo) and all(hi[i]<=[48,72,48][i]+.001 for i in range(3)),(lo,hi)
for original,exported in zip(m['textures'],r['textures'],strict=True):
    assert base64.b64decode(original['source'].split(',')[1])==(A/exported['resource'].split(':',1)[1]).read_bytes()
assert {e['name'] for e in m['elements'] if e['type']=='locator'}==set(r['anchors'])
assert {a['name'].split('.')[-1] for a in m['animations']}==set(r['animations'])
report=dict(boundsUnits=dict(min=lo,max=hi),faces=len(r['faces']),bones=len(r['bones']),sourceElements=len(m['elements']),anchors=list(r['anchors']),clips=list(r['animations']),uvsValid=True,textureBytesMatch=True)
print(json.dumps(report,indent=2))
