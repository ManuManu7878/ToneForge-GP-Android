#!/usr/bin/env python3
"""Offline sanity tests for ToneForge .prst SnapTone patching logic."""

def crc8(data, start=0x15):
    c=0
    for x in data[start:]:
        c ^= x
        for _ in range(8):
            c = (((c << 1) ^ 0x07) & 0xff) if c & 0x80 else ((c << 1) & 0xff)
    return c

def make_fake(device):
    n=507 if device=='GP5' else 552
    b=bytearray(n)
    magic=b'GP-5\x00' if device=='GP5' else b'GP-50'
    b[:5]=magic
    marker=0x80
    b[marker:marker+4]=bytes([0x03,0x30,0x28,0x00])
    base=marker+4
    # Ten model records. Make record 4 the N->S category (0x0f).
    for k in range(10):
        off=base+k*4
        b[off:off+4]=bytes([1+k,0,0,0x0f if k==4 else k])
    b[0x14]=crc8(b)
    return b, base+4*4

def patch(data, device, display_slot, name):
    exp=507 if device=='GP5' else 552
    assert len(data)==exp
    assert 51 <= display_slot <= 80
    b=bytearray(data)
    marker=b.find(bytes([0x03,0x30,0x28,0x00]))
    assert marker >= 0
    ns=-1
    base=marker+4
    for k in range(10):
        off=base+k*4
        if b[off+3] == 0x0f:
            ns=off; break
    assert ns>=0
    b[ns]=display_slot-1
    safe=''.join(ch for ch in name if ch.isalnum() or ch in '._ -').strip().replace(' ','_')[:40]
    raw=safe.replace('_',' ').encode('latin-1','replace')[:16]
    b[0x19:0x29]=b'\0'*16
    b[0x19:0x19+len(raw)]=raw
    b[0x14]=crc8(b)
    return b, ns

for dev in ('GP5','GP50'):
    src, expected_ns=make_fake(dev)
    for display, internal in ((51,50),(80,79)):
        out, ns=patch(src,dev,display,"Slash Sweet Child Solo")
        assert ns==expected_ns
        assert out[ns]==internal
        assert out[0x14]==crc8(out)
        assert bytes(out[0x19:0x29]).rstrip(b'\0') == b'Slash Sweet Chil'  # 16 bytes
print('PASS: GP5/GP50 SnapTone slot mapping, name patch and CRC-8')
