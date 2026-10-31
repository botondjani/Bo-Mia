"""
Generates app/src/main/assets/tend_character.riv: Bo and Mia on one rig.

Contract (see docs/rive-rig.md and ui/Character.kt):
  artboard "Character" 500x500, state machine "Mood" with inputs
    mood (number: 0 happy, 1 tired, 2 scrolling, 3 outside, 4 reading), mia (bool), tap (trigger),
    headband, glasses, hat, flower, leaf, bowtie (bool: accessories earned by habits),
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
        x, y = T(cx, cy)
        shape = self.add(SHAPE, name=name, parentId=parent, x=x, y=y)
        self.add(ELLIPSE, parentId=shape, pWidth=w * S, pHeight=h * S)
        return shape, self.paint(shape, fill)[0]

    def rect(self, name, parent, cx, cy, w, h, r, fill, origin_top=False):
        x, y = T(cx, cy)
        shape = self.add(SHAPE, name=name, parentId=parent, x=x, y=y)
        self.add(RECTANGLE, parentId=shape, pWidth=w * S, pHeight=h * S, cornerRadius=r * S,
                 originY=0.0 if origin_top else None)
        return shape, self.paint(shape, fill)[0]

    def path(self, name, parent, verts, closed, fill=None, stroke=None, width=2.6):
        """verts: (x, y, in_point or None, out_point or None) in painter coordinates."""
        shape = self.add(SHAPE, name=name, parentId=parent)
        path = self.add(POINTS_PATH, parentId=shape, isClosed=closed)
        for (vx, vy, vin, vout) in verts:
            px, py = T(vx, vy)
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
    acc = {name: r.node(f'acc_{name}', body) for name in ('hat', 'flower', 'glasses', 'headband', 'leaf', 'bowtie')}
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

    # Accessories sit on top of everything on the head.
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
    for i, name in enumerate(('leaf', 'bowtie')):
        w.obj(SM_BOOL, smName=name)                      # inputs 9, 10
        acc_inputs[name] = 9 + i
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

    layer('mood', [a_happy, a_tired, a_scroll, a_outside, a_reading], 3,
          [(3 + m, [num_eq(m)], 250) for m in range(5)])
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
