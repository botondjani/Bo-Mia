"""
Generates app/src/main/assets/tend_character.riv: Bo and Mia on one rig.

Contract (see docs/rive-rig.md and ui/Character.kt):
  artboard "Character" 500x500, state machine "Mood" with inputs
    mood (number: 0 happy, 1 tired, 2 scrolling, 3 outside, 4 reading, 5 sleeping, 6 sleepy), mia (bool), tap (trigger),
    headband, glasses, hat, flower, leaf, bowtie, nightcap (bool: accessories earned by habits),
    scientist (bool: lab coat, glasses and a waving pointer for onboarding and the tour),
    prop (number: 0 none, 1 dumbbell, 2 hearts, 3 palette and brush, 4 broom, 5 chef's hat; during timers).
Layers: mood pose, skin (Bo/Mia), breathing, blinking, glancing, tap hop.

Geometry follows the drawn fallback (ui/CharacterPainter.kt): a 100-unit box scaled x5,
with the origin at the character's feet (50, 92) so breathing squashes from the ground.
In the .riv format the first drawable in the file is drawn on top, so shapes are written
front to back.

Usage: python3 tools/rive/make_character.py [out.riv]
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from riv import *

MINT, MINT_LIGHT = 0xFF7ECAA3, 0xFFA9DDC1
GREY, GREY_LIGHT = 0xFFAEB8B0, 0xFFC8D0CA
INK, WHITE, BLUE = 0xFF1D2A23, 0xFFFFFFFF, 0xFF3B5BDB
COAT, COAT_LINE = 0xFFFDFDFB, 0xFFD5DCD6
WEIGHT, PALETTE, STRAW = 0xFF4A5852, 0xFFEAD2A8, 0xFFD9B45A
CHEEK, HAIR, CORAL, SUNNY, SKY, MOSS, SHADOW = 0x96C9937A, 0xFF5A3A2A, 0xFFE76649, 0xFFF6D673, 0xFF8EC4E8, 0xFF316849, 0x221D2A23
S = 5.0  # painter unit -> artboard px


def T(px, py):
    """Painter coordinates -> coordinates relative to the feet."""
    return ((px - 50) * S, (py - 92) * S)


class Rig:
    def __init__(self, w):
        self.w, self.n = w, 0
        self.origin = (50.0, 92.0)  # painter point the current parent sits on (the feet, unless a rotated group)

    def t(self, px, py):
        """Painter coordinates -> coordinates relative to the current parent's origin."""
        return ((px - self.origin[0]) * S, (py - self.origin[1]) * S)

    def add(self, type_key, **props):
        self.w.obj(type_key, **props)
        self.n += 1
        return self.n - 1

    def node(self, name, parent, x=0.0, y=0.0):
        return self.add(NODE, name=name, parentId=parent, x=x, y=y)

    def paint(self, shape, fill=None, stroke=None, width=2.6, scale_stroke=True):
        colors = []
        if fill is not None:
            f = self.add(FILL, parentId=shape)
            colors.append(self.add(SOLID_COLOR, parentId=f, color=fill))
        if stroke is not None:
            st = self.add(STROKE, parentId=shape, thickness=width * S, cap=1, join=1,
                          transformAffectsStroke=None if scale_stroke else False)
            colors.append(self.add(SOLID_COLOR, parentId=st, color=stroke))
        return colors

    def ellipse(self, name, parent, cx, cy, w, h, fill):
        x, y = self.t(cx, cy)
        shape = self.add(SHAPE, name=name, parentId=parent, x=x, y=y)
        self.add(ELLIPSE, parentId=shape, pWidth=w * S, pHeight=h * S)
        return shape, self.paint(shape, fill)[0]

    def rect(self, name, parent, cx, cy, w, h, r, fill, origin_top=False):
        x, y = self.t(cx, cy)
        shape = self.add(SHAPE, name=name, parentId=parent, x=x, y=y)
        self.add(RECTANGLE, parentId=shape, pWidth=w * S, pHeight=h * S, cornerRadius=r * S,
                 originY=0.0 if origin_top else None)
        return shape, self.paint(shape, fill)[0]

    def path(self, name, parent, verts, closed, fill=None, stroke=None, width=2.6):
        """verts: (x, y, in_point or None, out_point or None) in painter coordinates."""
        shape = self.add(SHAPE, name=name, parentId=parent)
        path = self.add(POINTS_PATH, parentId=shape, isClosed=closed)
        for (vx, vy, vin, vout) in verts:
            px, py = self.t(vx, vy)
            if vin is None and vout is None:
                self.add(STRAIGHT_VERTEX, parentId=path, vx=px, vy=py)
                continue
            props = dict(parentId=path, vx=px, vy=py)
            for key, pt in (('in', vin), ('out', vout)):
                if pt is None:
                    pt = (vx, vy)
                dx, dy = (pt[0] - vx) * S, (pt[1] - vy) * S
                props[key + 'Rotation'] = math.atan2(dy, dx)
                props[key + 'Distance'] = math.hypot(dx, dy)
            self.add(CUBIC_DETACHED_VERTEX, **props)
        return shape, self.paint(shape, fill, stroke, width)

    def ring(self, name, parent, cx, cy, d, color, width, fill=None):
        """A circle outline (and its inside, if [fill])."""
        x, y = self.t(cx, cy)
        shape = self.add(SHAPE, name=name, parentId=parent, x=x, y=y)
        self.add(ELLIPSE, parentId=shape, pWidth=d * S, pHeight=d * S)
        self.paint(shape, fill=fill, stroke=color, width=width)
        return shape

    def draw(self, name, parent, start, segs, closed, fill=None, stroke=None, width=2.6):
        """Like the painter's Path: start (x, y), then ('L', x, y), ('Q', qx, qy, x, y) or ('C', c1x, c1y, c2x, c2y, x, y)."""
        pts = [[start[0], start[1], None, None]]
        for sg in segs:
            prev = pts[-1]
            if sg[0] == 'L':
                pts.append([sg[1], sg[2], None, None])
                continue
            if sg[0] == 'Q':
                qx, qy, x, y = sg[1:]
                c1 = (prev[0] + 2 / 3 * (qx - prev[0]), prev[1] + 2 / 3 * (qy - prev[1]))
                c2 = (x + 2 / 3 * (qx - x), y + 2 / 3 * (qy - y))
            else:
                c1, c2, (x, y) = (sg[1], sg[2]), (sg[3], sg[4]), (sg[5], sg[6])
            prev[3] = c1
            pts.append([x, y, c2, None])
        # Back where it started: one vertex, its way in from the last segment.
        if closed and len(pts) > 2 and abs(pts[-1][0] - pts[0][0]) < 1e-6 and abs(pts[-1][1] - pts[0][1]) < 1e-6:
            pts[0][2] = pts[-1][2]
            pts.pop()
        return self.path(name, parent, [tuple(p) for p in pts], closed, fill=fill, stroke=stroke, width=width)

    def star(self, name, parent, cx, cy, outer, inner, fill):
        pts = []
        for k in range(10):
            r = outer if k % 2 == 0 else inner
            a = math.radians(k * 36 - 90)
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a), None, None))
        return self.path(name, parent, pts, True, fill=fill)


# Outfits: bought, worn one at a time (Accessory.isOutfit). Input names are the enum names in lower case.
OUTFITS = ('flower_crown', 'sunglasses', 'scarf', 'bobble_hat', 'beret', 'party_hat', 'bandana', 'headphones', 'crown', 'hair_bow',
           'chef_hat', 'captain_hat', 'cowboy_hat', 'wizard_hat', 'mustache', 'monocle', 'star_glasses')


def outfits(r, body):
    """Same shapes as CharacterPainter.drawOutfits / drawAnytime, each group written front to back."""
    g = {name: r.node(f'acc_{name}', body) for name in OUTFITS}
    GOLD, PINK, NAVY, TAN, TAN_DARK = 0xFFF2C14E, 0xFFF2A0B8, 0xFF2E4A7A, 0xFFA0673A, 0xFF6F4424
    RUST, STRIPE, RED, RED_DARK, LEAF = 0xFFC9652E, 0xFFF2D27A, 0xFFD2483F, 0xFFB23A33, 0xFF4FA36F

    p = g['scarf']
    for i, x in enumerate((61, 63.3, 65.6, 67.9)):
        r.path(f'fringe_{i}', p, [(x, 95.5, None, None), (x + 0.4, 98.5, None, None)], False, stroke=STRIPE, width=1.2)
    r.rect('scarf_tail_stripe', p, 64.15, 88.25, 8.3, 2.5, 0, STRIPE)
    for i, x in enumerate((33, 43, 53)):
        r.rect(f'scarf_stripe_{i}', p, x + 1.5, 79, 3, 9, 0, STRIPE)
    r.path('scarf_tail', p, [(58, 79, None, None), (67, 79, None, None), (69, 95, None, None), (60, 96, None, None)], True, fill=RUST)
    r.rect('scarf_band', p, 50, 79, 50, 10, 5, RUST)

    p = g['sunglasses']
    r.ellipse('sun_shine_l', p, 35, 43, 6, 4, 0x66FFFFFF)
    r.ellipse('sun_shine_r', p, 58, 43, 6, 4, 0x66FFFFFF)
    r.path('sun_bridge', p, [(47, 43, None, None), (53, 43, None, None)], False, stroke=INK, width=2)
    r.rect('sun_lens_l', p, 38.5, 46, 19, 14, 6, INK)
    r.rect('sun_lens_r', p, 61.5, 46, 19, 14, 6, INK)

    p = g['flower_crown']
    colors = (0xFFF2A0B8, WHITE, SUNNY, 0xFFF2A0B8, WHITE)
    for k in reversed(range(5)):
        tt = k / 4
        x, y = 28 + 44 * tt, 26 - 10 * (1 - (2 * tt - 1) ** 2)
        r.ellipse(f'fc_mid_{k}', p, x, y, 3, 3, GOLD)
        for q in range(5):
            a = math.radians(q * 72 - 90)
            r.ellipse(f'fc_petal_{k}_{q}', p, x + 2.4 * math.cos(a), y + 2.4 * math.sin(a), 4.2, 4.2, colors[k])
        r.ellipse(f'fc_leaf_{k}', p, x - 3.5, y + 0.25, 5, 3.5, LEAF)
    r.draw('fc_stem', p, (26, 27), [('Q', 50, 13, 74, 27)], False, stroke=MOSS, width=1.8)

    p = g['bobble_hat']
    r.ellipse('bobble', p, 50, 9, 11, 11, 0xFFF7F3E8)
    for i, x in enumerate(range(27, 74, 5)):
        r.path(f'knit_{i}', p, [(x, 24.5, None, None), (x, 31.5, None, None)], False, stroke=RED, width=1.2)
    r.rect('bobble_band', p, 50, 28, 54, 10, 4, RED_DARK)
    r.draw('bobble_dome', p, (26, 28), [('C', 26, 6, 74, 6, 74, 28)], True, fill=RED)

    # The beret and the party hat lean: each in a group turned around its own point.
    beret = r.add(NODE, name='beret_tilt', parentId=g['beret'], x=(50 - 50) * S, y=(20 - 92) * S, rotation=math.radians(-12))
    r.origin = (50.0, 20.0)
    r.ellipse('beret_nub', beret, 50, 11.5, 4.6, 4.6, 0xFFB5523B)
    r.rect('beret_band', beret, 50, 25.5, 36, 5, 3, 0xFF8E3A27)
    r.ellipse('beret_top', beret, 50, 20, 48, 16, 0xFFB5523B)
    party = r.add(NODE, name='party_tilt', parentId=g['party_hat'], x=(50 - 50) * S, y=(24 - 92) * S, rotation=math.radians(10))
    r.origin = (50.0, 24.0)
    r.ellipse('party_pom', party, 50, 1.5, 7.2, 7.2, SUNNY)
    r.path('party_stripe_1', party, [(41.5, 18, None, None), (58.5, 18, None, None)], False, stroke=CORAL, width=2.2)
    r.path('party_stripe_2', party, [(45, 11, None, None), (55, 11, None, None)], False, stroke=CORAL, width=2.2)
    r.path('party_cone', party, [(38, 25, None, None), (50, 1, None, None), (62, 25, None, None)], True, fill=SKY)
    r.origin = (50.0, 92.0)

    p = g['bandana']
    for i, (x, y) in enumerate(((42, 79), (50, 81), (58, 79), (46, 85), (54, 85), (50, 89))):
        r.ellipse(f'bandana_dot_{i}', p, x, y, 2.2, 2.2, WHITE)
    r.path('bandana_point', p, [(33, 75, None, None), (67, 75, None, None), (50, 92, None, None)], True, fill=CORAL)
    r.rect('bandana_knot', p, 50, 75, 44, 6, 3, CORAL)

    p = g['headphones']
    for side, cx in (('l', 23), ('r', 77)):
        shape, _ = r.rect(f'cup_{side}', p, cx, 46, 12, 18, 5, SKY)
        r.paint(shape, stroke=INK)
    r.draw('headband_arc', p, (23, 44), [('C', 22, 6, 78, 6, 77, 44)], False, stroke=INK, width=4)

    p = g['crown']
    r.ellipse('crown_ruby', p, 50, 19, 4.4, 4.4, CORAL)
    r.ellipse('crown_gem_l', p, 41, 20, 3.2, 3.2, SKY)
    r.ellipse('crown_gem_r', p, 59, 20, 3.2, 3.2, SKY)
    r.path('crown', p, [(35, 24, None, None), (34, 9, None, None), (42, 16, None, None), (50, 5, None, None), (58, 16, None, None), (66, 9, None, None), (65, 24, None, None)], True, fill=GOLD)

    p = g['hair_bow']
    r.ellipse('hairbow_knot', p, 68, 18, 5.2, 5.2, 0xFFD96C9C)
    r.path('hairbow_l', p, [(68, 18, None, None), (58, 10, None, None), (58, 26, None, None)], True, fill=PINK)
    r.path('hairbow_r', p, [(68, 18, None, None), (78, 10, None, None), (78, 26, None, None)], True, fill=PINK)

    p = g['chef_hat']
    r.path('chefhat_line', p, [(37, 21, None, None), (63, 21, None, None)], False, stroke=COAT_LINE, width=1.2)
    r.rect('chefhat_band', p, 50, 21, 26, 10, 2, COAT)
    r.ellipse('chefhat_puff_l', p, 40, 15, 16, 16, COAT)
    r.ellipse('chefhat_puff_r', p, 60, 15, 16, 16, COAT)
    r.ellipse('chefhat_puff_m', p, 50, 11, 18, 18, COAT)

    p = g['captain_hat']
    r.ellipse('captain_badge', p, 50, 15, 5.6, 5.6, GOLD)
    r.draw('captain_peak', p, (33, 24), [('Q', 50, 31, 67, 24), ('Q', 50, 27, 33, 24)], True, fill=INK)
    r.rect('captain_band', p, 50, 21.5, 38, 7, 0, NAVY)
    r.rect('captain_top', p, 50, 15, 46, 14, 8, COAT)

    p = g['cowboy_hat']
    r.rect('cowboy_band', p, 50, 23, 30, 4, 0, TAN_DARK)
    r.draw('cowboy_crown', p, (34, 27), [('L', 36, 8), ('Q', 43, 4, 50, 9), ('Q', 57, 4, 64, 8), ('L', 66, 27)], True, fill=TAN)
    r.draw('cowboy_brim', p, (14, 20), [('Q', 22, 29, 50, 29), ('Q', 78, 29, 86, 20), ('Q', 80, 33, 50, 33), ('Q', 20, 33, 14, 20)], True, fill=TAN)

    p = g['wizard_hat']
    r.star('wizard_star_1', p, 46, 16, 3.2, 1.4, SUNNY)
    r.star('wizard_star_2', p, 56, 9, 2.4, 1.0, SUNNY)
    r.star('wizard_star_3', p, 56, 20, 2.0, 0.9, SUNNY)
    r.draw('wizard_cone', p, (32, 25), [('Q', 46, 6, 61, -4), ('Q', 57, 8, 68, 25)], True, fill=0xFF6A4FB0)
    r.ellipse('wizard_brim', p, 50, 25, 52, 8, 0xFF513B8C)

    p = g['mustache']
    r.draw('mustache_l', p, (50, 54), [('C', 46, 50, 37, 50, 34, 56), ('C', 38, 55, 40, 59, 44, 59), ('C', 47, 59, 49, 57, 50, 55.5)], True, fill=HAIR)
    r.draw('mustache_r', p, (50, 54), [('C', 54, 50, 63, 50, 66, 56), ('C', 62, 55, 60, 59, 56, 59), ('C', 53, 59, 51, 57, 50, 55.5)], True, fill=HAIR)

    p = g['monocle']
    r.draw('monocle_chain', p, (70, 52), [('Q', 77, 64, 72, 75)], False, stroke=GOLD, width=1)
    r.ring('monocle', p, 61, 46, 21, GOLD, 2, fill=0x33FFFFFF)

    p = g['star_glasses']
    r.ellipse('star_lens_l', p, 39, 46, 10, 10, 0xCC1D2A23)
    r.ellipse('star_lens_r', p, 61, 46, 10, 10, 0xCC1D2A23)
    r.path('star_bridge', p, [(46, 44, None, None), (54, 44, None, None)], False, stroke=INK, width=2)
    r.star('star_frame_l', p, 39, 46, 12, 6.5, PINK)
    r.star('star_frame_r', p, 61, 46, 12, 6.5, PINK)
    return g


def build():
    w = Writer()
    header(w)
    w.obj(BACKBOARD)
    r = Rig(w)
    ab = r.add(ARTBOARD, name='Character', width=500.0, height=500.0, defaultStateMachineId=0)
    feet = (250.0, 460.0)

    # --- Groups (not drawn) ---------------------------------------------------------------
    ground = r.node('ground', ab, *feet)      # shadow, grass, sun: they stay put when Bo hops
    hop = r.node('hop', ab, *feet)            # tap hop
    body = r.node('body', hop)               # breathing
    pupils = r.node('pupils', body)          # glance / scrolling look-down
    bo = r.node('bo', body)
    mia_front = r.node('mia_front', body)
    mia_back = r.node('mia_back', body)
    acc = {name: r.node(f'acc_{name}', body) for name in ('hat', 'flower', 'glasses', 'headband', 'leaf', 'bowtie', 'nightcap')}
    props = [r.node(f'prop_{k}', body) for k in range(1, 6)]  # dumbbell, hearts, palette, broom, chef hat
    sci = r.node('scientist', body)
    pointer = r.node('pointer', sci, *T(80, 78))  # turns around the hand

    def at_hand(x, y):
        """Painter coordinates -> painter coordinates that land right under the pointer node."""
        return x - 30, y + 14

    # --- Drawables, front to back ---------------------------------------------------------
    # Props held or worn during timers (same shapes as CharacterPainter.drawProp), in front of everything.
    dumbbell, hearts, palette, broom, chef = props
    r.ellipse('db_hand', dumbbell, 81, 76, 8.4, 8.4, MINT)
    r.rect('db_weight_l', dumbbell, 69, 74, 6, 16, 2, WEIGHT)
    r.rect('db_weight_r', dumbbell, 93, 74, 6, 16, 2, WEIGHT)
    r.path('db_bar', dumbbell, [(70, 74, None, None), (92, 74, None, None)], False, stroke=INK, width=2.4)
    for name, cx, cy, sz in (('heart_big', 80, 20, 10), ('heart_small', 90, 33, 6)):
        r.ellipse(f'{name}_l', hearts, cx - 0.25 * sz, cy - 0.1 * sz, 0.6 * sz, 0.6 * sz, CORAL)
        r.ellipse(f'{name}_r', hearts, cx + 0.25 * sz, cy - 0.1 * sz, 0.6 * sz, 0.6 * sz, CORAL)
        r.path(f'{name}_tip', hearts, [(cx - 0.53 * sz, cy + 0.02 * sz, None, None), (cx + 0.53 * sz, cy + 0.02 * sz, None, None), (cx, cy + 0.62 * sz, None, None)], True, fill=CORAL)
    r.ellipse('brush_hand', palette, 80, 78, 8.4, 8.4, MINT)
    r.ellipse('brush_tip', palette, 92, 58, 4.4, 4.4, SKY)
    r.path('brush', palette, [(80, 78, None, None), (92, 58, None, None)], False, stroke=HAIR, width=2.2)
    for i, (x, y, col) in enumerate(((14, 70, CORAL), (20, 68, SKY), (26, 70, SUNNY), (14, 76, MINT))):
        r.ellipse(f'paint_{i}', palette, x, y, 4, 4, col)
    r.ellipse('palette', palette, 20, 73, 24, 18, PALETTE)
    r.ellipse('broom_hand', broom, 82, 64, 8.4, 8.4, MINT)
    r.path('broom_band', broom, [(70, 84, None, None), (82, 84, None, None)], False, stroke=CORAL, width=1.6)
    r.path('broom_straw', broom, [(70, 82, None, None), (82, 82, None, None), (86, 96, None, None), (66, 96, None, None)], True, fill=STRAW)
    r.path('broom_stick', broom, [(88, 40, None, None), (76, 84, None, None)], False, stroke=HAIR, width=2.4)
    r.path('chef_line', chef, [(37, 21, None, None), (63, 21, None, None)], False, stroke=COAT_LINE, width=1.2)
    r.rect('chef_band', chef, 50, 21, 26, 10, 2, COAT)
    r.ellipse('chef_puff_l', chef, 40, 15, 16, 16, COAT)
    r.ellipse('chef_puff_r', chef, 60, 15, 16, 16, COAT)
    r.ellipse('chef_puff_m', chef, 50, 11, 18, 18, COAT)

    # Scientist: the hand over the pointer, glasses over the eyes (the coat comes further back).
    r.ellipse('sci_hand', sci, 80, 78, 8.4, 8.4, MINT)
    r.ellipse('pointer_tip', pointer, *at_hand(92, 49), 4.8, 4.8, CORAL)
    r.path('pointer_stick', pointer, [(*at_hand(80, 78), None, None), (*at_hand(92, 49), None, None)], False, stroke=HAIR, width=2.2)
    for ex in (39, 61):
        x, y = T(ex, 46)
        ring = r.add(SHAPE, name=f'sci_lens_{ex}', parentId=sci, x=x, y=y)
        r.add(ELLIPSE, parentId=ring, pWidth=19 * S, pHeight=19 * S)
        r.paint(ring, stroke=INK, width=1.8)
    r.path('sci_bridge', sci, [(48.5, 45, None, None), (51.5, 45, None, None)], False, stroke=INK, width=1.8)

    # Outfits, then the habits' accessories: on top of everything on the head.
    acc.update(outfits(r, body))
    r.ellipse('nightcap_pom', acc['nightcap'], 71, 13, 7.2, 7.2, COAT)
    r.rect('nightcap_band', acc['nightcap'], 50, 22, 36, 6, 3, COAT)
    r.path('nightcap', acc['nightcap'], [(34, 22, None, (41, 11)), (70, 12, (59, 6), None), (64, 17, None, (60, 14.5)), (66, 22, (60, 17), None)], True, fill=0xFF5B7FC0)
    r.path('leaf', acc['leaf'], [(54, 19, (60, 20), (55, 11)), (67, 10, (61, 9), (66, 17))], True, fill=0xFF4FA36F)
    r.path('leaf_stem', acc['leaf'], [(52, 21, None, None), (55.5, 18, None, None)], False, stroke=MOSS, width=1.6)
    r.ellipse('bow_knot', acc['bowtie'], 50, 73, 4.8, 4.8, 0xFF5B8FC0)
    r.path('bowtie_l', acc['bowtie'], [(50, 73, None, None), (42, 68, None, None), (42, 78, None, None)], True, fill=SKY)
    r.path('bowtie_r', acc['bowtie'], [(50, 73, None, None), (58, 68, None, None), (58, 78, None, None)], True, fill=SKY)
    r.rect('hat_band', acc['hat'], 50, 18, 28, 4, 0, CORAL)
    r.ellipse('hat_brim', acc['hat'], 50, 22.5, 60, 9, SUNNY)
    r.rect('hat_crown', acc['hat'], 50, 15, 28, 14, 6, SUNNY)
    r.ellipse('flower_middle', acc['flower'], 71, 31, 4.4, 4.4, SUNNY)
    for k in range(5):
        ang = math.radians(k * 72 - 90)
        r.ellipse(f'petal_{k}', acc['flower'], 71 + 3.2 * math.cos(ang), 31 + 3.2 * math.sin(ang), 5.2, 5.2, WHITE)
    for ex in (39, 61):
        x, y = T(ex, 46)
        ring = r.add(SHAPE, name=f'lens_{ex}', parentId=acc['glasses'], x=x, y=y)
        r.add(ELLIPSE, parentId=ring, pWidth=19 * S, pHeight=19 * S)
        r.paint(ring, stroke=INK, width=1.8)
    r.path('bridge', acc['glasses'], [(48.5, 45, None, None), (51.5, 45, None, None)], False, stroke=INK, width=1.8)
    r.path('headband', acc['headband'], [(25, 37, None, (41.67, 29)), (75, 37, (58.33, 29), None)], False, stroke=CORAL, width=4.5)

    sun, _ = r.ellipse('sun', ground, 84, 14, 12, 12, SUNNY)
    zz, _ = r.path('zz', hop, [(77, 15, None, None), (82, 15, None, None), (77, 21, None, None), (82, 21, None, None)], False, stroke=INK, width=1.8)
    zz2, _ = r.path('zz_small', zz, [(85, 8, None, None), (88.5, 8, None, None), (85, 12, None, None), (88.5, 12, None, None)], False, stroke=INK, width=1.5)
    book = r.node('book', body)
    r.path('page_l', book, [(31, 70, None, None), (49, 73, None, None), (49, 90, None, None), (31, 87, None, None)], True, fill=0xFFFFF8EC)
    r.path('page_r', book, [(69, 70, None, None), (51, 73, None, None), (51, 90, None, None), (69, 87, None, None)], True, fill=0xFFFFF8EC)
    r.path('cover', book, [(28, 70, None, None), (50, 74, None, None), (72, 70, None, None), (72, 90, None, None), (50, 94, None, None), (28, 90, None, None)], True, fill=CORAL)
    phone = r.node('phone', body)
    r.rect('phone_screen', phone, 73, 69.5, 10, 17, 1.5, SKY)
    r.rect('phone_body', phone, 73, 70, 14, 24, 3, INK)
    # Mia's bow (in front of the head)
    r.path('bow_l', mia_front, [(50, 22, None, None), (42, 17, None, None), (42, 27, None, None)], True, fill=SUNNY)
    r.path('bow_r', mia_front, [(50, 22, None, None), (58, 17, None, None), (58, 27, None, None)], True, fill=SUNNY)

    lids_blink, lids_mood, lid_lines, lid_colors, pupil_colors = [], [], [], [], []
    for ex in (39, 61):
        s, c = r.rect(f'lid_blink_{ex}', body, ex, 37, 18, 17, 0, MINT, origin_top=True)
        lids_blink.append(s); lid_colors.append(c)
        s, c = r.rect(f'lid_mood_{ex}', body, ex, 37, 18, 17, 0, MINT, origin_top=True)
        lids_mood.append(s); lid_colors.append(c)
        # The lid's edge rides on the lid's bottom; the stroke keeps its weight while the lid scales.
        line = r.add(SHAPE, name=f'lid_line_{ex}', parentId=s, y=17 * S)
        lp = r.add(POINTS_PATH, parentId=line, isClosed=False)
        r.add(STRAIGHT_VERTEX, parentId=lp, vx=-7 * S, vy=0.0)
        r.add(STRAIGHT_VERTEX, parentId=lp, vx=7 * S, vy=0.0)
        r.paint(line, stroke=INK, scale_stroke=False)
        lid_lines.append(line)
    for ex in (39, 61):
        r.ellipse(f'highlight_{ex}', pupils, ex + 2.5, 45, 3, 3, WHITE)
        _, c = r.ellipse(f'pupil_{ex}', pupils, ex + 1, 47, 9.2, 9.2, INK)
        pupil_colors.append(c)
    for ex in (39, 61):
        r.ellipse(f'eye_{ex}', body, ex, 46, 16, 16, WHITE)

    # Mouths, one per mood
    mouth_open = r.node('mouth_open', body)
    r.ellipse('tongue', mouth_open, 50, 72, 10, 6, CORAL)
    r.path('mouth', mouth_open, [(41, 61, (42.33, 70.33), (47, 63)), (59, 61, (53, 63), (57.67, 70.33)), (50, 75, (54.67, 75), (45.33, 75))], True, fill=INK)
    mouth_happy, _ = r.path('smile', body, [(57.5, 64, None, (55.5, 68.6)), (42.5, 64, (44.5, 68.6), None)], False, stroke=INK)
    mouth_tired, _ = r.path('frown', body, [(43.4, 68.6, None, (45.3, 65.6)), (56.6, 68.6, (54.7, 65.6), None)], False, stroke=INK)
    mouth_flat, _ = r.path('flat', body, [(45, 68, None, None), (55, 68, None, None)], False, stroke=INK)

    # Lab coat: in front of cheeks, belly and body, behind the face.
    r.path('pen', sci, [(33, 75.5, None, None), (33, 71.5, None, None)], False, stroke=CORAL, width=1.8)
    r.path('pocket', sci, [(26.5, 75.5, None, None), (35.5, 75.5, None, None), (35.5, 82.5, None, None), (26.5, 82.5, None, None)], True, stroke=COAT_LINE, width=1.4)
    r.path('lapel_l', sci, [(44, 66, None, None), (40.5, 76, None, None)], False, stroke=COAT_LINE, width=1.4)
    r.path('lapel_r', sci, [(56, 66, None, None), (59.5, 76, None, None)], False, stroke=COAT_LINE, width=1.4)
    r.path('coat_l', sci, [(44, 66, None, None), (18.3, 64, None, None), (18, 68, None, (20, 86)), (50, 92, (34, 92), None), (40, 92, None, None)], True, fill=COAT)
    r.path('coat_r', sci, [(56, 66, None, None), (81.7, 64, None, None), (82, 68, None, (80, 86)), (50, 92, (66, 92), None), (60, 92, None, None)], True, fill=COAT)

    r.ellipse('cheek_l', body, 30, 60, 10, 10, CHEEK)
    r.ellipse('cheek_r', body, 70, 60, 10, 10, CHEEK)
    _, belly_color = r.ellipse('belly', body, 50, 76, 36, 28, MINT_LIGHT)
    _, (body_color,) = r.path('blob', body, [
        (50, 18, (24, 18), (76, 18)),
        (82, 68, (84, 48), (80, 86)),
        (50, 92, (66, 92), (34, 92)),
        (18, 68, (20, 86), (16, 48)),
    ], True, fill=MINT)
    # Behind the head: Bo's tuft, Mia's ears
    r.ellipse('tuft_l', bo, 46.5, 18, 7, 12, HAIR)
    r.ellipse('tuft_r', bo, 52.5, 18, 7, 12, HAIR)
    r.ellipse('ear_l', mia_back, 30, 25, 14, 14, CORAL)
    r.ellipse('ear_r', mia_back, 70, 25, 14, 14, CORAL)

    grass = r.node('grass', ground)
    for gx, tip in ((22, -2), (26, -2), (74, 2), (78, 2)):
        r.path(f'grass_{gx}', grass, [(gx, 92, None, None), (gx + tip, 86, None, None)], False, stroke=MOSS, width=1.8)
    r.ellipse('shadow', ground, 50, 93.5, 48, 7, SHADOW)

    ease = r.add(CUBIC_EASE)  # smooth in-out for breathing and the hop

    # --- Animations ----------------------------------------------------------------------
    anims = []

    def animation(name, duration, loop, keys):
        """keys: {object_index: {property_key: [(frame, value, interp), ...]}}"""
        anims.append(name)
        w.obj(LINEAR_ANIMATION, animName=name, fps=60, duration=duration, loop=loop)
        for obj, props in keys.items():
            w.obj(KEYED_OBJECT, objectId=obj)
            for prop, frames in props.items():
                w.obj(KEYED_PROPERTY, propertyKey=prop)
                for (frame, value, interp) in frames:
                    cubic = interp == INTERP_CUBIC
                    if prop == K_COLOR:
                        w.obj(KF_COLOR, frame=frame, interpolation=interp, interpolatorId=ease if cubic else None, colorValue=value)
                    else:
                        w.obj(KF_DOUBLE, frame=frame, interpolation=interp, interpolatorId=ease if cubic else None, value=value)
        return len(anims) - 1

    def pose(name, *, grey=False, lid=0.0, look_down=False, blue_eyes=False, mouth, phone_on=False, book_on=False, zz_on=False, outside=False):
        keys = {}
        def k(obj, prop, value):
            keys.setdefault(obj, {})[prop] = [(0, value, INTERP_LINEAR)]
        k(body_color, K_COLOR, GREY if grey else MINT)
        k(belly_color, K_COLOR, GREY_LIGHT if grey else MINT_LIGHT)
        for c in lid_colors:
            k(c, K_COLOR, GREY if grey else MINT)
        for s in lids_mood:
            k(s, K_SCALE_Y, lid)
        for line in lid_lines:
            k(line, K_OPACITY, 1.0 if lid > 0 else 0.0)
        k(pupils, K_Y, 2 * S if look_down else 0.0)
        for c in pupil_colors:
            k(c, K_COLOR, BLUE if blue_eyes else INK)
        for m in (mouth_happy, mouth_tired, mouth_flat, mouth_open):
            k(m, K_OPACITY, 1.0 if m == mouth else 0.0)
        k(phone, K_OPACITY, 1.0 if phone_on else 0.0)
        k(book, K_OPACITY, 1.0 if book_on else 0.0)
        k(zz, K_OPACITY, 1.0 if zz_on else 0.0)
        k(sun, K_OPACITY, 1.0 if outside else 0.0)
        k(grass, K_OPACITY, 1.0 if outside else 0.0)
        return animation(name, 1, LOOP_ONESHOT, keys)

    a_happy = pose('happy', mouth=mouth_happy)
    a_tired = pose('tired', grey=True, lid=9 / 17, mouth=mouth_tired, zz_on=True)
    a_scroll = pose('scrolling', lid=7 / 17, look_down=True, blue_eyes=True, mouth=mouth_flat, phone_on=True)
    a_outside = pose('outside', mouth=mouth_open, outside=True)
    a_reading = pose('reading', look_down=True, mouth=mouth_happy, book_on=True)
    a_sleeping = pose('sleeping', lid=1.0, mouth=mouth_happy, zz_on=True)
    a_sleepy = pose('sleepy', lid=9 / 17, mouth=mouth_happy, zz_on=True)

    def skin(name, mia):
        return animation(name, 1, LOOP_ONESHOT, {
            bo: {K_OPACITY: [(0, 0.0 if mia else 1.0, INTERP_HOLD)]},
            mia_front: {K_OPACITY: [(0, 1.0 if mia else 0.0, INTERP_HOLD)]},
            mia_back: {K_OPACITY: [(0, 1.0 if mia else 0.0, INTERP_HOLD)]},
        })

    a_bo, a_mia = skin('bo', False), skin('mia', True)
    a_breathe = animation('breathe', 72, LOOP_PINGPONG, {body: {
        K_SCALE_X: [(0, 1.0, INTERP_CUBIC), (72, 1.025, INTERP_CUBIC)],
        K_SCALE_Y: [(0, 1.0, INTERP_CUBIC), (72, 0.975, INTERP_CUBIC)],
    }})
    a_blink = animation('blink', 270, LOOP_LOOP, {s: {K_SCALE_Y: [
        (0, 0.0, INTERP_HOLD), (246, 0.0, INTERP_LINEAR), (252, 1.0, INTERP_LINEAR), (260, 0.0, INTERP_HOLD)]} for s in lids_blink})
    a_glance = animation('glance', 540, LOOP_LOOP, {pupils: {K_X: [
        (0, 0.0, INTERP_HOLD), (180, 0.0, INTERP_CUBIC), (198, -2 * S, INTERP_HOLD), (288, -2 * S, INTERP_CUBIC),
        (306, 0.0, INTERP_HOLD), (396, 0.0, INTERP_CUBIC), (414, 2 * S, INTERP_HOLD), (486, 2 * S, INTERP_CUBIC), (504, 0.0, INTERP_HOLD)]}})
    acc_anims = {
        name: (animation(f'{name}_off', 1, LOOP_ONESHOT, {node: {K_OPACITY: [(0, 0.0, INTERP_HOLD)]}}),
               animation(f'{name}_on', 1, LOOP_ONESHOT, {node: {K_OPACITY: [(0, 1.0, INTERP_HOLD)]}}))
        for name, node in acc.items()
    }
    a_sci_off = animation('scientist_off', 1, LOOP_ONESHOT, {sci: {K_OPACITY: [(0, 0.0, INTERP_HOLD)]}})
    a_sci_on = animation('scientist_on', 1, LOOP_ONESHOT, {sci: {K_OPACITY: [(0, 1.0, INTERP_HOLD)]}})
    a_props = [animation(f'prop_{k}', 1, LOOP_ONESHOT, {node: {K_OPACITY: [(0, 1.0 if i + 1 == k else 0.0, INTERP_HOLD)]} for i, node in enumerate(props)})
               for k in range(0, 6)]
    a_point = animation('point', 96, LOOP_PINGPONG, {pointer: {K_ROTATION: [(0, -0.14, INTERP_CUBIC), (96, 0.14, INTERP_CUBIC)]}})
    a_rest = animation('rest', 1, LOOP_ONESHOT, {hop: {K_Y: [(0, feet[1], INTERP_HOLD)]}})
    a_hop = animation('hop', 40, LOOP_ONESHOT, {hop: {K_Y: [
        (0, feet[1], INTERP_CUBIC), (12, feet[1] - 60, INTERP_CUBIC), (24, feet[1], INTERP_CUBIC), (31, feet[1] - 14, INTERP_CUBIC), (40, feet[1], INTERP_HOLD)]}})

    # --- State machine -------------------------------------------------------------------
    w.obj(STATE_MACHINE, animName='Mood')
    w.obj(SM_NUMBER, smName='mood', numberValue=0.0)   # input 0
    w.obj(SM_BOOL, smName='mia')                         # input 1
    w.obj(SM_TRIGGER, smName='tap')                      # input 2
    acc_inputs = {}
    for i, name in enumerate(('headband', 'glasses', 'hat', 'flower')):
        w.obj(SM_BOOL, smName=name)                      # inputs 3..6
        acc_inputs[name] = 3 + i
    w.obj(SM_BOOL, smName='scientist')                   # input 7
    w.obj(SM_NUMBER, smName='prop', numberValue=0.0)     # input 8
    for i, name in enumerate(('leaf', 'bowtie', 'nightcap')):
        w.obj(SM_BOOL, smName=name)                      # inputs 9, 10, 11
        acc_inputs[name] = 9 + i
    for i, name in enumerate(OUTFITS):
        w.obj(SM_BOOL, smName=name)                      # inputs 12 and on
        acc_inputs[name] = 12 + i
    MOOD, MIA, TAP, SCIENTIST, PROP = 0, 1, 2, 7, 8

    def layer(name, states, entry_to, any_transitions=(), state_transitions=None):
        """states: animation indices -> layer states 3, 4, ... (0 entry, 1 any, 2 exit)."""
        w.obj(SM_LAYER, smName=name)
        w.obj(ENTRY_STATE)
        w.obj(TRANSITION, stateToId=entry_to)
        w.obj(ANY_STATE)
        for (to, conds, duration) in any_transitions:
            w.obj(TRANSITION, stateToId=to, transitionDuration=duration or None)
            for c in conds:
                w.obj(*c[:1], **c[1])
        w.obj(EXIT_STATE)
        for i, anim in enumerate(states):
            w.obj(ANIMATION_STATE, animationId=anim)
            for (to, props) in (state_transitions or {}).get(3 + i, []):
                w.obj(TRANSITION, stateToId=to, **props)

    def num_eq(v):
        return (COND_NUMBER, dict(inputId=MOOD, op=OP_EQUAL, condValue=float(v)))

    layer('mood', [a_happy, a_tired, a_scroll, a_outside, a_reading, a_sleeping, a_sleepy], 3,
          [(3 + m, [num_eq(m)], 250) for m in range(7)])
    layer('skin', [a_bo, a_mia], 3, [
        (3, [(COND_BOOL, dict(inputId=MIA, op=OP_NOT_EQUAL))], 0),
        (4, [(COND_BOOL, dict(inputId=MIA, op=OP_EQUAL))], 0),
    ])
    layer('breathe', [a_breathe], 3)
    layer('blink', [a_blink], 3)
    layer('glance', [a_glance], 3)
    for name, (off, on) in acc_anims.items():
        layer(name, [off, on], 3, [
            (3, [(COND_BOOL, dict(inputId=acc_inputs[name], op=OP_NOT_EQUAL))], 200),
            (4, [(COND_BOOL, dict(inputId=acc_inputs[name], op=OP_EQUAL))], 200),
        ])
    layer('scientist', [a_sci_off, a_sci_on], 3, [
        (3, [(COND_BOOL, dict(inputId=SCIENTIST, op=OP_NOT_EQUAL))], 200),
        (4, [(COND_BOOL, dict(inputId=SCIENTIST, op=OP_EQUAL))], 200),
    ])
    layer('point', [a_point], 3)
    layer('prop', a_props, 3, [(3 + k, [(COND_NUMBER, dict(inputId=PROP, op=OP_EQUAL, condValue=float(k)))], 0) for k in range(6)])
    layer('tap', [a_rest, a_hop], 3,
          [(4, [(COND_TRIGGER, dict(inputId=TAP))], 0)],
          {4: [(3, dict(flags=FLAG_ENABLE_EXIT_TIME | FLAG_EXIT_TIME_IS_PERCENTAGE, exitTime=100))]})
    return bytes(w.b)


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, '..', '..', 'app', 'src', 'main', 'assets', 'tend_character.riv')
    data = build()
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    open(out, 'wb').write(data)
    print(f'{out}: {len(data)} bytes')
