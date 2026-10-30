"""สร้างสไปรต์พิกเซลอาร์ตของเกม (วาดที่ความละเอียดต่ำ แล้วขยาย 2 เท่าแบบ nearest-neighbor)
ผลลัพธ์: sprite sheet แนวนอน เรียงเฟรมจากซ้ายไปขวา ทุกตัวหันขวา (เกมพลิกซ้ายให้เอง)
"""
import sys
from PIL import Image, ImageDraw

OUT = sys.argv[1]
SCALE = 2

OUTLINE = (16, 18, 36, 255)
T = (0, 0, 0, 0)

def c(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def new(w, h):
    return Image.new('RGBA', (w, h), T)

def rect(im, x, y, w, h, col):
    if w <= 0 or h <= 0:
        return
    ImageDraw.Draw(im).rectangle([x, y, x + w - 1, y + h - 1], fill=col)

def ell(im, x0, y0, x1, y1, col):
    ImageDraw.Draw(im).ellipse([x0, y0, x1, y1], fill=col)

def poly(im, pts, col):
    ImageDraw.Draw(im).polygon(pts, fill=col)

def px(im, x, y, col):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), col)

def outline(im, col=OUTLINE):
    """ใส่ขอบ 1 พิกเซลรอบรูป (ช่องโปร่งใสที่ติดกับช่องทึบ 4 ทิศ)"""
    src = im.copy()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src.getpixel((nx, ny))[3] != 0:
                    im.putpixel((x, y), col)
                    break
    return im

def up(im):
    return im.resize((im.width * SCALE, im.height * SCALE), Image.NEAREST)

def sheet(frames):
    w, h = frames[0].size
    out = Image.new('RGBA', (w * len(frames), h), T)
    for i, f in enumerate(frames):
        out.paste(f, (i * w, 0))
    return out

# ------------------------------------------------------------------ PLAYER: ผู้ใหญ่บ้าน (20x30 -> 40x60)
SKIN = c('#e2a877'); SKIN_D = c('#b97f52'); HAIR = c('#231c1c')
KH = c('#cdb066'); KH_D = c('#a08a46'); KH_L = c('#e8d38f'); BUTTON = c('#fff1a8')
PANTS = c('#7c6b3d'); PANTS_D = c('#5e4f2b'); SHOE = c('#2a211c'); SHOE_D = c('#1a1410')
CLOTH_R = c('#d23a3a'); CLOTH_W = c('#fbefe0'); BELT = c('#4a2f1a'); GOLD = c('#ffd23a')

def player(b=0, back=(0, 0), front=(0, 0), arm_up=False, flutter=0):
    im = new(20, 30)
    # แขนหลัง (เสื้อกากีเข้ม)
    rect(im, 3, 11 + b, 3, 6, KH_D)
    # ขา
    for (dx, lift), x0, leg_col, shoe_col in ((back, 6, PANTS_D, SHOE_D), (front, 10, PANTS, SHOE)):
        x = x0 + dx
        bottom = 26 - lift
        rect(im, x, 20 + b, 4, max(1, bottom - (20 + b)), leg_col)
        rect(im, x, bottom, 6, 3, shoe_col)            # รองเท้าแตะ/หนัง ปลายชี้ขวา
        rect(im, x + 1, bottom, 4, 1, c('#6a5646'))
    # ลำตัว: เสื้อราชปะแตนสีกากี
    rect(im, 5, 10 + b, 10, 9, KH)
    rect(im, 5, 10 + b, 2, 9, KH_D)
    rect(im, 8, 11 + b, 4, 3, KH_L)                    # กระเป๋าอก
    rect(im, 8, 11 + b, 4, 1, KH_D)
    for yy in (12, 15):                                 # กระดุมทอง
        px(im, 13, yy + b, BUTTON)
    px(im, 13, 17 + b, BUTTON)
    # เข็มขัด
    rect(im, 5, 18 + b, 10, 2, BELT)
    rect(im, 9, 18 + b, 2, 2, GOLD)
    # ผ้าขาวม้าพาดไหล่ (ลายตาราง แดง-ขาว) ชายห้อยด้านหลัง
    rect(im, 5, 10 + b, 9, 2, CLOTH_R)
    for xx in range(5, 14, 2):
        px(im, xx, 10 + b, CLOTH_W)
    for xx in range(6, 14, 2):
        px(im, xx, 11 + b, CLOTH_W)
    rect(im, 2, 10 + b + flutter, 3, 6, CLOTH_R)
    for yy in range(11, 16, 2):
        px(im, 3, yy + b + flutter, CLOTH_W)
    px(im, 2, 15 + b + flutter, CLOTH_W)
    # หัว (หันขวา)
    rect(im, 5, 1 + b, 10, 9, SKIN)
    rect(im, 5, 3 + b, 2, 6, SKIN_D)
    rect(im, 5, 1 + b, 10, 3, HAIR)                    # ผมสั้นดำ
    rect(im, 5, 3 + b, 3, 3, HAIR)
    rect(im, 12, 3 + b, 3, 1, HAIR)                     # ขมับ
    rect(im, 11, 5 + b, 2, 2, c('#ffffff'))             # ตา
    px(im, 12, 5 + b, c('#111111')); px(im, 12, 6 + b, c('#111111'))
    rect(im, 11, 4 + b, 3, 1, HAIR)                     # คิ้ว
    px(im, 15, 6 + b, SKIN)                             # จมูก
    px(im, 13, 8 + b, c('#8a4a3a'))                     # ยิ้ม
    px(im, 14, 8 + b, c('#8a4a3a'))
    px(im, 12, 7 + b, c('#e58a7a'))                     # แก้มอมชมพู
    rect(im, 8, 9 + b, 4, 1, SKIN_D)                    # คอ
    # แขนหน้า + มือ
    if arm_up:
        rect(im, 13, 9 + b, 3, 5, KH)
        rect(im, 14, 8 + b, 3, 2, SKIN)
    else:
        rect(im, 13, 12 + b, 4, 4, KH)
        rect(im, 15, 15 + b, 3, 2, SKIN)
    return up(outline(im))

def make_player():
    return sheet([
        player(0, (0, 0), (0, 0)),
        player(1, (0, 0), (0, 0), flutter=1),
        player(0, (-3, 0), (3, 1), flutter=1),
        player(1, (-1, 0), (1, 0), flutter=0),
        player(0, (3, 1), (-3, 0), flutter=1),
        player(1, (1, 0), (-1, 0), flutter=0),
        player(0, (-2, 3), (3, 2), arm_up=True, flutter=2),
    ])

# ------------------------------------------------------------------ WALKER: ผีผ้าขาว (20x25 -> 40x50)
G_W = c('#f6f4ec'); G_S = c('#cfd5dc'); G_L = c('#ffffff'); G_EYE = c('#1d1d2b')

def walker(foot_back=0, foot_front=0, squash=0):
    im = new(20, 25)
    # ปลายเท้า/ชายผ้าโผล่ล่าง
    rect(im, 4 + foot_back, 21, 4, 3, G_S)
    rect(im, 11 + foot_front, 21, 4, 3, G_W)
    # ตัวผ้าห่อ
    ell(im, 2, 2 + squash, 18, 22, G_W)
    rect(im, 2, 12 + squash, 17, 9, G_W)
    ell(im, 2, 2 + squash, 8, 14, G_S)                  # เงาด้านหลัง
    rect(im, 2, 14 + squash, 4, 7, G_S)
    # ชายผ้าเป็นลอน
    for xx in (3, 7, 11, 15):
        rect(im, xx, 20, 2, 2, G_W)
    # ปมผ้าบนหัว
    poly(im, [(8, 3 + squash), (11, 0), (13, 3 + squash)], G_W)
    px(im, 10, 1, G_S)
    # แขนยื่นไปข้างหน้า (ท่าผีน่ารัก)
    rect(im, 15, 11 + squash, 5, 3, G_W)
    rect(im, 15, 13 + squash, 5, 1, G_S)
    # ตาโตกลมดำ + ประกายตา
    rect(im, 9, 7 + squash, 3, 4, G_EYE)
    rect(im, 14, 7 + squash, 3, 4, G_EYE)
    px(im, 10, 8 + squash, G_L); px(im, 15, 8 + squash, G_L)
    # แก้มชมพู + ปากกลมเล็ก
    rect(im, 8, 12 + squash, 2, 1, c('#f4a6b0'))
    rect(im, 16, 12 + squash, 2, 1, c('#f4a6b0'))
    rect(im, 12, 13 + squash, 2, 2, c('#5a3a4a'))
    return up(outline(im))

def make_walker():
    return sheet([walker(-1, 1, 0), walker(1, -1, 1)])

# ------------------------------------------------------------------ FLYER: กระสือ (หัวลอยมีแสงไฟ) (22x15 -> 44x30)
H_SKIN = c('#f1dcc0'); H_SKIN_D = c('#d0b08c'); H_HAIR = c('#2b2340'); H_HAIR_L = c('#4a3a70')
FL_O = c('#ff9a2e'); FL_Y = c('#ffe066'); FL_R = c('#ff6a3a')

def flyer(tail_up):
    im = new(22, 15)
    # แสงรัศมีรอบหัว (โปร่งแสง)
    # หางแสงไฟพลิ้ว (แทนปีก)
    if tail_up:
        poly(im, [(10, 7), (0, 2), (3, 6), (0, 9), (10, 10)], FL_O)
        poly(im, [(10, 8), (4, 5), (6, 8), (10, 9)], FL_Y)
    else:
        poly(im, [(10, 7), (0, 10), (3, 8), (1, 13), (10, 11)], FL_O)
        poly(im, [(10, 8), (5, 10), (6, 9), (10, 10)], FL_Y)
    # ผมยาวสยายหลังหัว
    ell(im, 8, 2, 19, 13, H_HAIR)
    rect(im, 8, 6, 5, 6, H_HAIR)
    # หน้า
    ell(im, 11, 3, 20, 12, H_SKIN)
    ell(im, 11, 8, 17, 12, H_SKIN_D)
    rect(im, 12, 4, 7, 2, H_HAIR)                       # ผมม้า
    px(im, 14, 3, H_HAIR_L); px(im, 17, 3, H_HAIR_L)
    # ตาเรืองแสง + ยิ้มน่ารัก
    rect(im, 15, 6, 2, 3, c('#1a1424'))
    rect(im, 18, 6, 2, 3, c('#1a1424'))
    px(im, 15, 6, FL_Y); px(im, 18, 6, FL_Y)
    px(im, 15, 10, c('#8a4a3a')); px(im, 16, 11, c('#8a4a3a')); px(im, 17, 11, c('#8a4a3a')); px(im, 18, 10, c('#8a4a3a'))
    px(im, 14, 9, c('#f4a6b0')); px(im, 19, 9, c('#f4a6b0'))
    out = outline(im)
    glow = new(22, 15)
    ImageDraw.Draw(glow).ellipse([9, 0, 21, 14], fill=(255, 224, 120, 70))
    glow.alpha_composite(out)
    return up(glow)

def make_flyer():
    return sheet([flyer(True), flyer(False)])

# ------------------------------------------------------------------ TANK: ผีตาโขน (28x35 -> 56x70)
K_RED = c('#d8443a'); K_YEL = c('#f2c230'); K_GRN = c('#3ea85a'); K_BLU = c('#3a6ad8')
K_RED_D = c('#9a2a2a'); K_YEL_D = c('#b88a14'); K_GRN_D = c('#287a3e'); K_BLU_D = c('#274a9a')
MASK = c('#f3e4bc'); MASK_D = c('#c9b383'); HAT = c('#c99a4b'); HAT_D = c('#8f6a2a'); HAT_L = c('#e6c079')
K_DARK = c('#2b2140')

def tank(leg_a=0, leg_b=0, lean=0, eye=(c('#ffffff'), c('#1d1d2b')), rear=False, bob=0):
    im = new(28, 35)
    # ขา (กางเกงม่วงเข้ม + เท้าเปล่า)
    rect(im, 7 + leg_a, 27, 6, 6, K_DARK)
    rect(im, 6 + leg_a, 31, 8, 3, c('#8a5a3a'))
    rect(im, 15 + leg_b, 27, 6, 6, K_DARK)
    rect(im, 14 + leg_b, 31, 8, 3, c('#a8683f'))
    # ตัว: เสื้อผ้าชิ้นปะหลากสี (คล้ายผ้าตาโขน)
    x = 5 + lean
    cols = [(K_RED, K_RED_D), (K_YEL, K_YEL_D), (K_GRN, K_GRN_D), (K_BLU, K_BLU_D)]
    for i in range(18):
        col = cols[(i // 3) % 4][0]
        rect(im, x + i, 10 + bob, 1, 17, col)
    rect(im, x, 10 + bob, 3, 17, K_RED_D)
    for yy in (14, 18, 22):                              # ลายขอบผ้า
        rect(im, x, yy + bob, 18, 1, c('#ffffff', 70))
    rect(im, x, 25 + bob, 18, 2, K_DARK)
    # แขน/แถบไหล่ (ซ้าย-ขวา)
    rect(im, x - 2, 11 + bob, 5, 9, K_YEL_D)
    rect(im, x - 2, 11 + bob, 5, 2, K_YEL)
    rect(im, x + 15, 11 + bob, 5, 9, K_GRN)
    rect(im, x + 15, 11 + bob, 5, 2, c('#7ad48c'))
    # หัวกลมหน้ากากผีตาโขน
    hx = x + 4 + (2 if rear else 0)
    # หมวกหวดนึ่งข้าว (ทรงกรวย)
    poly(im, [(hx + 1, 5 + bob), (hx + 6, 0 + bob), (hx + 11, 5 + bob)], HAT)
    poly(im, [(hx + 1, 5 + bob), (hx + 6, 0 + bob), (hx + 5, 5 + bob)], HAT_D)
    rect(im, hx + 6, 1 + bob, 1, 3, HAT_L)
    for xx in (hx + 3, hx + 8):
        px(im, xx, 3 + bob, HAT_D)
    # หน้ากาก
    rect(im, hx, 5 + bob, 12, 7, MASK)
    rect(im, hx, 5 + bob, 3, 7, MASK_D)
    rect(im, hx, 5 + bob, 12, 1, K_RED)                  # แถบแดงหน้าผาก
    rect(im, hx - 2, 7 + bob, 2, 4, K_RED)               # หู
    rect(im, hx + 12, 7 + bob, 2, 4, K_RED)
    # ตาโตกลม
    for ex in (hx + 2, hx + 7):
        rect(im, ex, 6 + bob, 3, 3, eye[0])
        rect(im, ex + 1, 7 + bob, 2, 2, eye[1])
    # จมูกยาวสีแดง
    poly(im, [(hx + 6, 8 + bob), (hx + 13, 9 + bob), (hx + 6, 11 + bob)], K_RED)
    # ปากยิ้มกว้าง ฟันขาว
    rect(im, hx + 1, 11 + bob, 5, 1, K_DARK)
    for tx in (hx + 2, hx + 4):
        px(im, tx, 11 + bob, c('#ffffff'))
    return up(outline(im))

def make_tank():
    return sheet([
        tank(0, 0, bob=0),
        tank(1, -1, bob=1),
        tank(-1, 1, lean=-2, eye=(c('#fff070'), c('#d02020')), rear=True),   # 2 windup ตาเหลือง
        tank(-2, 2, lean=2, eye=(c('#ff3a3a'), c('#ffffff'))),                # 3 charge ตาแดง
    ])

# ------------------------------------------------------------------ HEAL: ดอกบัว (12x12 -> 24x24)
LOTUS = [
    "............",
    ".....WW.....",
    "....WPPW....",
    "..W.WPPW.W..",
    ".WPWPPPPWPW.",
    ".WPPPYYPPPW.",
    "..PPPYYPPP..",
    "...PPPPPP...",
    ".GG.GGGG.GG.",
    "..GGGGGGGG..",
    "............",
    "............",
]
LCOL = {'W': c('#ffe3ec'), 'P': c('#f06a9a'), 'Y': c('#ffd23a'), 'G': c('#3ea85a')}

def make_heal():
    im = new(12, 12)
    for y, row in enumerate(LOTUS):
        for x, ch in enumerate(row):
            if ch in LCOL:
                px(im, x, y + 1, LCOL[ch])
    return up(outline(im))

# ------------------------------------------------------------------ เขียนไฟล์ + พรีวิว
files = {
    'player.png': make_player(),
    'walker.png': make_walker(),
    'flyer.png': make_flyer(),
    'tank.png': make_tank(),
    'heal.png': make_heal(),
}
for name, img in files.items():
    img.save(f'{OUT}/{name}')
    print(name, img.size)

rows = list(files.values())
W = max(i.width for i in rows) * 3 + 40
H = sum(i.height * 3 + 14 for i in rows) + 14
prev = Image.new('RGBA', (W, H), (96, 150, 110, 255))
y = 14
for img in rows:
    big = img.resize((img.width * 3, img.height * 3), Image.NEAREST)
    prev.alpha_composite(big, (20, y))
    y += big.height + 14
prev.save(sys.argv[2])
