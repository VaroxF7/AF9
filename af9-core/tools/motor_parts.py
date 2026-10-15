"""
Paints the 32x32 textures of the motor parts of every tier (LV to UHV): the finned housing, the copper-wound stator, the
slotted rotor with its magnet poles, the assembled motor (GT's own motor item, overridden under assets/gtceu), and the
industrial motor housing. Run from af9-core:

    python tools/motor_parts.py

Everything is drawn here (no base images); each tier recolours the shell with its own (dark, mid, light) ramp, the tier
colours the placeholders already used. Copper, the magnet poles and the dark grey shaft are the same on every tier.
"""
import json
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets')
OUT = os.path.join(RES, 'af9', 'textures', 'item')
GT_OUT = os.path.join(RES, 'gtceu', 'textures', 'item', 'metaitems')

# tier: (dark, mid, light)
RAMPS = {
    'lv': ((0x3a, 0x3a, 0x3a), (0xa7, 0xa7, 0xa7), (0xd9, 0xd9, 0xd9)),
    'mv': ((0x2b, 0x40, 0x4b), (0x7d, 0xb9, 0xd8), (0xa2, 0xf0, 0xff)),
    'hv': ((0x4b, 0x4d, 0x4f), (0xd8, 0xdc, 0xe4), (0xff, 0xff, 0xff)),
    'ev': ((0x52, 0x31, 0x51), (0xed, 0x8e, 0xea), (0xff, 0xb8, 0xff)),
    'iv': ((0x24, 0x2c, 0x48), (0x68, 0x7e, 0xce), (0x87, 0xa3, 0xff)),
    'luv': ((0x39, 0x2d, 0x42), (0xa4, 0x82, 0xbf), (0xd5, 0xa9, 0xf8)),
    'zpm': ((0x18, 0x3c, 0x3f), (0x47, 0xad, 0xb6), (0x5c, 0xe0, 0xec)),
    'uv': ((0x44, 0x1e, 0x24), (0xc3, 0x57, 0x69), (0xfd, 0x71, 0x88)),
    'uhv': ((0x51, 0x52, 0x54), (0xe8, 0xec, 0xf2), (0xff, 0xff, 0xff)),
}
# reinforced titanium niobium alloy: AF9Materials colour 0x8a94c8
RTNA = ((0x2c, 0x31, 0x52), (0x8a, 0x94, 0xc8), (0xb0, 0xba, 0xff))
# the dark grey of the shaft and bearing hub
GREY = ((0x22, 0x22, 0x26), (0x62, 0x60, 0x66), (0x9a, 0x99, 0x9e))
N=32
OL=(0x1d,0x1c,0x21)
CU=[(0x5a,0x26,0x16),(0x8a,0x41,0x29),(0xc1,0x5a,0x36),(0xe7,0x7c,0x56),(0xff,0xa8,0x80)]
RED=[(0x55,0x18,0x16),(0x8f,0x2c,0x26),(0xc4,0x4a,0x3a),(0xe8,0x70,0x5c)]
def lerp(a,b,t):
    t=max(0,min(1,t)); return tuple(int(round(a[i]+(b[i]-a[i])*t)) for i in range(3))
def shade(r,v):
    d,m,l=r
    return lerp(d,m,v/0.5) if v<0.5 else lerp(m,l,(v-0.5)/0.5)
def ell(x,y,cx,cy,rx,ry): return ((x-cx)/rx)**2+((y-cy)/ry)**2
def outline(px):
    out=dict(px)
    for (x,y) in px:
        for dx,dy in((1,0),(-1,0),(0,1),(0,-1)):
            n=(x+dx,y+dy)
            if n not in px and 0<=n[0]<N and 0<=n[1]<N: out[n]=OL
    return out
def img(px):
    im=Image.new('RGBA',(N,N),(0,0,0,0))
    for (x,y),c in px.items(): im.putpixel((x,y),c+(255,))
    return im
def px_all():
    for y in range(N):
        for x in range(N): yield x,y

def housing(ramp):
    px={}
    # shell: drum, axis horizontal, opening at left. top y 5..27
    for x,y in px_all():
        if 9<=x<=28:
            inside = 5<=y<=27 if x<=24 else ell(x,y,24,16,4.6,11.2)<=1
            if inside:
                t=(y-5)/22
                v=0.97-0.85*t
                if y in (9,14,19,24): v-=0.22        # ribs: dark groove under each
                if y in (8,13,18,23): v+=0.1
                if x>=24: v-=0.12*(x-24)/4           # roll-off toward the end plate
                px[(x,y)]=shade(ramp,v)
    # end plate (right) with boss and bolts
    for x,y in px_all():
        r=ell(x,y,25.5,16,3.2,8.5)
        if r<=1 and x>=23: px[(x,y)]=shade(ramp,0.38+0.25*(1-r)-0.1*((y-8)/16))
    for x,y in px_all():
        if ell(x,y,26,16,1.7,2.8)<=1: px[(x,y)]=shade(ramp,0.2 if y>16 else 0.7)
    for (bx,by) in((26,9),(26,23),(26,12),(26,20)):
        px[(bx,by)]=shade(ramp,0.95); px[(bx+1,by)]=shade(ramp,0.15)
    # front flange ring (left) with fin teeth
    cx,cy=10,16
    for x,y in px_all():
        r=ell(x,y,cx,cy,7.6,11.4)
        if r<=1:
            ang=math.atan2((y-cy)/11.4,(x-cx)/7.6)
            tooth=int((ang+math.pi)/(2*math.pi)*20)%2==0
            if r>0.82 and not tooth: continue
            px[(x,y)]=shade(ramp,0.72-0.45*r+ (0.25 if (y<cy and r>0.6) else 0))
    # mounting holes in the flange
    for (hx,hy) in((5,7),(15,7),(5,25),(15,25)):
        if (hx,hy) in px: px[(hx,hy)]=(0x10,0x10,0x14); px[(hx+1,hy)]=(0x10,0x10,0x14)
    # hollow bore (stator removed): inner wall visible, dark deep end
    for x,y in px_all():
        r=ell(x,y,cx,cy,4.9,8.0)
        if r<=1:
            wall = ell(x,y,cx+2.2,cy,4.2,7.2)>1
            if wall:
                px[(x,y)]=lerp(shade(ramp,0.05),shade(ramp,0.55),(y-8)/16 if y>cy else 0.0)
            else:
                d=ell(x,y,cx+2.2,cy,2.6,4.6)
                px[(x,y)]=lerp((0x0c,0x0c,0x10),(0x2a,0x2a,0x32),d)
    # bright inner rim on the upper-left
    for x,y in px_all():
        r=ell(x,y,cx,cy,4.9,8.0)
        if 0.82<r<=1.35 and y<cy+1 and x<cx+1 and (x,y) in px: px[(x,y)]=shade(ramp,0.97)
    return outline(px)

def stator(ramp):
    px={}
    cx,cy=10,16
    for x,y in px_all():
        if 10<=x<=27 and 5<=y<=27:
            t=(y-5)/22
            v=0.95-0.8*t+(0.1 if x%3==0 else -0.04)   # laminations
            px[(x,y)]=shade(ramp,v)
    for x,y in px_all():   # rear winding overhang
        if ell(x,y,27,16,3.6,8.8)<=1 and x>=26: px[(x,y)]=CU[1+((x+y)%3)] if (x+y)%2 else CU[2]
    for x,y in px_all():   # front copper ring: loops
        r=ell(x,y,cx,cy,7.4,11.4)
        if r<=1:
            ang=math.atan2((y-cy)/11.4,(x-cx)/7.6)
            loop=int((ang+math.pi)/(2*math.pi)*24)
            c=CU[(loop%3)+1]
            if r>0.8: c=lerp(c,CU[0],0.35)
            px[(x,y)]=lerp(c,CU[4],0.25*(1-r)*(1 if y<cy else 0.3))
    # a few strand gaps
    for x,y in px_all():
        r=ell(x,y,cx,cy,7.4,11.4)
        if r<=1 and (x*5+y*3)%11==0: px[(x,y)]=CU[0]
    # steel slot teeth ring in the bore, then dark bore
    for x,y in px_all():
        r=ell(x,y,cx,cy,5.0,8.0)
        if r<=1:
            ang=math.atan2((y-cy)/8.0,(x-cx)/5.0)
            tooth=int((ang+math.pi)/(2*math.pi)*16)%2==0
            if r>0.55: px[(x,y)]=shade(ramp,0.8 if tooth else 0.3)
            else: px[(x,y)]=lerp((0x0c,0x0c,0x10),(0x30,0x30,0x38),(y-8)/16)
    return outline(px)

def rotor(ramp):
    px={}
    sh=lambda v: shade(GREY,v)
    for x,y in px_all():   # shaft, both sides
        if (0<=x<=12 or 27<=x<=31) and 13<=y<=18:
            px[(x,y)]=sh(0.95-0.75*(y-13)/5)
    for x in (0,1):       # chamfered tip
        for y in (13,18): px.pop((x,y),None)
    for x,y in px_all():   # laminated core
        if 11<=x<=26 and 5<=y<=27:
            v=0.97-0.85*(y-5)/22 + (0.07 if x%3==0 else 0)
            px[(x,y)]=shade(ramp,v)
    for x,y in px_all():   # magnet poles on the top
        for (x0,x1) in ((14,18),(20,24)):
            if x0<=x<=x1 and 6<=y<=11:
                px[(x,y)]=RED[3] if y==6 else RED[2] if y<9 else RED[1]
        if x in (19,) and 6<=y<=11: px[(x,y)]=shade(ramp,0.15)
    # left face: ellipse with rim, 8 vent holes, hub
    cx,cy=12,16
    for x,y in px_all():
        r=ell(x,y,cx,cy,6.4,11.4)
        if r<=1: px[(x,y)]=shade(ramp,0.6-0.35*r+(0.25 if y<cy and r>0.8 else 0))
    for k in range(10):
        a=math.radians(k*36+18)
        hx,hy=cx+4.1*math.cos(a),cy+7.8*math.sin(a)
        for x,y in px_all():
            if ell(x,y,hx,hy,1.0,1.5)<=1: px[(x,y)]=(0x12,0x12,0x16)
    for x,y in px_all():   # bearing hub
        r=ell(x,y,cx,cy,2.4,3.6)
        if r<=1: px[(x,y)]=sh(0.35+0.5*(1-r)) if r>0.35 else sh(0.8)
    for x,y in px_all():   # shaft re-drawn across the hub
        if 0<=x<=14 and 14<=y<=17: px[(x,y)]=sh(0.95-0.75*(y-14)/3)
    for y in (13,18):      # collar
        for x in (7,8): px[(x,y)]=sh(0.35)
    return outline(px)


def motor(ramp):
    """Assembled motor: the housing with the stator's copper ring and the rotor's shaft in the mouth."""
    px=housing(ramp)
    px={k:v for k,v in px.items()}
    cx,cy=10,16
    for x,y in px_all():
        r=ell(x,y,cx,cy,5.2,8.4)
        if r<=1:   # copper winding in the mouth
            ang=math.atan2((y-cy)/8.4,(x-cx)/5.2)
            c=CU[1+int((ang+math.pi)/(2*math.pi)*16)%3]
            if r>0.75: c=lerp(c,CU[0],0.3)
            px[(x,y)]=lerp(c,CU[4],0.3*(1-r)*(1 if y<cy else 0.2))
    for x,y in px_all():   # rotor core face
        r=ell(x,y,cx,cy,3.4,5.6)
        if r<=1: px[(x,y)]=shade(ramp,0.55-0.3*r+(0.2 if y<cy else 0))
    for k in range(6):
        a=math.radians(k*60+30); hx,hy=cx+2.0*math.cos(a),cy+3.5*math.sin(a)
        for x,y in px_all():
            if ell(x,y,hx,hy,0.6,0.9)<=1: px[(x,y)]=(0x14,0x14,0x18)
    sh=lambda v: shade(GREY,v)
    for x in range(0,12):
        for y in range(14,19): px[(x,y)]=sh(0.95-0.7*(y-14)/4)
    for y in range(13,20):
        px[(0,y)]=OL
    for x in (0,1,2):
        for y in (13,19): px.pop((x,y),None)
    for y in range(13,20):
        for x in (4,5): px[(x,y)]=sh(0.45+0.4*(1-abs(y-16)/3))
    px={k:v for k,v in px.items()}
    # re-outline the shaft
    base={k:v for k,v in px.items() if v!=OL}
    return outline(base)


# the two status lights on the industrial housing's side change colour every few frames
LED_COLOURS = {'g': (0x5c, 0xff, 0x7a), 'a': (0xff, 0xb0, 0x2e), 'r': (0xff, 0x3a, 0x3a), 'o': (0x30, 0x34, 0x3a)}
LED_UPPER, LED_LOWER = (20, 11), (20, 21)
LED_FRAMES = ('ga', 'gr', 'ag', 'rg', 'ra', 'ga', 'og', 'ao', 'gg', 'ar')


def industrial():
    """The housing in the alloy's colours with a small controller module (a BMS board) on top of it."""
    im = img(housing(RTNA))
    board, board_lit, chip, pin = (0x2f, 0x5d, 0x4a), (0x4b, 0x8a, 0x6c), (0x16, 0x16, 0x1a), (0xd9, 0xb8, 0x4a)
    led_ok, led_warn = (0x5c, 0xff, 0x7a), (0xff, 0xb0, 0x2e)
    for y in range(0, 6):  # the module: x 12..22, y 0..5, sitting on the housing's top edge
        for x in range(12, 23):
            edge = y == 0 or x in (12, 22)
            im.putpixel((x, y), OL + (255,) if edge else (board_lit if y < 2 else board) + (255,))
    for x in range(13, 22):  # the connector row, where the board meets the housing
        im.putpixel((x, 5), (pin if x % 2 else OL) + (255,))
    for x, y in ((14, 3), (15, 3), (14, 4), (15, 4), (18, 3), (19, 3)):  # controller chips
        im.putpixel((x, y), chip + (255,))
    for x, c in ((14, led_ok), (17, led_warn), (20, led_ok)):  # status lights
        im.putpixel((x, 1), c + (255,))
    return im


def industrial_strip(base):
    """An animation strip: the module and the housing, and for each frame the colours of the side lights."""
    strip = Image.new('RGBA', (N, N * len(LED_FRAMES)), (0, 0, 0, 0))
    for i, (upper, lower) in enumerate(LED_FRAMES):
        frame = base.copy()
        for pos, key in ((LED_UPPER, upper), (LED_LOWER, lower)):
            for dx in (0, 1):
                frame.putpixel((pos[0] + dx, pos[1]), LED_COLOURS[key] + (255,))
        strip.paste(frame, (0, N * i))
    return strip


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(GT_OUT, exist_ok=True)
    for tier, ramp in RAMPS.items():
        img(housing(ramp)).save(os.path.join(OUT, tier + '_motor_housing.png'))
        img(stator(ramp)).save(os.path.join(OUT, tier + '_stator.png'))
        img(rotor(ramp)).save(os.path.join(OUT, tier + '_rotor.png'))
        img(motor(ramp)).save(os.path.join(GT_OUT, 'electric.motor.' + tier + '.png'))
    industrial_strip(industrial()).save(os.path.join(OUT, 'industrial_electric_motor_housing.png'))
    with open(os.path.join(OUT, 'industrial_electric_motor_housing.png.mcmeta'), 'w') as f:
        json.dump({'animation': {'frametime': 10}}, f)


if __name__ == '__main__':
    main()
