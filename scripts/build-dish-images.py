"""把 raw/ 里的生成图按菜名匹配到菜品 id，压缩后输出到前端 public/dishes/<id>.jpg。"""
import os, re, shutil, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "dish-images", "raw")
OUT = os.path.join(ROOT, "canteen-frontend", "public", "dishes")
SIZE = (768, 768)
QUALITY = 84

DISHES = {
    1: "宫保鸡丁", 2: "麻婆豆腐", 3: "水煮肉片", 4: "鱼香肉丝", 5: "干煸豆角",
    6: "蜜汁叉烧饭", 7: "白切鸡饭", 8: "烧鸭饭", 9: "豉汁排骨饭", 10: "卤水双拼饭",
    11: "牛肉拉面", 12: "番茄鸡蛋面", 13: "老北京炸酱面", 14: "山西刀削面", 15: "岐山臊子面",
    16: "麻辣香锅小份", 17: "麻辣香锅大份", 18: "素菜香锅", 19: "香锅宽粉", 20: "羊肉泡馍",
    21: "烤羊肉串", 22: "新疆大盘鸡", 23: "牛肉小炒", 24: "香煎鸡胸沙拉", 25: "三文鱼牛油果沙拉",
    26: "缤纷时蔬沙拉", 27: "金枪鱼藜麦沙拉", 28: "水晶虾饺皇", 29: "爆浆流沙包", 30: "豉汁蒸肠粉",
    31: "豉汁蒸凤爪", 32: "糯米鸡", 33: "杨枝甘露", 34: "顺德双皮奶", 35: "黑糖珍珠奶茶",
    36: "芋泥啵啵奶茶", 37: "腊味煲仔饭", 38: "香菇滑鸡煲仔饭", 39: "豉汁排骨煲仔饭", 40: "窝蛋牛肉煲仔饭",
    41: "猪肉白菜水饺", 42: "三鲜水饺", 43: "韭菜鸡蛋水饺", 44: "酸菜猪肉水饺", 45: "两荤一素套餐",
    46: "一荤两素套餐", 47: "红烧肉", 48: "番茄炒蛋", 49: "黑椒牛柳铁板饭", 50: "香辣鸡腿铁板饭",
    51: "照烧鸡排铁板饭", 52: "铁板时蔬豆腐",
}


def collect_raw():
    files = []
    for dirpath, _, names in os.walk(RAW):
        for n in names:
            if n.lower().endswith((".png", ".jpg", ".jpeg", ".webp")):
                files.append(os.path.join(dirpath, n))
    return files


def main():
    os.makedirs(RAW, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    files = collect_raw()
    used = set()
    plan = {}
    order = sorted(DISHES.items(), key=lambda kv: -len(kv[1]))
    for did, name in order:
        if os.path.exists(os.path.join(OUT, f"{did}.jpg")):
            plan[did] = None
            continue
        hit = None
        for f in files:
            if f in used:
                continue
            stem = re.sub(r"_?\d{4}-\d{2}-\d{2}T[\d-]+\.(png|jpg|jpeg|webp)$", "", os.path.basename(f),
                          flags=re.I)
            segs = [s for s in re.split(r"[_，,、]", stem) if s]
            if name in segs:
                hit = f
                break
        if hit:
            used.add(hit)
            plan[did] = hit

    done, missing = 0, []
    for did in sorted(plan):
        src = plan[did]
        dst = os.path.join(OUT, f"{did}.jpg")
        if src is None:
            done += 1
            continue
        try:
            im = Image.open(src).convert("RGB")
            im.thumbnail(SIZE, Image.LANCZOS)
            canvas = Image.new("RGB", SIZE, (255, 255, 255))
            canvas.paste(im, ((SIZE[0] - im.width) // 2, (SIZE[1] - im.height) // 2))
            canvas.save(dst, "JPEG", quality=QUALITY, optimize=True, progressive=True)
            done += 1
            print(f"  {did:>2} {DISHES[did]:<10} <- {os.path.basename(src)}")
        except Exception as e:
            missing.append(did)
            print(f"  {did:>2} {DISHES[did]} 失败: {e}")

    print(f"\n已就绪 {done}/{len(DISHES)}，待补: {missing if missing else '无'}")
    return missing


if __name__ == "__main__":
    sys.exit(0 if not main() else 1)
