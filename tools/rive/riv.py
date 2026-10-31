"""
Minimal writer for the Rive runtime format (.riv, major version 7).

Only what tend's character needs: artboard, nodes, shapes (ellipse, rectangle, points
path), fills/strokes, linear animations with double/colour keyframes, and a state machine
with number/bool/trigger inputs. Type and property keys come from the rive-runtime
generated headers (include/rive/generated/*_base.hpp).
"""
import struct

# Type keys
BACKBOARD, ARTBOARD, NODE, SHAPE, ELLIPSE, RECTANGLE, POINTS_PATH = 23, 1, 2, 3, 4, 7, 16
STRAIGHT_VERTEX, CUBIC_DETACHED_VERTEX, FILL, STROKE, SOLID_COLOR = 5, 6, 20, 24, 18
LINEAR_ANIMATION, KEYED_OBJECT, KEYED_PROPERTY, KF_DOUBLE, KF_COLOR, CUBIC_EASE = 31, 25, 26, 30, 37, 28
STATE_MACHINE, SM_NUMBER, SM_BOOL, SM_TRIGGER, SM_LAYER = 53, 56, 59, 58, 57
ENTRY_STATE, ANY_STATE, EXIT_STATE, ANIMATION_STATE, TRANSITION = 63, 62, 64, 61, 65
COND_NUMBER, COND_BOOL, COND_TRIGGER = 70, 71, 68

# Property keys (encoding)
P = {
    'name': (4, 's'), 'parentId': (5, 'u'), 'width': (7, 'f'), 'height': (8, 'f'),
    'x': (13, 'f'), 'y': (14, 'f'), 'rotation': (15, 'f'), 'scaleX': (16, 'f'), 'scaleY': (17, 'f'), 'opacity': (18, 'f'),
    'pWidth': (20, 'f'), 'pHeight': (21, 'f'), 'vx': (24, 'f'), 'vy': (25, 'f'), 'radius': (26, 'f'),
    'originX': (123, 'f'), 'originY': (124, 'f'),
    'cornerRadius': (31, 'f'), 'isClosed': (32, 'b'), 'color': (37, 'c'), 'thickness': (47, 'f'), 'cap': (48, 'u'), 'join': (49, 'u'), 'transformAffectsStroke': (50, 'b'),
    'objectId': (51, 'u'), 'propertyKey': (53, 'u'), 'animName': (55, 's'), 'fps': (56, 'u'), 'duration': (57, 'u'), 'loop': (59, 'u'),
    'x1': (63, 'f'), 'y1': (64, 'f'), 'x2': (65, 'f'), 'y2': (66, 'f'),
    'frame': (67, 'u'), 'interpolation': (68, 'u'), 'interpolatorId': (69, 'u'), 'value': (70, 'f'), 'colorValue': (88, 'c'),
    'inRotation': (84, 'f'), 'inDistance': (85, 'f'), 'outRotation': (86, 'f'), 'outDistance': (87, 'f'),
    'smName': (138, 's'), 'numberValue': (140, 'f'), 'boolValue': (141, 'b'), 'animationId': (149, 'u'),
    'stateToId': (151, 'u'), 'flags': (152, 'u'), 'transitionDuration': (158, 'u'), 'exitTime': (160, 'u'),
    'inputId': (155, 'u'), 'op': (156, 'u'), 'condValue': (157, 'f'), 'defaultStateMachineId': (236, 'u'),
}

# Animatable property keys
K_X, K_Y, K_SCALE_X, K_SCALE_Y, K_OPACITY, K_COLOR = 13, 14, 16, 17, 18, 37

FLAG_ENABLE_EXIT_TIME, FLAG_EXIT_TIME_IS_PERCENTAGE = 1 << 2, 1 << 3
OP_EQUAL, OP_NOT_EQUAL = 0, 1
LOOP_ONESHOT, LOOP_LOOP, LOOP_PINGPONG = 0, 1, 2
INTERP_HOLD, INTERP_LINEAR, INTERP_CUBIC = 0, 1, 2


class Writer:
    def __init__(self):
        self.b = bytearray()

    def varuint(self, v):
        v = int(v)
        while True:
            byte = v & 0x7F
            v >>= 7
            if v:
                self.b.append(byte | 0x80)
            else:
                self.b.append(byte)
                return

    def obj(self, type_key, **props):
        self.varuint(type_key)
        for name, value in props.items():
            if value is None:
                continue
            key, kind = P[name]
            self.varuint(key)
            if kind == 'u':
                self.varuint(value)
            elif kind == 'f':
                self.b += struct.pack('<f', value)
            elif kind == 'c':
                self.b += struct.pack('<I', value & 0xFFFFFFFF)
            elif kind == 'b':
                self.b.append(1 if value else 0)
            elif kind == 's':
                data = value.encode('utf-8')
                self.varuint(len(data))
                self.b += data
        self.varuint(0)


def header(w, file_id=0):
    w.b += b'RIVE'
    w.varuint(7)  # major
    w.varuint(0)  # minor
    w.varuint(file_id)
    w.varuint(0)  # empty property table of contents: every key used is known to the runtime
