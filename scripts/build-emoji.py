"""把 emoji 素材处理成前端可用的透明圆形图标。

原图是 512x512 的圆形图标，但四角带灰色背景（不是纯白），
直接按方形裁切会露出灰边，在白色卡片上很难看。
处理方式：
  1. 按内容外接圆裁切
  2. 用圆形蒙版把四角变透明
  3. 缩放到 128x128
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'emoji')
DST = os.path.join(ROOT, 'canteen-frontend', 'public', 'ui', 'emoji')
os.makedirs(DST, exist_ok=True)

EMOJI_MAP = {
    '01_食堂窗口.png': 'stall.png',
    '02_在售菜品.png': 'dish.png',
    '03_累计评价.png': 'review.png',
    '04_今日评价.png': 'today.png',
    '05_新增收藏.png': 'favorite.png',
    '06_投诉留言.png': 'feedback.png',
}

OUT_SIZE = 128
# 蒙版略小于外接圆，把最外圈的灰边一起切掉
MASK_SCALE = 0.94
# 边缘羽化像素，避免锯齿
FEATHER = 2


def process(src, dst):
    im = Image.open(src).convert('RGBA')
    w, h = im.size

    # 1) 居中裁正方形（圆形图标居中）
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    im = im.crop((left, top, left + side, top + side))

    # 2) 缩放到目标尺寸（先放大 4 倍做蒙版，边缘更平滑）
    scale = 4
    big = OUT_SIZE * scale
    im = im.resize((big, big), Image.LANCZOS)

    # 3) 圆形蒙版（带羽化）
    mask = Image.new('L', (big, big), 0)
    draw = ImageDraw.Draw(mask)
    inset = int(big * (1 - MASK_SCALE) / 2)
    draw.ellipse((inset, inset, big - inset - 1, big - inset - 1), fill=255)
    # 高斯模糊做羽化
    from PIL import ImageFilter
    mask = mask.filter(ImageFilter.GaussianBlur(FEATHER * scale / 2))

    # 4) 应用蒙版并缩回目标尺寸
    out = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    out = out.resize((OUT_SIZE, OUT_SIZE), Image.LANCZOS)
    out.save(dst, optimize=True)
    return out.size


def main():
    for src_name, dst_name in EMOJI_MAP.items():
        src = os.path.join(SRC, src_name)
        if not os.path.exists(src):
            print('  跳过:', src_name)
            continue
        dst = os.path.join(DST, dst_name)
        size = process(src, dst)
        kb = os.path.getsize(dst) / 1024
        print(f'  {dst_name}  {size}  {kb:.0f}KB')


if __name__ == '__main__':
    main()
