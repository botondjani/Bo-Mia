"""
Generates app/src/main/assets/tend_character.riv: Bo and Mia on one rig.

Contract (see docs/rive-rig.md and ui/Character.kt):
  artboard "Character" 500x500, state machine "Mood" with inputs
    mood (number: 0 happy, 1 tired, 2 scrolling, 3 outside, 4 reading), mia (bool), tap (trigger).
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

    # --- Drawables, front to back ---------------------------------------------------------
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
    a_rest = animation('rest', 1, LOOP_ONESHOT, {hop: {K_Y: [(0, feet[1], INTERP_HOLD)]}})
    a_hop = animation('hop', 40, LOOP_ONESHOT, {hop: {K_Y: [
        (0, feet[1], INTERP_CUBIC), (12, feet[1] - 60, INTERP_CUBIC), (24, feet[1], INTERP_CUBIC), (31, feet[1] - 14, INTERP_CUBIC), (40, feet[1], INTERP_HOLD)]}})

    # --- State machine -------------------------------------------------------------------
    w.obj(STATE_MACHINE, animName='Mood')
    w.obj(SM_NUMBER, smName='mood', numberValue=0.0)   # input 0
    w.obj(SM_BOOL, smName='mia')                         # input 1
    w.obj(SM_TRIGGER, smName='tap')                      # input 2
    MOOD, MIA, TAP = 0, 1, 2

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
