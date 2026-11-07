const fs = require('fs');
const path = require('path');

// 固定种子伪随机，保证数据可复现
let seed = 20261022;
function rnd() { seed = (seed * 1103515245 + 12345) % 2147483648; return seed / 2147483648; }
function randInt(min, max) { return min + Math.floor(rnd() * (max - min + 1)); }
function pick(arr) { return arr[randInt(0, arr.length - 1)]; }
function esc(s) { return String(s).replace(/'/g, "''"); }
function pad(n) { return String(n).padStart(2, '0'); }

function dateStr(offsetDays, h, m) {
  const d = new Date();
  d.setDate(d.getDate() - offsetDays);
  d.setHours(h, m, randInt(0, 59));
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

// 真实在用号段，剔除 170/171/172/174 等虚拟运营商号段
const PHONE_PREFIX = ['130', '131', '132', '133', '134', '135', '136', '137', '138', '139',
  '147', '150', '151', '152', '153', '155', '156', '157', '158', '159',
  '166', '167', '173', '175', '176', '177', '178',
  '180', '181', '182', '183', '184', '185', '186', '187', '188', '189',
  '191', '193', '195', '196', '197', '198', '199'];

const usedPhones = new Set();
function genPhone() {
  let p;
  do {
    p = pick(PHONE_PREFIX) + String(randInt(0, 99999999)).padStart(8, '0');
  } while (usedPhones.has(p));
  usedPhones.add(p);
  return p;
}

const surnames = '赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦许何吕施张孔曹严华金魏陶姜谢邹喻柏水窦章云苏潘葛奚范彭郎鲁韦昌马苗凤花方俞任袁柳'.split('');
const given = ['伟', '芳', '娜', '敏', '静', '丽', '强', '磊', '军', '洋', '勇', '艳', '杰', '涛', '明', '超', '霞', '平', '刚', '鹏', '辉', '文', '宇', '浩', '晨', '雨欣', '思远', '子涵', '梦琪', '嘉怡', '若彤', '一鸣', '佳琪', '梓萱', '浩然', '欣怡'];

function genStudentNo() {
  return pick(['2023', '2024', '2025'])
    + String(randInt(1, 12)).padStart(2, '0')
    + String(randInt(1, 8)).padStart(2, '0')
    + randInt(1, 4)
    + String(randInt(1, 40)).padStart(2, '0');
}

const STUDENT_COUNT = 60;
const students = [];
for (let i = 1; i <= STUDENT_COUNT; i++) {
  students.push({
    username: 'student' + String(i).padStart(2, '0'),
    realName: pick(surnames) + pick(given),
    studentNo: genStudentNo(),
    phone: genPhone()
  });
}
// sys_user 里前两条是管理员，学生从 id=3 开始
const ADMIN_COUNT = 2;
function randStudentId() { return ADMIN_COUNT + randInt(1, STUDENT_COUNT); }

// 负责人电话单独一段，避免与学生号码撞号
const stalls = [
  { name: '川味小炒', windowNo: '01', location: '一楼东侧', category: '川菜', desc: '现点现炒，麻辣鲜香，支持自选辣度', manager: '张建国', phone: genPhone(), hygiene: 92.5 },
  { name: '粤式烧腊', windowNo: '02', location: '一楼西侧', category: '粤菜', desc: '每日现烤叉烧烧鸭，配秘制酱汁', manager: '陈志明', phone: genPhone(), hygiene: 88.0 },
  { name: '面食工坊', windowNo: '03', location: '一楼中部', category: '面食', desc: '手工拉面，汤底每日现熬', manager: '王秀兰', phone: genPhone(), hygiene: 90.5 },
  { name: '麻辣香锅', windowNo: '04', location: '二楼东侧', category: '香锅', desc: '三十余种食材自由搭配，称重计费', manager: '刘德海', phone: genPhone(), hygiene: 78.5 },
  { name: '清真风味', windowNo: '05', location: '二楼西侧', category: '清真', desc: '牛羊肉专区，独立操作间', manager: '马文清', phone: genPhone(), hygiene: 86.0 },
  { name: '轻食沙拉', windowNo: '06', location: '二楼中部', category: '轻食', desc: '低卡定制，标注每份热量', manager: '周晓雯', phone: genPhone(), hygiene: 94.0 },
  { name: '港式茶点', windowNo: '07', location: '三楼东侧', category: '茶点', desc: '现蒸点心，早午市供应', manager: '黄志强', phone: genPhone(), hygiene: 79.0 },
  { name: '饮品甜品', windowNo: '08', location: '三楼西侧', category: '甜品', desc: '鲜果现制，可选无糖', manager: '林思颖', phone: genPhone(), hygiene: 74.0 },
  { name: '煲仔饭', windowNo: '09', location: '一楼北侧', category: '煲仔', desc: '砂锅现煲，底部带锅巴', manager: '梁伯贤', phone: genPhone(), hygiene: 89.0 },
  { name: '东北饺子', windowNo: '10', location: '二楼北侧', category: '饺子', desc: '现包现煮，十余种馅可选', manager: '赵桂芳', phone: genPhone(), hygiene: 91.0 },
  { name: '自选快餐', windowNo: '11', location: '一楼南侧', category: '快餐', desc: '二十余种热菜自选，米饭免费加', manager: '孙立军', phone: genPhone(), hygiene: 76.5 },
  { name: '铁板烧饭', windowNo: '12', location: '三楼北侧', category: '铁板', desc: '铁板现煎，上桌滋滋响', manager: '吴俊杰', phone: genPhone(), hygiene: 72.5 }
];

// [菜名, 价格, 分类, 简介, 是否招牌]
const dishDefs = [
  ['宫保鸡丁', 12.00, '荤菜', '花生米酥脆，鸡腿肉滑嫩', 1],
  ['麻婆豆腐', 8.00, '素菜', '嫩豆腐配牛肉末，麻辣鲜香', 0],
  ['水煮肉片', 16.00, '荤菜', '里脊肉片，红油现泼', 1],
  ['鱼香肉丝', 12.00, '荤菜', '酸甜微辣，下饭首选', 0],
  ['干煸豆角', 9.00, '素菜', '四季豆干煸，配肉末', 0],
  ['蜜汁叉烧饭', 15.00, '主食', '半肥瘦叉烧，配青菜卤蛋', 1],
  ['白切鸡饭', 14.00, '主食', '走地鸡浸熟，姜葱蘸料', 0],
  ['烧鸭饭', 16.00, '主食', '脆皮烧鸭，配酸梅酱', 1],
  ['豉汁排骨饭', 13.00, '主食', '肋排蒸制，豉香浓郁', 0],
  ['卤水双拼饭', 17.00, '主食', '叉烧拼烧鸭，一次尝两样', 0],
  ['牛肉拉面', 12.00, '主食', '一清二白三红四绿', 1],
  ['番茄鸡蛋面', 10.00, '主食', '酸甜开胃，手工细面', 0],
  ['老北京炸酱面', 11.00, '主食', '六必居干黄酱，配菜码', 0],
  ['山西刀削面', 12.00, '主食', '面叶中厚边薄，筋道爽滑', 1],
  ['岐山臊子面', 13.00, '主食', '酸辣香，薄筋光', 0],
  ['麻辣香锅小份', 20.00, '荤菜', '约400g，可选微辣至特辣', 1],
  ['麻辣香锅大份', 30.00, '荤菜', '约700g，两人份推荐', 1],
  ['素菜香锅', 15.00, '素菜', '十余种时蔬自由搭配', 0],
  ['香锅宽粉', 14.00, '小吃', '红薯宽粉打底，吸味够劲', 0],
  ['羊肉泡馍', 18.00, '主食', '高汤慢炖，馍需自掰', 1],
  ['烤羊肉串', 5.00, '小吃', '内蒙羔羊，炭火现烤', 0],
  ['新疆大盘鸡', 22.00, '荤菜', '土鸡配皮带面，分量足', 1],
  ['牛肉小炒', 16.00, '荤菜', '牛里脊现炒，配青椒洋葱', 0],
  ['香煎鸡胸沙拉', 16.00, '素菜', '约320大卡，配油醋汁', 1],
  ['三文鱼牛油果沙拉', 24.00, '荤菜', '约420大卡，富含Omega-3', 1],
  ['缤纷时蔬沙拉', 14.00, '素菜', '约260大卡，八种时蔬', 0],
  ['金枪鱼藜麦沙拉', 20.00, '荤菜', '约380大卡，高蛋白饱腹', 0],
  ['水晶虾饺皇', 18.00, '小吃', '皮薄透亮，整虾入馅', 1],
  ['爆浆流沙包', 12.00, '小吃', '咸蛋黄流心，现蒸出笼', 1],
  ['豉汁蒸肠粉', 10.00, '主食', '米浆现蒸，豉汁调味', 0],
  ['豉汁蒸凤爪', 12.00, '小吃', '软糯脱骨，酱香入味', 0],
  ['糯米鸡', 10.00, '小吃', '荷叶包裹，糯米吸满汁', 0],
  ['杨枝甘露', 12.00, '饮品', '芒果西柚西米，冰镇', 1],
  ['顺德双皮奶', 8.00, '甜品', '水牛奶双层奶皮', 1],
  ['黑糖珍珠奶茶', 7.00, '饮品', '可选无糖/少冰', 0],
  ['芋泥啵啵奶茶', 9.00, '饮品', '手打芋泥，可选半糖', 0],
  ['腊味煲仔饭', 18.00, '主食', '砂锅现煲，腊味油润', 1],
  ['香菇滑鸡煲仔饭', 16.00, '主食', '鸡肉嫩滑，香菇入味', 1],
  ['豉汁排骨煲仔饭', 17.00, '主食', '肋排铺满，米饭吸汁', 0],
  ['窝蛋牛肉煲仔饭', 16.00, '主食', '牛肉滑嫩，配溏心蛋', 0],
  ['猪肉白菜水饺', 12.00, '主食', '现包现煮，皮薄馅大', 1],
  ['三鲜水饺', 14.00, '主食', '虾仁猪肉韭菜，鲜味足', 1],
  ['韭菜鸡蛋水饺', 11.00, '素菜', '素馅清爽，适合清淡口', 0],
  ['酸菜猪肉水饺', 13.00, '主食', '东北酸菜，解腻开胃', 0],
  ['两荤一素套餐', 14.00, '主食', '二十余种热菜任选，米饭免费加', 0],
  ['一荤两素套餐', 12.00, '主食', '米饭免费加，管饱', 0],
  ['红烧肉', 10.00, '荤菜', '肥瘦相间，入口即化', 1],
  ['番茄炒蛋', 6.00, '素菜', '食堂经典，下饭首选', 1],
  ['黑椒牛柳铁板饭', 20.00, '主食', '铁板现煎，黑椒味浓', 1],
  ['香辣鸡腿铁板饭', 18.00, '主食', '鸡腿去骨，微辣入味', 1],
  ['照烧鸡排铁板饭', 17.00, '主食', '照烧汁现刷，甜咸适口', 0],
  ['铁板时蔬豆腐', 12.00, '素菜', '素菜也能上铁板', 0]
];
const perStall = [5, 5, 5, 4, 4, 4, 5, 4, 4, 4, 4, 4];

// 每个档口一套口碑画像：星级概率 [5,4,3,2,1] + 评价热度，决定排行榜与好评率差异
const stallProfile = [
  { dist: [0.60, 0.26, 0.09, 0.035, 0.015], heat: 26 }, // 川味小炒：口碑爆棚
  { dist: [0.40, 0.29, 0.17, 0.09, 0.05], heat: 20 },   // 粤式烧腊：良好
  { dist: [0.38, 0.30, 0.18, 0.09, 0.05], heat: 22 },   // 面食工坊：良好
  { dist: [0.55, 0.27, 0.10, 0.05, 0.03], heat: 25 },   // 麻辣香锅：口碑爆棚
  { dist: [0.28, 0.27, 0.22, 0.13, 0.10], heat: 14 },   // 清真风味：一般
  { dist: [0.13, 0.20, 0.25, 0.24, 0.18], heat: 10 },   // 轻食沙拉：垫底
  { dist: [0.23, 0.24, 0.22, 0.18, 0.13], heat: 14 },   // 港式茶点：偏差
  { dist: [0.17, 0.21, 0.24, 0.21, 0.17], heat: 14 },   // 饮品甜品：偏差
  { dist: [0.42, 0.28, 0.16, 0.09, 0.05], heat: 16 },   // 煲仔饭：良好
  { dist: [0.57, 0.26, 0.10, 0.05, 0.02], heat: 22 },   // 东北饺子：口碑爆棚
  { dist: [0.30, 0.29, 0.21, 0.12, 0.08], heat: 20 },   // 自选快餐：量大管饱但味道一般
  { dist: [0.19, 0.22, 0.24, 0.20, 0.15], heat: 13 }    // 铁板烧饭：偏差
];

// 按档口定制的评价语料，{dish} 生成时替换为具体菜品名，避免出现放之四海皆准的套话
const COMMENTS = [
  {
    5: ['{dish}锅气很足，现炒出来的就是香', '麻辣鲜香，{dish}在食堂里算做得好的了', '{dish}份量实在，师傅给肉很大方', '不知道吃什么的时候就来点一份{dish}', '可以要求微辣，{dish}对不吃辣的人也友好', '师傅颠勺很利索，{dish}出锅就端走', '{dish}很下饭，配米饭能多来半碗'],
    4: ['{dish}味道不错，就是油有点大', '挺下饭的，{dish}稍微咸了一点点', '现炒很香，就是{dish}高峰期要等七八分钟', '麻辣到位，{dish}的米饭还要另算钱', '{dish}好吃，就是辣度对我来说有点顶'],
    3: ['中规中矩，{dish}偶尔吃还行', '味道可以，但{dish}没第一次惊艳', '{dish}有点凉了，像是提前炒好的', '价格小贵，{dish}的量一般', '{dish}的辣度不太稳定，时重时淡'],
    2: ['太油了，吃完{dish}碗底一层油', '偏咸，配了两碗饭才把{dish}吃下去', '这次{dish}的肉有点老，嚼着费劲', '油大还咸，吃完{dish}不太舒服', '{dish}感觉炒过头了，菜都蔫了'],
    1: ['齁咸，{dish}实在吃不下', '太辣了，完全盖住{dish}本身的味道', '{dish}是凉的，明显放了一阵', '一股油腥味，{dish}难以下咽', '{dish}价格和味道完全对不上']
  },
  {
    5: ['{dish}的肉肥瘦相间，蜜汁调得正好', '{dish}的肉给得挺多，一个人吃完全够', '{dish}肉质很嫩，蘸料也地道', '食堂里能吃到这个水平的{dish}挺惊喜', '每天都想吃{dish}，可惜是限量的', '{dish}配的卤蛋入味，青菜也清爽'],
    4: ['{dish}味道不错，就是分量偏少', '{dish}好吃，配的青菜有点凉', '肉很正，就是{dish}的饭给得少了点', '整体满意，{dish}价格稍微高一点', '{dish}出品挺稳，偶尔肉会偏柴'],
    3: ['还行，{dish}没有很惊艳', '味道一般，{dish}价格偏贵', '肉有点干，{dish}不像现烤出来的口感', '{dish}配菜比较敷衍，就两片青菜', '{dish}算是普通盒饭水平'],
    2: ['肉有点柴，{dish}像是放了一会', '太油腻了，这份{dish}肥肉太多', '偏甜，{dish}个人不太习惯', '分量确实少，男生吃{dish}根本不够', '这次{dish}的肉有点发干，不入味'],
    1: ['又干又柴，{dish}不想吃第二次', '又贵又少，{dish}完全不值', '肉有股怪味，{dish}不太新鲜', '{dish}肥肉太多，受不了', '这次{dish}彻底踩雷']
  },
  {
    5: ['{dish}的汤底确实是现熬的，鲜得很', '面条筋道，{dish}是师傅手工拉的', '{dish}分量够，加面还不加钱', '冬天来一碗{dish}特别舒服', '{dish}浇头给得实在，不是清汤寡水', '在食堂能吃上这碗{dish}挺满足的'],
    4: ['{dish}不错，汤稍微淡了点', '好吃，就是{dish}高峰期排队久', '面条口感好，{dish}的肉给得少了些', '性价比可以，希望{dish}多几种浇头', '总体满意，偶尔{dish}的面会有点坨'],
    3: ['普通水平，{dish}能吃', '{dish}的面有点煮过了，软塌塌的', '今天的{dish}汤底比较寡淡', '价格还行，{dish}份量一般', '想吃面的时候随便来碗{dish}'],
    2: ['面坨了，{dish}端上来就不太热', '汤太咸，{dish}喝不下去', '排队二十分钟，{dish}不太值当', '浇头少得可怜，{dish}里就几片肉', '{dish}的面软了，口感不好'],
    1: ['面是坨的，{dish}的汤也是凉的', '太咸了，{dish}齁得慌', '等了半小时，态度还不好', '{dish}有股碱水味，很难吃', '失望，{dish}不如出去吃']
  },
  {
    5: ['食材新鲜，{dish}随便点都不踩雷', '辣得过瘾，{dish}点微辣刚好', '老板给料实在，{dish}称重也公道', '三十多种食材，{dish}每次换着来', '和同学拼一锅{dish}，人均很划算', '{dish}荤素搭配称重，吃多少拿多少'],
    4: ['好吃，{dish}就是稍微有点咸', '油大了一点，但{dish}确实香', '食材新鲜，就是{dish}价格略高', '味道很正，就是点{dish}排队久', '希望{dish}的素菜种类再多一点'],
    3: ['一般般，{dish}和外面差不多', '有点咸，{dish}盖住了食材本味', '价格偏高，{dish}随便夹夹就二三十', '油太多，吃完{dish}有点腻', '{dish}不难吃，但也没什么记忆点'],
    2: ['太咸了，{dish}齁得难受', '油大，吃完{dish}胃不舒服', '称重不太透明，{dish}的价差挺多', '食材蔫了，{dish}像在冰柜里放久了', '辣度失控，{dish}中辣比别家特辣还辣'],
    1: ['齁咸还油，{dish}真吃不下', '食材明显不新鲜，{dish}叶子都黄了', '性价比很低，{dish}随手拿点就三十多', '吃完{dish}肚子不舒服', '又咸又油，{dish}很失望']
  },
  {
    5: ['羊肉正宗，{dish}一点膻味都没有', '{dish}分量足，两个人点一份就够', '肉串是炭火烤的，{dish}很香', '牛肉给得实在，{dish}能吃饱', '有独立操作间，{dish}吃着放心', '{dish}的汤底浓郁，配馍刚好'],
    4: ['{dish}好吃，就是串有点小', '{dish}不错，土豆给得少了', '味道好，就是{dish}价格稍贵', '肉实在，{dish}就是油大了点', '好吃，为{dish}排队也算值得'],
    3: ['一般，{dish}的牛肉有点老', '味道还行，{dish}分量偏少', '羊肉有点膻，今天{dish}状态不好', '{dish}价格比别的窗口高一截', '{dish}中规中矩'],
    2: ['肉有点老，{dish}嚼着费劲', '价格贵，{dish}量还少', '汤比较油，{dish}喝不下去', '今天{dish}膻味重', '{dish}出餐慢，等太久'],
    1: ['肉又老又柴，{dish}难吃', '又贵又少，{dish}不推荐', '膻味太重，{dish}吃不下', '这次{dish}明显不新鲜', '体验很差，{dish}不会再点']
  },
  {
    5: ['减脂期有个去处挺好的，{dish}不错', '{dish}食材很新鲜，分量也可以', '牛油果熟得刚好，{dish}好吃', '热量标得清楚，吃{dish}很安心', '{dish}清爽不油腻，适合夏天'],
    4: ['健康是健康，就是{dish}有点贵', '{dish}很新鲜，酱汁给得少了', '鸡胸有点柴，{dish}其他都还行', '低卡确实没负担，{dish}味道一般', '食材新鲜，希望{dish}能出个大份'],
    3: ['贵，而且{dish}真的吃不饱', '味道寡淡，{dish}全靠酱汁撑', '蔬菜还行，{dish}蛋白质给得太少', '减脂可以，{dish}当正餐不行', '{dish}一般，性价比不高'],
    2: ['一份二十多，吃完{dish}还是饿', '蔬菜有点蔫，{dish}不太新鲜', '酱汁太少，{dish}干巴巴的', '{dish}的鸡胸又柴又没味道', '吃不饱，吃完{dish}还得再买主食'],
    1: ['又贵又少，吃完{dish}跟没吃一样', '蔬菜黄了，{dish}明显不新鲜', '难吃还贵，{dish}不如吃盒饭', '三十块就几片叶子，{dish}不值', '这次{dish}的牛油果是坏的']
  },
  {
    5: ['{dish}皮薄馅足，做工很细', '{dish}会爆浆，趁热吃很香', '现蒸的，{dish}端上来还烫嘴', '{dish}软糯，一抿就脱骨', '早市来一份{dish}很舒服', '{dish}是米浆现蒸的，口感很滑'],
    4: ['{dish}好吃，就是四个太少了', '{dish}不错，稍微甜了点', '现蒸要等，不过{dish}值得等', '{dish}很入味，就是偏咸', '整体还行，{dish}价格小贵'],
    3: ['还行，但{dish}可选的品类太少了', '下午去基本没什么剩的，买{dish}得赶早', '{dish}价格偏贵，一份吃不饱', '味道一般，{dish}比不上外面茶楼', '{dish}中等水平'],
    2: ['下午三点去，{dish}早就卖完了', '蒸过头了，{dish}的皮都烂了', '价格贵，{dish}量还少', '等了十五分钟，{dish}上来是凉的', '{dish}明显是半成品加热的'],
    1: ['又贵又少，{dish}完全不值', '是凉的，{dish}像放了很久', '难吃，{dish}皮厚馅少', '经常缺货，为了{dish}白跑一趟', '踩雷，{dish}不会再买']
  },
  {
    5: ['{dish}用料实在，口味很正', '{dish}奶味很浓，做得很正宗', '珍珠煮得Q弹，{dish}也好喝', '夏天来一杯{dish}很爽', '甜度可以调，{dish}对控糖的友好'],
    4: ['{dish}不错，就是冰有点多', '{dish}好喝，稍甜了些', '{dish}挺好，就是份量小', '水果新鲜，{dish}价格稍高', '总体可以，希望{dish}多出无糖款'],
    3: ['{dish}是普通奶茶的水平', '有点甜，{dish}喝几口就腻', '价格小贵，{dish}跟外面差不多', '{dish}一般，没什么亮点', '{dish}不难喝，也没什么特别'],
    2: ['太甜了，{dish}齁得慌', '珍珠是硬的，{dish}明显没煮透', '冰块占了半杯，{dish}没多少', '香精味比较重，{dish}喝着不太行', '水果不太新鲜，{dish}有点酸'],
    1: ['甜得发腻，{dish}喝不下去', '明显是糖浆兑的，{dish}很难喝', '珍珠又硬又没味，{dish}踩雷', '水果馊了，{dish}明显放久了', '太差了，{dish}不推荐']
  },
  {
    5: ['{dish}砂锅现煲，锅巴很香', '底下那层锅巴刮起来特别过瘾', '{dish}料很足，肉铺满一层', '等十几分钟值得，{dish}确实是现煲的', '冬天吃一份{dish}全身都暖', '{dish}配的例汤也不糊弄'],
    4: ['{dish}好吃，就是要等十来分钟', '锅巴很脆，就是{dish}稍微咸了点', '{dish}分量够，价格稍高', '味道不错，希望{dish}多几种口味', '现煲的{dish}，就是高峰期太慢'],
    3: ['一般，{dish}没有想象中惊艳', '{dish}肉有点少，基本在吃米饭', '价格偏贵，{dish}偶尔吃还行', '今天{dish}的锅巴有点糊', '{dish}中规中矩'],
    2: ['等了二十分钟，{dish}上来是温的', '偏咸，{dish}配的米饭还不够', '肉有点腥，{dish}不太行', '价格贵，{dish}量一般', '{dish}底部糊得厉害，发苦'],
    1: ['又贵又慢，{dish}不推荐', '{dish}的肉有异味，明显不新鲜', '等了半小时还上错，体验很差', '{dish}糊了，根本吃不了', '踩雷，{dish}不会再点']
  },
  {
    5: ['{dish}是现包的，皮薄馅大', '一口咬下去{dish}还有汤汁', '食堂里{dish}的水平能排前三', '蘸点醋和辣椒油，{dish}绝了', '{dish}分量足，一份就管饱', '师傅包得快，{dish}端上来还热乎'],
    4: ['{dish}不错，就是煮得稍微软了', '好吃，就是{dish}高峰期要等', '馅很香，{dish}的皮稍厚了点', '性价比高，{dish}我常点', '{dish}味道好，希望多几种馅'],
    3: ['还行，但{dish}有股速冻的口感', '{dish}皮有点厚，馅偏少', '味道一般，{dish}没什么特别的', '价格还行，{dish}份量一般', '{dish}中规中矩'],
    2: ['{dish}煮破了，一盘都是碎的', '馅有点腥，{dish}不太新鲜', '{dish}皮厚馅少，一口全是面', '等太久，{dish}上来都坨了', '{dish}偏咸，蘸料也一般'],
    1: ['{dish}明显是冻了很久的，难吃', '皮黏馅馊，{dish}没法吃', '等了半小时，{dish}还上错了', '太咸了，{dish}吃不下', '踩雷，{dish}不会再买']
  },
  {
    5: ['{dish}出餐快，赶时间首选', '米饭还能免费加，{dish}很实在', '菜品种类多，{dish}每天换着来', '{dish}价格便宜，学生党友好', '阿姨打菜手不抖，{dish}给得挺多', '赶课的时候就来一份{dish}'],
    4: ['{dish}方便，就是菜有点凉了', '性价比高，{dish}味道稍淡', '{dish}不错，高峰期排队也快', '能吃饱，{dish}味道再重点就好了', '{dish}挺实惠，就是油稍微大了'],
    3: ['{dish}就是普通大锅菜的水平', '菜有点凉，{dish}像是提前炒好的', '味道一般，图个方便', '{dish}油有点大，吃着腻', '{dish}不难吃，也没什么特色'],
    2: ['{dish}的菜是凉的，吃着不舒服', '油太大，吃完{dish}腻得慌', '阿姨手抖，{dish}给得太少', '{dish}肉很少，基本都是素菜', '菜有点馊，{dish}不太新鲜'],
    1: ['{dish}的菜明显是剩的，凉透了', '太油太咸，{dish}难以下咽', '在{dish}里吃出异物，体验极差', '又贵又难吃，{dish}不值', '失望，{dish}不会再吃']
  },
  {
    5: ['{dish}铁板现煎，上桌还滋滋响', '黑椒味很浓，{dish}做得到位', '{dish}肉给得多，能吃饱', '铁板保温好，{dish}吃到最后一都热乎', '{dish}配的例汤和泡菜也不错'],
    4: ['{dish}好吃，就是油有点大', '味道不错，{dish}价格稍贵', '{dish}分量还行，就是排队久了点', '铁板很香，就是{dish}稍微咸了', '总体满意，希望{dish}出个小份'],
    3: ['一般，{dish}价格偏贵了', '{dish}油太大，吃几口就腻', '味道普通，不如隔壁窗口', '{dish}的肉有点老，嚼着费劲', '{dish}中规中矩，偶尔吃'],
    2: ['{dish}太油了，底下全是油', '肉有点腥，{dish}不太新鲜', '价格贵，{dish}量还少', '等了二十分钟，{dish}上来是温的', '{dish}偏咸，配饭都压不住'],
    1: ['又贵又油，{dish}很难吃', '{dish}的肉有怪味，明显不新鲜', '等了半小时，{dish}还是夹生的', '油得发腻，{dish}吃不下', '踩雷，{dish}不会再点']
  }
];

// 按目标比例配额分配星级，先对比例做小幅抖动，保证档口均分稳定又不至于每道菜完全一样
function allocRatings(n, dist) {
  const jitter = dist.map(p => Math.max(0.01, p * (0.85 + rnd() * 0.3)));
  const sum = jitter.reduce((a, b) => a + b, 0);
  const raw = jitter.map(p => p / sum * n);
  const counts = raw.map(Math.floor);
  const order = raw.map((v, i) => [v - counts[i], i]).sort((a, b) => b[0] - a[0]);
  let rest = n - counts.reduce((a, b) => a + b, 0);
  for (let i = 0; rest > 0; i++) {
    counts[order[i % order.length][1]]++;
    rest--;
  }
  const ratings = [];
  counts.forEach((c, i) => { for (let k = 0; k < c; k++) ratings.push(5 - i); });
  for (let i = ratings.length - 1; i > 0; i--) {
    const j = randInt(0, i);
    const t = ratings[i]; ratings[i] = ratings[j]; ratings[j] = t;
  }
  return ratings;
}

// 评价集中在饭点前后，少量出现在白天其他时段
function mealDateStr(daysAgo) {
  const r = rnd();
  let h;
  if (r < 0.30) h = 11;
  else if (r < 0.56) h = 12;
  else if (r < 0.74) h = 17;
  else if (r < 0.90) h = 18;
  else h = pick([10, 13, 16, 19, 20]);
  return dateStr(daysAgo, h, randInt(0, 59));
}

const lines = [];
lines.push('-- 校园食堂菜品反馈评价系统 - 初始化数据（自动生成）');
lines.push('USE canteen_feedback;');
lines.push('');
lines.push('SET FOREIGN_KEY_CHECKS = 0;');
lines.push("SET @t := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_order_item');");
lines.push("SET @sql := IF(@t > 0, 'TRUNCATE TABLE dining_order_item', 'SELECT 1');");
lines.push('PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;');
lines.push("SET @t := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_order');");
lines.push("SET @sql := IF(@t > 0, 'TRUNCATE TABLE dining_order', 'SELECT 1');");
lines.push('PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;');
lines.push('TRUNCATE TABLE login_log;');
lines.push('TRUNCATE TABLE dish_favorite;');
lines.push('TRUNCATE TABLE feedback;');
lines.push('TRUNCATE TABLE dish_review;');
lines.push('TRUNCATE TABLE dish;');
lines.push('TRUNCATE TABLE canteen_stall;');
lines.push('TRUNCATE TABLE sys_notice;');
lines.push('TRUNCATE TABLE sys_user;');
lines.push('SET FOREIGN_KEY_CHECKS = 1;');
lines.push('');

lines.push(`-- 1. 用户（管理员 admin/admin123，学生 student01~${STUDENT_COUNT} / 123456）`);
lines.push('INSERT INTO sys_user (username, password, real_name, role, student_no, phone, status) VALUES');
lines.push("('admin', '$2b$10$zkDS.twH10LGpOg230pUMuJwyaUkhADV.2CXmpIGcdMOMf7Lk2JXC', '张明远', 'ADMIN', NULL, '" + genPhone() + "', 1),");
lines.push("('manager', '$2b$10$zkDS.twH10LGpOg230pUMuJwyaUkhADV.2CXmpIGcdMOMf7Lk2JXC', '李婉清', 'ADMIN', NULL, '" + genPhone() + "', 1),");
const stuVals = students.map(s =>
  `('${s.username}', '$2b$10$D1nntRy5JPpK9X3GPQZQLexO3f6sb7aZN1ptEDcJ.fjjFnkTnkQEa', '${esc(s.realName)}', 'STUDENT', '${s.studentNo}', '${s.phone}', 1)`);
lines.push(stuVals.join(',\n') + ';');
lines.push('');

lines.push('-- 2. 档口（hygiene_score 为学校评定的卫生分）');
lines.push('INSERT INTO canteen_stall (name, window_no, location, category, manager_name, manager_phone, description, hygiene_score, status) VALUES');
lines.push(stalls.map(s =>
  `('${s.name}', '${s.windowNo}', '${s.location}', '${s.category}', '${s.manager}', '${s.phone}', '${esc(s.desc)}', ${s.hygiene.toFixed(1)}, 1)`
).join(',\n') + ';');
lines.push('');

lines.push('-- 3. 菜品');
lines.push('INSERT INTO dish (stall_id, name, price, category, image, description, featured, status) VALUES');
const dishRows = [];
const dishOwner = [];
const dishNames = [];
let dishIndex = 0;
for (let s = 0; s < stalls.length; s++) {
  for (let k = 0; k < perStall[s]; k++) {
    const [name, price, cat, desc, featured] = dishDefs[dishIndex];
    dishOwner.push(s);
    dishNames.push(name);
    dishRows.push(`(${s + 1}, '${esc(name)}', ${price.toFixed(2)}, '${cat}', '', '${esc(desc)}', ${featured}, 1)`);
    dishIndex++;
  }
}
lines.push(dishRows.join(',\n') + ';');
lines.push('');

lines.push('-- 4. 菜品评价（按档口口碑分层，含好评爆棚与口碑垫底的档口）');
lines.push('INSERT INTO dish_review (dish_id, user_id, rating, content, anonymous, status, create_time) VALUES');
const reviewRows = [];
const totalDishes = dishIndex;
const dishAvg = [];
// 每个档口挑 1 道近期新上架的菜，只有零星几条评价，用于红黑榜的「新菜品」散点
const newDishSet = new Set();
let idxCursor = 0;
for (let s = 0; s < stalls.length; s++) {
  newDishSet.add(idxCursor + randInt(1, perStall[s]));
  idxCursor += perStall[s];
}
for (let d = 1; d <= totalDishes; d++) {
  const sIdx = dishOwner[d - 1];
  const profile = stallProfile[sIdx];
  const n = newDishSet.has(d)
    ? randInt(1, 5)
    : Math.max(6, Math.min(28, Math.round(profile.heat * (0.8 + rnd() * 0.4))));
  const used = new Set();
  const usedText = new Set();
  const ratings = allocRatings(n, profile.dist);
  for (const rating of ratings) {
    let uid = randStudentId();
    let guard = 0;
    while (used.has(uid) && guard++ < 40) uid = randStudentId();
    if (used.has(uid)) continue;
    used.add(uid);
    // 差评的人更愿意写原因，好评多数只打分
    const emptyRate = rating >= 4 ? 0.38 : rating === 3 ? 0.28 : 0.12;
    let content = '';
    if (rnd() >= emptyRate) {
      const pool = COMMENTS[sIdx][rating];
      let tpl = pick(pool);
      for (let t = 0; t < 4 && usedText.has(tpl); t++) tpl = pick(pool);
      usedText.add(tpl);
      content = tpl.replace('{dish}', dishNames[d - 1]);
    }
    const anon = rnd() < 0.22 ? 1 : 0;
    reviewRows.push(`(${d}, ${uid}, ${rating}, '${esc(content)}', ${anon}, 1, '${mealDateStr(randInt(0, 89))}')`);
  }
  dishAvg.push(ratings.length ? ratings.reduce((a, b) => a + b, 0) / ratings.length : 0);
}
lines.push(reviewRows.join(',\n') + ';');
lines.push('');

lines.push('-- 5. 菜品收藏（偏向高分菜品）');
lines.push('INSERT INTO dish_favorite (user_id, dish_id, create_time) VALUES');
const favRows = [];
const favSet = new Set();
for (let i = 0; i < 260 && favRows.length < 190; i++) {
  const did = randInt(1, totalDishes);
  if (rnd() > Math.min(0.95, 0.12 + dishAvg[did - 1] * 0.14)) continue;
  const uid = randStudentId();
  const key = uid + '-' + did;
  if (favSet.has(key)) continue;
  favSet.add(key);
  favRows.push(`(${uid}, ${did}, '${dateStr(randInt(0, 60), randInt(9, 21), randInt(0, 59))}')`);
}
lines.push(favRows.join(',\n') + ';');
lines.push('');

lines.push('-- 6. 留言反馈');
lines.push('INSERT INTO feedback (user_id, stall_id, title, content, type, status, reply_content, reply_user_id, reply_time, create_time) VALUES');
// [学生序号, 档口ID, 标题, 内容, 类型, 状态, 回复, 回复管理员ID]
const fbs = [
  [3, 1, '希望增加不辣的川菜选项', '川味小炒整体偏辣，建议每天固定出两个不辣的菜，照顾不吃辣的同学。', 'SUGGEST', 'RESOLVED', '同学你好，已与档口沟通，自本周起每日供应 2 道不辣菜品（清炒时蔬、番茄炒蛋轮换），欢迎继续反馈。', 1],
  [7, 2, '叉烧饭分量偏少', '男生吃一份叉烧饭不太够，希望能加一个加饭选项，价格另算也可以接受。', 'SUGGEST', 'PROCESSING', '已收到。档口正在评估加饭方案，预计下周上线「加饭 +1 元」选项。', 1],
  [11, 4, '麻辣香锅称重计价不透明', '同样的菜有时候价格差挺多，希望能在窗口把单价公示出来。', 'COMPLAINT', 'RESOLVED', '感谢反馈。已在窗口张贴「荤菜 5.6 元/100g、素菜 2.8 元/100g」价目表，并加装了电子显示屏实时称重。', 2],
  [15, 3, '牛肉面汤底很好喝', '汤底确实是每天现熬的，味道很正，希望一直保持这个水准。', 'PRAISE', 'RESOLVED', '谢谢你的认可，我们会继续坚持每日现熬汤底、手工拉面。', 1],
  [19, 5, '清真窗口排队时间太长', '中午 12 点排队要二十分钟以上，建议增加出餐口或者提前备餐。', 'COMPLAINT', 'PROCESSING', '已协调在 11:30-12:30 高峰期增开一个临时出餐口，缓解排队压力。', 2],
  [23, 6, '轻食窗口价格偏高且吃不饱', '一份沙拉二十多，吃完还得再去买个主食，学生党实在消费不起。建议出小份或者搭配套餐。', 'COMPLAINT', 'PROCESSING', '已约谈档口负责人，要求本周内推出「半份 + 主食」的学生套餐，价格控制在 15 元以内。', 1],
  [5, 6, '沙拉里的蔬菜不太新鲜', '昨天买的三文鱼沙拉，生菜边都发黄了，希望能加强食材轮换。', 'COMPLAINT', 'RESOLVED', '非常抱歉。已要求档口当日未售完的叶菜全部报损处理，并改为每日两次配送。', 2],
  [9, 7, '虾饺供应时间太短', '经常下午去就没有虾饺了，建议延长供应时间或者增加备货量。', 'COMPLAINT', 'PENDING', null, null],
  [13, 8, '奶茶冰块太多', '一杯三分之二是冰，喝两口就没了，希望可以默认少冰。', 'COMPLAINT', 'PROCESSING', '已要求档口将默认冰量改为「少冰」，需要正常冰的同学可以主动说明。', 2],
  [17, 2, '餐具不够干净', '昨天拿到的一次性筷子有毛刺，希望能换一批质量好点的。', 'COMPLAINT', 'RESOLVED', '已更换餐具供应商，并加强开箱抽检，给你带来不便非常抱歉。', 1],
  [21, 1, '炒菜师傅炒得很好', '每次去都是现炒，锅气很足，比周边的小炒店还好吃。', 'PRAISE', 'PENDING', null, null],
  [27, 5, '羊肉泡馍希望配糖蒜', '少了糖蒜感觉不完整，建议免费提供一份。', 'SUGGEST', 'PENDING', null, null],
  [2, 4, '麻辣香锅的素菜种类太少', '素菜就那几样，希望能加个菌菇类，比如金针菇、杏鲍菇。', 'SUGGEST', 'PENDING', null, null],
  [6, 3, '希望能加个凉面', '夏天想吃凉面，面食工坊可以考虑加个凉面品种。', 'SUGGEST', 'PENDING', null, null],
  [10, 7, '港式茶点价格偏贵', '四个虾饺十八块，作为食堂价格有点高了，建议出个小份。', 'COMPLAINT', 'PENDING', null, null],
  [14, 8, '杨枝甘露甜度可以调整吗', '希望可以选三分糖，现在默认全糖对控糖的人不太友好。', 'SUGGEST', 'RESOLVED', '已支持在点单时选择「全糖 / 七分糖 / 半糖 / 无糖」，下单时告知即可。', 2],
  [18, 6, '轻食窗口分量标注不清晰', '包装上只写了热量，希望能把蛋白质和碳水也标上，方便计算。', 'SUGGEST', 'PENDING', null, null],
  [22, 5, '大盘鸡分量很足，好评', '两个人点一份就够了，皮带面还免费加，性价比挺高。', 'PRAISE', 'PENDING', null, null],
  [26, 1, '川味小炒高峰期出餐有点慢', '12 点去要等十来分钟，不过现炒的可以理解，希望能加个叫号。', 'SUGGEST', 'PENDING', null, null],
  [30, 7, '建议茶点窗口延长到晚市', '下午 5 点以后三楼基本没什么吃的了，希望能供应到 6 点半。', 'SUGGEST', 'PENDING', null, null],
  [4, 2, '烧鸭饭的鸭子皮不脆了', '最近几次买到手都是软的，是不是提前切好放太久了？', 'COMPLAINT', 'PENDING', null, null],
  [8, 4, '建议麻辣香锅出一个单人份', '小份一个人吃有点多，大份又吃不完，希望中间有个规格。', 'SUGGEST', 'PENDING', null, null],
  [12, 8, '珍珠经常是硬的', '是不是煮好放太久了？希望能现煮现卖。', 'COMPLAINT', 'PENDING', null, null],
  [16, 3, '面食工坊的汤底变淡了', '这学期的汤感觉没上学期浓郁了，是不是换配方了？', 'COMPLAINT', 'PENDING', null, null],
  [20, 1, '希望川味小炒支持少油', '油确实大，吃完碗底一层，希望可以要求少油。', 'SUGGEST', 'RESOLVED', '已支持在点单时说明「少油 / 少盐」，师傅会单独炒制。', 1],
  [1, 10, '饺子窗口排队时间太长了', '中午下课过去要排二十分钟，饺子现包可以理解，但建议提前包一批备着。', 'COMPLAINT', 'PROCESSING', '已协调档口在 11:00 前预制 200 份常用馅的饺子，高峰期出餐速度预计提升一半。', 1],
  [24, 9, '煲仔饭等太久，上课要迟到', '现煲确实香，但二十分钟真的等不起，建议中午高峰期提前煲一批。', 'SUGGEST', 'PROCESSING', '已要求档口在 11:20 前预煲腊味、滑鸡两款各 30 份，到店即可取。', 2],
  [28, 12, '铁板烧饭价格偏高', '一份二十，对学生来说有点贵，而且油太大了，希望能出个小份。', 'COMPLAINT', 'PENDING', null, null],
  [16, 11, '自选快餐的菜经常是凉的', '大锅菜提前炒好放着，晚去就凉了，希望能加装保温台。', 'COMPLAINT', 'RESOLVED', '已为自选快餐加装 6 组恒温保温台，菜品温度维持在 60℃ 以上。', 1],
  [29, 10, '三鲜水饺很好吃，希望增加供应', '虾仁给得实在，晚上去经常卖完了，建议增加备货。', 'PRAISE', 'PENDING', null, null],
  [25, 12, '铁板烧饭的肉有点腥', '上周吃了两次，牛肉都有点腥味，希望加强食材验收。', 'COMPLAINT', 'PENDING', null, null],
  [2, 9, '煲仔饭锅巴糊了', '这次底部的锅巴全糊了，发苦，希望注意火候。', 'COMPLAINT', 'PENDING', null, null],
  [26, 11, '红烧肉做得很好，希望天天有', '肥而不腻，价格也实惠，就是经常卖完了。', 'PRAISE', 'PENDING', null, null],
  [33, 12, '铁板烧饭窗口有油污，卫生堪忧', '每次路过都能看到灶台边一层黑油，地面也黏脚，希望后勤查一下卫生评级。', 'COMPLAINT', 'PROCESSING', '已现场核查，该档口卫生分已降至 72.5 分（不合格），责令 3 日内完成灶台与排烟系统深度清洁，复检合格后方可继续营业。', 1],
  [41, 8, '饮品窗口的冰块存放不规范', '看到工作人员直接用手铲冰，希望能配专用冰铲和手套。', 'COMPLAINT', 'RESOLVED', '已为档口配发专用冰铲、一次性手套与口罩，并要求员工操作时全程佩戴。', 2],
  [37, 7, '蒸笼看起来很久没换了', '蒸笼边缘发黑，希望定期更换或深度清洗。', 'COMPLAINT', 'PROCESSING', '已要求档口每周更换一次笼布，蒸笼每两周深度清洁一次并记录台账。', 1],
  [45, 11, '自选快餐的保温台温度不够', '晚上七点去的，菜基本是凉的，保温台好像没通电。', 'COMPLAINT', 'PENDING', null, null],
  [52, 4, '麻辣香锅地面太滑', '油溅到地上没人拖，上次差点滑倒，建议增加防滑垫和清洁频次。', 'COMPLAINT', 'RESOLVED', '已在操作区铺设防滑垫，并将地面清洁频次调整为每 30 分钟一次。', 2],
  [34, 10, '饺子窗口的擀面台很干净', '明档能看到整个包制过程，案板和手都很干净，吃着放心。', 'PRAISE', 'RESOLVED', '感谢认可，我们会继续保持明档操作与每日消毒。', 1],
  [58, 6, '轻食窗口的食材保鲜做得好', '每份都是现做，能看到冷藏柜温度显示，生菜也很新鲜。', 'PRAISE', 'PENDING', null, null],
  [49, 9, '煲仔饭砂锅有裂缝', '看到好几个砂锅边上有缺口，长期用会不会有安全隐患？', 'COMPLAINT', 'PROCESSING', '已排查并淘汰 12 只有裂缝的砂锅，新锅已到位，后续每月排查一次。', 2],
  [31, 3, '面食工坊的筷子消毒柜没开', '消毒柜指示灯是灭的，筷子摸起来还有点湿。', 'COMPLAINT', 'RESOLVED', '经查为插座接触不良，已修复并恢复 24 小时消毒，每日由专人检查记录。', 1],
  [55, 2, '烧腊窗口的挂炉很干净', '明档挂着，能看见师傅戴手套口罩操作，比其他窗口规范。', 'PRAISE', 'PENDING', null, null],
  [47, 5, '清真窗口的砧板生熟不分', '看到切生肉和切熟食用同一块板，希望能分开。', 'COMPLAINT', 'PROCESSING', '已要求档口配置生、熟、蔬三色砧板并张贴标识，禁止混用。', 1],
  [60, 11, '希望公示各窗口卫生检查结果', '建议像餐厅一样贴个「笑脸/平脸/哭脸」，我们选窗口也有个参考。', 'SUGGEST', 'RESOLVED', '采纳建议。已在各窗口张贴食品安全等级公示牌，同时系统内也已上线「卫生分」查询。', 2]
];
const fbRows = fbs.map(f => {
  const [uid, sid, title, content, type, status, reply, replyUid] = f;
  return [ADMIN_COUNT + uid, sid, title, content, type, status, reply, replyUid];
}).map(([uid, sid, title, content, type, status, reply, replyUid]) => {
  const rt = reply ? `'${dateStr(randInt(0, 5), randInt(9, 18), randInt(0, 59))}'` : 'NULL';
  return `(${uid}, ${sid}, '${esc(title)}', '${esc(content)}', '${type}', '${status}', ${reply ? "'" + esc(reply) + "'" : 'NULL'}, ${replyUid || 'NULL'}, ${rt}, '${dateStr(randInt(0, 40), randInt(8, 21), randInt(0, 59))}')`;
});
lines.push(fbRows.join(',\n') + ';');
lines.push('');

lines.push('-- 7. 公告');
lines.push('INSERT INTO sys_notice (title, content, publisher_id, status) VALUES');
lines.push("('关于二号窗口设备检修的通知', '二号窗口（粤式烧腊）因烤炉设备例行检修，10 月 25 日暂停营业一天，10 月 26 日恢复正常供应。请同学们提前做好就餐安排，给您带来不便敬请谅解。', 1, 1),");
lines.push("('关于轻食沙拉窗口整改的通报', '针对近期收到的价格偏高、食材不新鲜等反馈，后勤处已约谈该档口负责人，要求：一、本周内推出 15 元以内的学生套餐；二、当日未售完叶菜一律报损；三、包装上完整标注热量、蛋白质与碳水。整改情况将在两周后公示。', 1, 1),");
lines.push("('关于三楼茶点、饮品窗口延长营业的通知', '自 11 月 1 日起，三楼东侧（港式茶点）与三楼西侧（饮品甜品）营业时间延长至 18:30，下午课程结束后也可以正常就餐。', 2, 1),");
lines.push("('新增「菜品评价」功能上线啦', '为提升食堂服务质量，校园食堂菜品反馈评价系统正式上线。同学们可以对菜品打分、写评价、提建议，我们会认真对待每一条反馈。', 1, 1),");
lines.push("('关于制止餐饮浪费的倡议', '请同学们按需取餐，践行光盘行动。食堂各窗口均提供「小份菜」选项，欢迎选择。', 2, 1);");
lines.push('');

lines.push('-- 8. 登录日志');
lines.push('INSERT INTO login_log (username, real_name, role, ip, status, login_time) VALUES');
const logRows = [];
for (let i = 0; i < 96; i++) {
  const s = students[randInt(0, STUDENT_COUNT - 1)];
  const ok = rnd() < 0.9 ? 1 : 0;
  const ip = `10.${randInt(16, 28)}.${randInt(0, 255)}.${randInt(2, 250)}`;
  logRows.push(`('${s.username}', '${esc(s.realName)}', 'STUDENT', '${ip}', ${ok}, '${mealDateStr(randInt(0, 29))}')`);
}
for (let i = 0; i < 6; i++) {
  logRows.push(`('admin', '张明远', 'ADMIN', '10.16.8.${randInt(2, 60)}', 1, '${dateStr(randInt(0, 29), randInt(8, 20), randInt(0, 59))}')`);
}
lines.push(logRows.sort().join(',\n') + ';');

const OUT = path.join(__dirname, '..', 'init-data.sql');
fs.writeFileSync(OUT, lines.join('\n'), 'utf8');

const totalReviews = reviewRows.length;
const goodReviews = reviewRows.filter(r => {
  const m = r.match(/^\(\d+, \d+, (\d),/);
  return m && Number(m[1]) >= 4;
}).length;
console.log('dishes=' + totalDishes + ' reviews=' + totalReviews + ' favorites=' + favRows.length
  + ' feedbacks=' + fbRows.length + ' logs=' + logRows.length
  + ' goodRate=' + (goodReviews / totalReviews * 100).toFixed(1) + '%');
for (let s = 0; s < stallProfile.length; s++) {
  const idx = [];
  for (let d = 0; d < totalDishes; d++) if (dishOwner[d] === s) idx.push(d);
  const avgs = idx.map(d => dishAvg[d]);
  const mean = avgs.reduce((a, b) => a + b, 0) / avgs.length;
  console.log('  ' + stalls[s].name + ' 均分=' + mean.toFixed(2) + ' 负责人=' + stalls[s].manager + ' ' + stalls[s].phone);
}
