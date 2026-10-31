"""Probe file: two overlapping circles (red first, blue second) and a state machine
with a number input 'n' that switches between two one-frame animations moving the blue one."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from riv import *

w = Writer()
header(w)
w.obj(BACKBOARD)
w.obj(ARTBOARD, name='Probe', width=200.0, height=200.0)          # 0
w.obj(SHAPE, name='red', parentId=0, x=80.0, y=100.0)               # 1
w.obj(ELLIPSE, parentId=1, pWidth=100.0, pHeight=100.0)             # 2
w.obj(FILL, parentId=1)                                             # 3
w.obj(SOLID_COLOR, parentId=3, color=0xFFE76649)                    # 4
w.obj(SHAPE, name='blue', parentId=0, x=120.0, y=100.0)             # 5
w.obj(ELLIPSE, parentId=5, pWidth=100.0, pHeight=100.0)             # 6
w.obj(FILL, parentId=5)                                             # 7
w.obj(SOLID_COLOR, parentId=7, color=0xFF8EC4E8)                    # 8
for i, y in enumerate([100.0, 40.0]):
    w.obj(LINEAR_ANIMATION, animName=f'pos{i}', fps=60, duration=1, loop=LOOP_ONESHOT)
    w.obj(KEYED_OBJECT, objectId=5)
    w.obj(KEYED_PROPERTY, propertyKey=K_Y)
    w.obj(KF_DOUBLE, frame=0, interpolation=INTERP_LINEAR, value=y)
w.obj(STATE_MACHINE, animName='SM')
w.obj(SM_NUMBER, smName='n')
w.obj(SM_LAYER, smName='L')
w.obj(ENTRY_STATE)                                  # state 0
w.obj(TRANSITION, stateToId=3)
w.obj(ANY_STATE)                                    # state 1
w.obj(TRANSITION, stateToId=3)
w.obj(COND_NUMBER, inputId=0, op=OP_EQUAL, condValue=0.0)
w.obj(TRANSITION, stateToId=4)
w.obj(COND_NUMBER, inputId=0, op=OP_EQUAL, condValue=1.0)
w.obj(EXIT_STATE)                                   # state 2
w.obj(ANIMATION_STATE, animationId=0)               # state 3
w.obj(ANIMATION_STATE, animationId=1)               # state 4
open(sys.argv[1], 'wb').write(w.b)
print(len(w.b), 'bytes')
