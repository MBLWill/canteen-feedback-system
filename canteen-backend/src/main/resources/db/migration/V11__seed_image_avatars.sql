-- ============================================================
-- V11 回填图片素材路径：学生头像 / 管理员头像 / 商家 logo / 档口封面
--
-- 背景：db/init-data.sql、db/full-setup.sql 只写了菜品图（/dishes/），
--       没有写 sys_user.avatar、canteen_stall.cover_image，
--       导致「别人 clone 后全新导入数据库」时，学生头像、商家 logo、
--       档口封面全部为空（前端只能显示首字母占位）。
--       图片文件本身已随 canteen-frontend/public/ 一起提交到仓库，
--       这里只负责把数据库里的路径字段补上。
--
-- 规则（与 scripts/generate_data.py 的头像分配规则一致）：
--   · 学生 student01..student60 -> /ui/avatars/10004..10063
--     编号 = 10003 + 学号序号；其中 10036/10037/10038 为 .png，其余 .jpg
--   · 管理员 admin -> 10002.jpg，manager -> 10003.jpg
--   · 商家 logo -> /ui/logo/stall-{stall_id}.webp
--   · 档口封面 -> /ui/stall/stall-{id}.jpg
--
-- 幂等：仅在字段为空时回填，重复执行无副作用（不影响用户自行上传的头像）。
-- ============================================================

-- 1) 学生头像
UPDATE sys_user
SET avatar = CONCAT('/ui/avatars/', 10003 + CAST(SUBSTRING(username, 8) AS UNSIGNED),
    CASE WHEN (10003 + CAST(SUBSTRING(username, 8) AS UNSIGNED)) IN (10036, 10037, 10038)
         THEN '.png' ELSE '.jpg' END)
WHERE role = 'STUDENT'
  AND username REGEXP '^student[0-9]{2}$'
  AND (avatar IS NULL OR avatar = '');

-- 2) 管理员 / 食堂经理头像
UPDATE sys_user
SET avatar = '/ui/avatars/10002.jpg'
WHERE username = 'admin' AND (avatar IS NULL OR avatar = '');

UPDATE sys_user
SET avatar = '/ui/avatars/10003.jpg'
WHERE username = 'manager' AND (avatar IS NULL OR avatar = '');

-- 3) 商家 logo（1~20 号档口均有对应 logo 文件）
UPDATE sys_user
SET avatar = CONCAT('/ui/logo/stall-', stall_id, '.webp')
WHERE role = 'MERCHANT'
  AND stall_id BETWEEN 1 AND 20
  AND (avatar IS NULL OR avatar = '');

-- 4) 档口封面（1~20 号档口均有对应封面文件）
UPDATE canteen_stall
SET cover_image = CONCAT('/ui/stall/stall-', id, '.jpg')
WHERE id BETWEEN 1 AND 20
  AND (cover_image IS NULL OR cover_image = '');
