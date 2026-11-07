"""把 emoji 与背景素材处理成前端可用的尺寸。

emoji: 512x512 圆形图标，内容居中，裁成正方形后缩放到 96x96（2x 用于高分屏）。
background: 极宽横幅（约 9:1），已含完整文案与按钮，
            裁掉左右各一点点避免边缘瑕疵，统一缩放到 1600 宽。
"""
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_EMOJI = os.path.join(ROOT, 'emoji')
SRC_BG = os.path.join(ROOT, 'background')
DST = os.path.join(ROOT, 'canteen-frontend', 'public', 'ui')
os.makedirs(os.path.join(DST, 'emoji'), exist_ok=True)
os.makedirs(os.path.join(DST, 'banner'), exist_ok=True)

# emoji 映射：文件名 -> 前端用的英文名
EMOJI_MAP = {
    '01_食堂窗口.png': 'stall.png',
    '02_在售菜品.png': 'dish.png',
    '03_累计评价.png': 'review.png',
    '04_今日评价.png': 'today.png',
    '05_新增收藏.png': 'favorite.png',
    '06_投诉留言.png': 'feedback.png',
}

EMOJI_SIZE = 128  # 输出 128，前端按 40~44 显示，留足 2x 余量


def process_emoji(src, dst):
    im = Image.open(src).convert('RGBA')
    w, h = im.size
    # 圆形图标居中，按最短边裁正方形
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    im = im.crop((left, top, left + side, top + side))
    im = im.resize((EMOJI_SIZE, EMOJI_SIZE), Image.LANCZOS)
    im.save(dst, optimize=True)
    return im.size


def process_banner(src, dst, target_w=1600):
    im = Image.open(src).convert('RGB')
    w, h = im.size
    # 左右各裁 1% 去掉可能的描边瑕疵，保持比例
    trim = int(w * 0.005)
    im = im.crop((trim, 0, w - trim, h))
    w2, h2 = im.size
    new_h = int(h2 * target_w / w2)
    im = im.resize((target_w, new_h), Image.LANCZOS)
    im.save(dst, quality=88, optimize=True)
    return im.size


def main():
    print('=== emoji ===')
    for src_name, dst_name in EMOJI_MAP.items():
        src = os.path.join(SRC_EMOJI, src_name)
        if not os.path.exists(src):
            print('  跳过（不存在）:', src_name)
            continue
        dst = os.path.join(DST, 'emoji', dst_name)
        size = process_emoji(src, dst)
        kb = os.path.getsize(dst) / 1024
        print(f'  {src_name} -> emoji/{dst_name}  {size}  {kb:.0f}KB')

    print('=== banner ===')
    for i in (1, 2, 3):
        src = os.path.join(SRC_BG, f'{i}.png')
        if not os.path.exists(src):
            continue
        dst = os.path.join(DST, 'banner', f'banner{i}.jpg')
        size = process_banner(src, dst)
        kb = os.path.getsize(dst) / 1024
        print(f'  {i}.png -> banner/banner{i}.jpg  {size}  {kb:.0f}KB')


if __name__ == '__main__':
    main()
