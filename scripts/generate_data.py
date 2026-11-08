# -*- coding: utf-8 -*-
"""
生成食堂反馈系统的自洽演示数据：
1. 分配头像（74 个用户）
2. 新增 8 家档口 + 32 个菜品 + 8 个商家账号
3. 为所有 20 家档口重造自洽的历史订单(dining_order + dining_order_item) + 每日销售(dish_sales)
   保证：销量 = 订单菜品项数量之和；收入 = 订单项金额之和
"""
import random
import datetime
from collections import defaultdict

random.seed(20261108)

OUT = []

def sql(s):
    OUT.append(s)

# ============ 头像分配 ============
avatar_ids = [
    10002,10003,10004,10005,10006,10007,10008,10009,10010,10011,10012,10013,10014,
    10015,10016,10017,10018,10019,10020,10021,10022,10023,10024,10025,10026,10027,
    10028,10029,10030,10031,10032,10033,10034,10035,10036,10037,10038,10039,10040,
    10041,10042,10043,10044,10045,10046,10047,10048,10049,10050,10051,10052,10053,
    10054,10055,10056,10057,10058,10059,10060,10061,10062,10063,10064,10065,10066,
    10067,10068,10069,10070,10071,10072,10073,10074,10075,10076,10077,10079,10081,
    10082,10083,10084,10085,10086,10087,10088,10089,10090,10091,10092,10093,10094,
    10095,10097,10098,10099,10100,10101,10102,10103,10104,10105,10106,10107,10108,
    10109,10110,10111,10112
]
png_ids = {10036,10037,10038,10087,10097,10100,10102,10103,10104,10105,10106,10107,10108,10109,10110,10112}
jpeg_ids = {10089}

def avatar_ext(aid):
    if aid in png_ids:
        return '.png'
    if aid in jpeg_ids:
        return '.jpeg'
    return '.jpg'

def avatar_url(aid):
    return f"/ui/avatars/{aid}{avatar_ext(aid)}"

all_user_ids = [1, 2] + list(range(3, 63)) + list(range(66, 78)) + list(range(78, 86))
assert len(all_user_ids) == 82, len(all_user_ids)

sql("-- ===== 头像分配（74 个用户每人一张不同头像） =====")
for idx, uid in enumerate(all_user_ids):
    aid = avatar_ids[idx]
    sql(f"UPDATE sys_user SET avatar = '{avatar_url(aid)}' WHERE id = {uid};")

# ============ 新档口 + 新菜品 + 新商家 ============
new_stalls = [
    (13, "粤式早茶", "S104", "south", 1, "早茶点心", "陈师傅"),
    (14, "川湘小炒", "S204", "south", 2, "川湘菜", "李师傅"),
    (15, "台式牛肉面", "S105", "south", 1, "面食", "王师傅"),
    (16, "家常风味", "S205", "south", 2, "家常菜", "张师傅"),
    (17, "烧烤西餐", "N107", "north", 1, "烧烤", "刘师傅"),
    (18, "面点工坊", "N108", "north", 1, "面点", "赵师傅"),
    (19, "广式点心", "N109", "north", 1, "早茶点心", "孙师傅"),
    (20, "炒饭炒面", "N110", "north", 1, "快餐", "周师傅"),
]

dish_map = {
    "10001": ("水晶小笼包", "小吃", 12),
    "10002": ("海鲜麻辣香锅", "荤菜", 28),
    "10005": ("土豆炖牛腩", "荤菜", 22),
    "10007": ("珍珠糯米丸子", "荤菜", 15),
    "10015": ("泰式海鲜沙拉", "凉菜", 18),
    "10016": ("红烧牛肉面", "主食", 18),
    "10017": ("家常红烧肉", "荤菜", 12),
    "10018": ("板栗烧鸡", "荤菜", 20),
    "10019": ("糖醋咕咾肉", "荤菜", 18),
    "10020": ("蜜汁烤肋排", "荤菜", 32),
    "10021": ("凉拌花生木耳", "凉菜", 8),
    "10022": ("老醋花生", "凉菜", 7),
    "10023": ("韩式炸酱面", "主食", 16),
    "10024": ("鲜肉小笼包", "小吃", 13),
    "10026": ("鲜肉大包", "小吃", 4),
    "10029": ("牛肉拉面", "主食", 15),
    "10053": ("麻辣牛肉面", "主食", 18),
    "10036": ("中式早点套餐", "主食", 10),
    "10037": ("广式烧卖虾饺", "小吃", 16),
    "10038": ("清炒时蔬", "素菜", 9),
    "10040": ("西红柿炒鸡蛋", "素菜", 8),
    "10043": ("扬州炒饭", "主食", 14),
    "10044": ("家常炒面", "主食", 12),
    "10046": ("啤酒鸭", "荤菜", 26),
    "10047": ("白灼菜心", "素菜", 9),
    "10048": ("大肉包", "小吃", 5),
    "10049": ("卤水双拼", "凉菜", 15),
    "10050": ("卤肉卷饼", "小吃", 10),
    "10057": ("白面馒头", "主食", 2),
    "10059": ("小炒鸡块", "荤菜", 18),
    "10060": ("鲜肉包", "小吃", 4),
    "10062": ("灌汤小笼包", "小吃", 14),
}

stall_dishes = {
    13: ["10001", "10024", "10037", "10062"],
    14: ["10002", "10005", "10018", "10046"],
    15: ["10016", "10029", "10053", "10023"],
    16: ["10017", "10019", "10040", "10059"],
    17: ["10020", "10015", "10021", "10022"],
    18: ["10026", "10048", "10057", "10060"],
    19: ["10007", "10036", "10049", "10050"],
    20: ["10038", "10043", "10044", "10047"],
}

sql("")
sql("-- ===== 新增 8 家档口 =====")
for (sid, name, win, area, floor, cat, mgr) in new_stalls:
    sql(f"INSERT INTO canteen_stall (id, name, window_no, area, floor, category, manager_name, manager_phone, cover_image, hygiene_score, status) "
        f"VALUES ({sid}, '{name}', '{win}', '{area}', {floor}, '{cat}', '{mgr}', '1380000{sid:04d}', '/dishes/10001.jpg', {random.randint(88, 98)}.0, 1);")

sql("")
sql("-- ===== 新增 32 个菜品 =====")
dish_id_counter = 53
dish_id_of = {}
for (sid, name, win, area, floor, cat, mgr) in new_stalls:
    dish_id_of[sid] = []
    for img in stall_dishes[sid]:
        dish_id_counter += 1
        dname, dcat, dprice = dish_map[img]
        dish_id_of[sid].append(dish_id_counter)
        sql(f"INSERT INTO dish (id, stall_id, name, price, category, image, description, featured, status) "
            f"VALUES ({dish_id_counter}, {sid}, '{dname}', {dprice}.00, '{dcat}', '/dishes/{img}.jpg', '{dname}，{cat}特色美食。', {1 if random.random()<0.5 else 0}, 1);")

sql("")
sql("-- ===== 新增 8 个商家账号（密码复用 100012 的哈希，即 123456） =====")
PW_HASH = "$2a$10$9a4ogJdOUtpWZADoX3U7c.LtVttldK8A7QsqIxc6dir9UPTmhKIc."
new_merchants = [
    (78, "100024", 13),
    (79, "100025", 14),
    (80, "100026", 15),
    (81, "100027", 16),
    (82, "100028", 17),
    (83, "100029", 18),
    (84, "100030", 19),
    (85, "100031", 20),
]
for (uid, uname, sid) in new_merchants:
    sname = next(s[1] for s in new_stalls if s[0] == sid)
    aid = avatar_ids[all_user_ids.index(uid)]
    sql(f"INSERT INTO sys_user (id, username, password, real_name, role, stall_id, avatar, balance, status) "
        f"VALUES ({uid}, '{uname}', '{PW_HASH}', '{sname} 商家', 'MERCHANT', {sid}, '{avatar_url(aid)}', 0.00, 1);")

# ============ 自洽的订单 + 销售数据 ============
sql("")
sql("-- ===== 清空旧的订单与销售数据 =====")
sql("DELETE FROM dining_order_item;")
sql("DELETE FROM dining_order;")
sql("DELETE FROM dish_sales;")
sql("DELETE FROM pay_record;")

sql("")
sql("-- ===== 重造自洽的历史订单 + 每日销售 =====")

existing_stall_dishes = {
    1: [1,2,3,4,5], 2: [6,7,8,9,10], 3: [11,12,13,14,15], 4: [16,17,18,19],
    5: [20,21,22,23], 6: [24,25,26,27], 7: [28,29,30,31,32], 8: [33,34,35,36],
    9: [37,38,39,40], 10: [41,42,43,44], 11: [45,46,47,48], 12: [49,50,51,52],
}
all_stalls_dishes = {}
for sid in range(1, 13):
    all_stalls_dishes[sid] = existing_stall_dishes[sid]
for sid in dish_id_of:
    all_stalls_dishes[sid] = dish_id_of[sid]

existing_prices = {
    1:12,2:8,3:16,4:12,5:9,6:15,7:14,8:16,9:13,10:17,11:12,12:10,13:11,14:12,15:13,
    16:20,17:30,18:15,19:14,20:18,21:5,22:22,23:16,24:16,25:24,26:14,27:20,28:18,29:12,
    30:10,31:12,32:10,33:12,34:8,35:7,36:9,37:18,38:16,39:17,40:16,41:12,42:14,43:11,
    44:13,45:14,46:12,47:10,48:6,49:20,50:18,51:17,52:12,
}
existing_name_map = {
    1:"宫保鸡丁",2:"麻婆豆腐",3:"水煮肉片",4:"鱼香肉丝",5:"干煸豆角",6:"蜜汁叉烧饭",7:"白切鸡饭",
    8:"烧鸭饭",9:"豉汁排骨饭",10:"卤水双拼饭",11:"牛肉拉面",12:"番茄鸡蛋面",13:"老北京炸酱面",
    14:"山西刀削面",15:"岐山臊子面",16:"麻辣香锅小份",17:"麻辣香锅大份",18:"素菜香锅",19:"香锅宽粉",
    20:"羊肉泡馍",21:"烤羊肉串",22:"新疆大盘鸡",23:"牛肉小炒",24:"香煎鸡胸沙拉",25:"三文鱼牛油果沙拉",
    26:"缤纷时蔬沙拉",27:"金枪鱼藜麦沙拉",28:"水晶虾饺皇",29:"爆浆流沙包",30:"豉汁蒸肠粉",31:"豉汁蒸凤爪",
    32:"糯米鸡",33:"杨枝甘露",34:"顺德双皮奶",35:"黑糖珍珠奶茶",36:"芋泥啵啵奶茶",37:"腊味煲仔饭",
    38:"香菇滑鸡煲仔饭",39:"豉汁排骨煲仔饭",40:"窝蛋牛肉煲仔饭",41:"猪肉白菜水饺",42:"三鲜水饺",
    43:"韭菜鸡蛋水饺",44:"酸菜猪肉水饺",45:"两荤一素套餐",46:"一荤两素套餐",47:"红烧肉",48:"番茄炒蛋",
    49:"黑椒牛柳铁板饭",50:"香辣鸡腿铁板饭",51:"照烧鸡排铁板饭",52:"铁板时蔬豆腐",
}

dish_info = {}
for did in existing_prices:
    dish_info[did] = (existing_prices[did], existing_name_map[did])
for (sid, name, win, area, floor, cat, mgr) in new_stalls:
    for idx, img in enumerate(stall_dishes[sid]):
        dname, dcat, dprice = dish_map[img]
        did = dish_id_of[sid][idx]
        dish_info[did] = (dprice, dname)

student_ids = list(range(3, 63))

order_id_counter = 0
order_item_id_counter = 0
pay_id_counter = 0
sales_agg = defaultdict(lambda: [0, 0.0])

stall_hot = {}
for sid in range(1, 21):
    stall_hot[sid] = random.uniform(0.6, 1.6)

today = datetime.date(2026, 11, 8)

def esc(s):
    return s.replace("'", "''")

def gen_orders_for_stall(sid, day):
    global order_id_counter, order_item_id_counter, pay_id_counter
    dishes = all_stalls_dishes[sid]
    hot = stall_hot[sid]
    n_orders = max(1, int(random.randint(3, 10) * hot))
    if day.weekday() >= 5:
        n_orders = int(n_orders * 1.3) + 1

    for _ in range(n_orders):
        order_id_counter += 1
        uid = random.choice(student_ids)
        r = random.random()
        if r < 0.85:
            status = "DONE"; pay_status = "PAID"
        elif r < 0.93:
            status = "PENDING"; pay_status = "PAID"
        else:
            status = "CANCELED"; pay_status = "UNPAID"

        n_items = random.randint(1, 4)
        chosen = random.sample(dishes, min(n_items, len(dishes)))
        items = []
        total = 0.0
        for did in chosen:
            price, dname = dish_info[did]
            qty = random.randint(1, 2)
            amt = price * qty
            total += amt
            items.append((did, dname, price, qty, amt))

        order_no = f"D{order_id_counter:08d}"
        pickup = datetime.datetime.combine(day, datetime.time(random.randint(11, 13), random.randint(0, 59)))
        create_time = datetime.datetime.combine(day, datetime.time(random.randint(8, 11), random.randint(0, 59)))

        if status == "CANCELED":
            total = 0.0

        remark = "" if random.random() < 0.7 else random.choice(["少辣", "不要香菜", "多饭", "打包带走"])

        pay_no_val = "NULL" if pay_status == "UNPAID" else f"'{order_no}'"
        pay_channel_val = "NULL" if pay_status == "UNPAID" else "'ALIPAY'"
        paid_time_val = "NULL" if pay_status == "UNPAID" else f"'{create_time}'"

        sql(f"INSERT INTO dining_order (id, order_no, user_id, stall_id, pickup_time, remark, total_amount, status, create_time, update_time, pay_status, pay_no, pay_channel, paid_time) "
            f"VALUES ({order_id_counter}, '{order_no}', {uid}, {sid}, '{pickup}', '{esc(remark)}', {total:.2f}, '{status}', '{create_time}', '{create_time}', '{pay_status}', {pay_no_val}, {pay_channel_val}, {paid_time_val});")

        for (did, dname, price, qty, amt) in items:
            order_item_id_counter += 1
            sql(f"INSERT INTO dining_order_item (id, order_id, dish_id, dish_name, price, quantity, create_time) "
                f"VALUES ({order_item_id_counter}, {order_id_counter}, {did}, '{esc(dname)}', {price:.2f}, {qty}, '{create_time}');")
            # 非取消订单（DONE + PENDING）都计入销量
            if status != "CANCELED":
                key = (sid, did, str(day))
                sales_agg[key][0] += qty
                sales_agg[key][1] += amt

        if pay_status == "PAID":
            pay_id_counter += 1
            sql(f"INSERT INTO pay_record (id, pay_no, order_id, user_id, channel, amount, status, trade_no, paid_time, create_time, update_time) "
                f"VALUES ({pay_id_counter}, 'PAY{order_id_counter:08d}', {order_id_counter}, {uid}, 'ALIPAY', {total:.2f}, 'SUCCESS', 'TRADE{order_id_counter:08d}', '{create_time}', '{create_time}', '{create_time}');")

days_list = []
for i in range(180, 0, -1):
    days_list.append(today - datetime.timedelta(days=i))

sql("")
sql("-- ===== 历史订单生成（过去 180 天，每档口每天随机 3~10 单） =====")
for day in days_list:
    for sid in range(1, 21):
        gen_orders_for_stall(sid, day)

sql("")
sql("-- ===== dish_sales 每日销售（由订单聚合，保证与订单一致） =====")
for (sid, did, day_str), (qty, amt) in sorted(sales_agg.items()):
    sql(f"INSERT INTO dish_sales (stall_id, dish_id, dish_name, sale_date, quantity, amount) "
        f"VALUES ({sid}, {did}, '{esc(dish_info[did][1])}', '{day_str}', {qty}, {amt:.2f});")

content = "\n".join(OUT)
with open("scripts/generated_data.sql", "w", encoding="utf-8") as f:
    f.write(content)

print(f"订单数: {order_id_counter}")
print(f"订单项数: {order_item_id_counter}")
print(f"销售记录数: {len(sales_agg)}")
print(f"支付记录数: {pay_id_counter}")
print(f"新菜品 id 范围: 54 ~ {dish_id_counter}")
print("SQL 已生成到 scripts/generated_data.sql")
