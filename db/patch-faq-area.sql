-- 智能体知识库：食堂区域/楼层说明（可重复执行）
USE canteen_feedback;

-- 1. 新增「区域楼层」FAQ（已存在则跳过）
INSERT INTO sys_faq (category, question, answer, keywords, sort_no, status)
SELECT 'RULE', '食堂分几个区域几层楼',
       '食堂分南北两区：南区食堂有两层（1 楼、2 楼），北区食堂只有一层。南区 1 楼有川味小炒、粤式烧腊、面食工坊；南区 2 楼有麻辣香锅、清真风味、轻食沙拉；其余档口（港式茶点、饮品甜品、煲仔饭、东北饺子、自选快餐、铁板烧饭）都在北区。',
       '区域,南区,北区,几楼,楼层,食堂,在哪层,几层,分布,二楼,一楼',
       0, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_faq WHERE question = '食堂分几个区域几层楼' AND deleted = 0);

-- 2. 清真窗口位置迁到南区 2 楼
UPDATE sys_faq
SET answer = '清真风味在南区 2 楼西侧 05 号窗口，设有独立操作间，提供牛羊肉专区菜品，不使用猪肉及其制品。',
    keywords = '清真,清真窗口,回族,牛肉,羊肉,在哪,南区,二楼'
WHERE question = '清真窗口在哪里' AND deleted = 0;

SELECT '智能体区域楼层知识已同步' AS result;
