"""Copy vanilla bed pixels unchanged; split model faces at dye-mask boundaries.

The dye mask is obtained by comparing vanilla white/red bed images, so pillow,
wood and legs remain untinted without inventing or repainting any texture.
"""
from pathlib import Path
from io import BytesIO
import copy
import json
import zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/sum1rem0d'
JAR = ROOT / '.gradle/isolated-home/caches/fabric-loom/26.3/minecraft-client.jar'


def rectangles(mask, x0, y0, x1, y1):
    todo = {(x, y) for y in range(y0, y1) for x in range(x0, x1)}
    while todo:
        x, y = min(todo, key=lambda p: (p[1], p[0]))
        dye = mask[y][x]
        right = x + 1
        while right < x1 and (right, y) in todo and mask[y][right] == dye:
            right += 1
        bottom = y + 1
        while bottom < y1 and all((u, bottom) in todo and mask[bottom][u] == dye for u in range(x, right)):
            bottom += 1
        todo.difference_update((u, v) for v in range(y, bottom) for u in range(x, right))
        yield x, y, right, bottom, dye


def fragment(element, direction, face, region):
    u, v, right, bottom, dye = region
    u0, v0, u1, v1 = face['uv']
    a, b = (u-u0)/(u1-u0), (right-u0)/(u1-u0)
    c, d = (v-v0)/(v1-v0), (bottom-v0)/(v1-v0)
    lo, hi = element['from'].copy(), element['to'].copy()
    def restrict(axis, start, end, reverse=False):
        low, high = lo[axis], hi[axis]
        lo[axis] = high-end*(high-low) if reverse else low+start*(high-low)
        hi[axis] = high-start*(high-low) if reverse else low+end*(high-low)
    if direction in ('up', 'down'):
        restrict(0, a, b)
        restrict(2, c, d, direction == 'down')
    else:
        restrict(1, c, d, True)
        restrict(0 if direction in ('north', 'south') else 2, a, b, direction in ('north', 'east'))
    edited = copy.deepcopy(face)
    edited['uv'] = [u, v, right, bottom]
    # Explicit independent white layer: never let a shared item/renderer tint
    # reach pillow, wood or legs, even when cloth uses an arbitrary RGB color.
    edited['tintindex'] = 0 if dye else 1
    return {'from': lo, 'to': hi, 'faces': {direction: edited}}


def write(relative, data):
    (ASSETS / relative).write_text(json.dumps(data, indent=2)+'\n', encoding='utf-8')


with zipfile.ZipFile(JAR) as jar:
    def native(relative):
        return jar.read('assets/minecraft/'+relative)
    def model(name):
        return json.loads(native('models/block/'+name+'.json'))
    display = model('template_bed')['display']
    halves = []
    for half in ('head', 'foot'):
        template = model('template_bed_'+half)
        textures = template['textures'].copy()
        textures.update(model('white_bed_'+half)['textures'])
        masks = {}
        for key, value in list(textures.items()):
            if not value.startswith('minecraft:block/'):
                continue
            name = value.split('/')[-1]
            path = 'textures/block/'+name+'.png'
            data = native(path)
            # Preserve the original PNG byte-for-byte, including wood and pillow pixels.
            (ASSETS / path).write_bytes(data)
            textures[key] = 'sum1rem0d:block/'+name
            white = Image.open(BytesIO(data)).convert('RGBA')
            if name.startswith('white_bed_'):
                red = Image.open(BytesIO(native(path.replace('white_bed_', 'red_bed_')))).convert('RGBA')
                # Vanilla pillow shadows vary slightly between bed colors; they
                # must not be mistaken for dyed cloth and tinted themselves.
                masks[key] = [[max(abs(a-b) for a,b in zip(white.getpixel((x,y))[:3],red.getpixel((x,y))[:3])) > 40 for x in range(16)] for y in range(16)]
                if name == 'white_bed_head_up':
                    for y in range(8):
                        masks[key][y] = [False]*16
                    assert not any(masks[key][y][x] for y in range(8) for x in range(16)), 'Pillow must remain unchanged'
                if name.endswith(('_east','_west','_south')):
                    assert not any(masks[key][y][x] for y in range(13,16) for x in range(16)), 'Wooden legs must remain unchanged'
            else:
                masks[key] = [[False]*16 for _ in range(16)]
        pieces = []
        for element in template['elements']:
            for direction, face in element['faces'].items():
                key = face['texture'][1:]
                for region in rectangles(masks[key], *map(int,face['uv'])):
                    pieces.append(fragment(element,direction,face,region))
        result = {'textures': textures, 'elements': pieces, 'display': display}
        write('models/block/mixed_bed_'+half+'.json', result)
        halves.append(result)
    # Same two vanilla halves as the vanilla composite item, without relying on
    # version-specific item transformation codecs on older supported targets.
    merged = []
    textures = {}
    for index, half in enumerate(halves):
        prefix = 'head_' if index == 0 else 'foot_'
        textures.update({prefix+k: v for k,v in half['textures'].items()})
        for element in copy.deepcopy(half['elements']):
            element['from'][2] += index*16
            element['to'][2] += index*16
            for face in element['faces'].values():
                face['texture'] = '#'+prefix+face['texture'][1:]
                face.pop('cullface', None)
            merged.append(element)
    textures['particle'] = 'minecraft:block/white_wool'
    write('models/item/mixed_bed.json', {'textures':textures,'elements':merged,'display':display})
    for kind, source in [('mixed_wool','white_wool'), ('mixed_concrete','white_concrete')]:
        path = ASSETS / ('models/block/'+kind+'.json')
        data = json.loads(path.read_text())
        data['textures'] = {key: 'minecraft:block/'+source for key in data['textures']}
        write('models/block/'+kind+'.json', data)
    print('Preserved native pixels and split bed faces:', *(len(h['elements']) for h in halves))
