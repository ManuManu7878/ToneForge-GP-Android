#!/usr/bin/env python3
"""Offline binary sanity tests for v0.5 scene generation.
Mirrors the Java record edits on synthetic GP-5/GP-50 .prst containers.
"""
import struct

REC_MODELS=bytes([0x03,0x30,0x28,0x00])
REC_BYPASS=bytes([0x01,0x30,0x04,0x00])
REC_PARAMS=bytes([0x04,0x30,0x40,0x01])
DST,AMP,CAB,EQ,DLY,RVB,NS=2,3,4,5,7,8,9

def crc8(data,start=0x15):
    c=0
    for x in data[start:]:
        c ^= x
        for _ in range(8):
            c = (((c<<1)^0x07)&0xff) if c&0x80 else ((c<<1)&0xff)
    return c

def make_fake(device):
    n=507 if device=='GP5' else 552
    b=bytearray(n)
    b[:5]=b'GP-5\x00'[:5] if device=='GP5' else b'GP-50'
    mo=0x70; bo=0xa0; po=0xac
    b[mo:mo+4]=REC_MODELS
    for k in range(10):
        # categories are placeholders; only N->S category identity matters in real files.
        cat=0x0f if k==NS else (0x20+k)
        struct.pack_into('<BBBB',b,mo+4+k*4,k,0,0,cat)
    b[bo:bo+4]=REC_BYPASS
    struct.pack_into('<I',b,bo+4,0x3ff)  # all on initially
    b[po:po+4]=REC_PARAMS
    for i in range(80): struct.pack_into('<f',b,po+4+i*4,50.0)
    b[0x14]=crc8(b)
    return b

def set_bit(mask,bit,on): return mask|(1<<bit) if on else mask&~(1<<bit)
def set_param(b,base,blk,alg,val): struct.pack_into('<f',b,base+(blk*8+alg)*4,float(val))
def patch_scene(src,snap,scene):
    b=bytearray(src); mb=b.find(REC_MODELS)+4; bb=b.find(REC_BYPASS)+4; pb=b.find(REC_PARAMS)+4
    # scene tuple: od_on, od(g,t,v), eq5, dly_on/model/mix/ms/fb, rvb_on/model/mix/dec/damp
    od_on, od, eq, dly, rvb = scene
    internal=snap-1
    for blk,idx in ((NS,internal),(DST,0),(EQ,0),(DLY,dly[1]),(RVB,rvb[1])):
        off=mb+blk*4; b[off:off+3]=bytes([idx&255,0,0])
    mask=struct.unpack_from('<I',b,bb)[0]
    for blk,on in ((NS,True),(AMP,False),(CAB,False),(DST,od_on),(EQ,True),(DLY,dly[0]),(RVB,rvb[0])):
        mask=set_bit(mask,blk,on)
    struct.pack_into('<I',b,bb,mask)
    for alg,val in enumerate(od): set_param(b,pb,DST,alg,val)
    for alg,val in enumerate(eq): set_param(b,pb,EQ,alg,val)
    set_param(b,pb,EQ,5,50)
    for alg,val in enumerate((dly[2],dly[3],dly[4],1)): set_param(b,pb,DLY,alg,val)
    set_param(b,pb,RVB,0,rvb[2]); set_param(b,pb,RVB,1,rvb[3])
    if rvb[1] in (0,5):
        set_param(b,pb,RVB,2,rvb[4]); set_param(b,pb,RVB,3,1)
    else: set_param(b,pb,RVB,2,1)
    b[0x14]=crc8(b)
    return b

slash=[
(False,(8,52,60),(46,49,54,56,52),(False,4,10,280,14),(True,1,12,28,55)),
(True,(10,54,66),(45,49,55,58,53),(True,4,12,285,16),(True,5,14,34,58)),
(True,(16,56,75),(44,49,57,61,54),(True,4,20,350,25),(True,5,18,42,58)),
]

for dev in ('GP5','GP50'):
    src=make_fake(dev)
    outs=[patch_scene(src,51,s) for s in slash]
    assert all(len(x)==len(src) for x in outs)
    assert all(x[0x14]==crc8(x) for x in outs)
    assert len({bytes(x) for x in outs})==3
    for i,x in enumerate(outs):
        mb=x.find(REC_MODELS)+4; bb=x.find(REC_BYPASS)+4; pb=x.find(REC_PARAMS)+4
        assert x[mb+NS*4]==50
        mask=struct.unpack_from('<I',x,bb)[0]
        assert mask&(1<<NS) and not(mask&(1<<AMP)) and not(mask&(1<<CAB)) and mask&(1<<EQ) and mask&(1<<RVB)
        assert bool(mask&(1<<DST))==slash[i][0]
        assert bool(mask&(1<<DLY))==slash[i][3][0]
        # EQ band 4 (1.6k) and delay time should rise toward lead in this profile.
        eq16=struct.unpack_from('<f',x,pb+(EQ*8+3)*4)[0]
        dtime=struct.unpack_from('<f',x,pb+(DLY*8+1)*4)[0]
        assert eq16==slash[i][2][3] and dtime==slash[i][3][3]
    assert struct.unpack_from('<f',outs[2],outs[2].find(REC_PARAMS)+4+(DLY*8+1)*4)[0] == 350.0
print('PASS: v0.5 GP5/GP50 scene binary edits, block states, parameters, SnapTone mapping and CRC')
