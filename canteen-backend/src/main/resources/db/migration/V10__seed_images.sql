-- 演示图片数据补齐：档口门面图 / 学生头像 / 商家头像
--
-- 背景：GitHub 上的数据导出只含表结构与业务数据，头像与门面图的路径是作者本地跑
-- scripts/generate_data.py、scripts/update_logo_stall.sql 之后才写进库的，没有一起提交。
-- 于是别人 clone 下来导入数据后，档口封面、学生头像全是空值，页面只能显示渐变色/首字占位。
--
-- 这里按仓库里已有的静态资源回填，全部为幂等 UPDATE，重复执行不会覆盖用户自己换过的头像。

-- 1) 档口门面图：/ui/stall/stall-<id>.jpg
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-1.jpg' WHERE id = 1 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-2.jpg' WHERE id = 2 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-3.jpg' WHERE id = 3 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-4.jpg' WHERE id = 4 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-5.jpg' WHERE id = 5 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-6.jpg' WHERE id = 6 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-7.jpg' WHERE id = 7 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-8.jpg' WHERE id = 8 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-9.jpg' WHERE id = 9 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-10.jpg' WHERE id = 10 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-11.jpg' WHERE id = 11 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-12.jpg' WHERE id = 12 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-13.jpg' WHERE id = 13 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-14.jpg' WHERE id = 14 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-15.jpg' WHERE id = 15 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-16.jpg' WHERE id = 16 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-17.jpg' WHERE id = 17 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-18.jpg' WHERE id = 18 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-19.jpg' WHERE id = 19 AND (cover_image IS NULL OR cover_image = '');
UPDATE canteen_stall SET cover_image = '/ui/stall/stall-20.jpg' WHERE id = 20 AND (cover_image IS NULL OR cover_image = '');

-- 2) 用户头像：按用户 id 顺序，依次分配 /ui/avatars 下的图片（不依赖具体 id，重建库也能对上）
UPDATE sys_user u
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM sys_user WHERE deleted = 0
) t ON t.id = u.id
JOIN (
    SELECT 1 AS rn, '/ui/avatars/10002.jpg' AS url
    UNION ALL
    SELECT 2 AS rn, '/ui/avatars/10003.jpg' AS url
    UNION ALL
    SELECT 3 AS rn, '/ui/avatars/10004.jpg' AS url
    UNION ALL
    SELECT 4 AS rn, '/ui/avatars/10005.jpg' AS url
    UNION ALL
    SELECT 5 AS rn, '/ui/avatars/10006.jpg' AS url
    UNION ALL
    SELECT 6 AS rn, '/ui/avatars/10007.jpg' AS url
    UNION ALL
    SELECT 7 AS rn, '/ui/avatars/10008.jpg' AS url
    UNION ALL
    SELECT 8 AS rn, '/ui/avatars/10009.jpg' AS url
    UNION ALL
    SELECT 9 AS rn, '/ui/avatars/10010.jpg' AS url
    UNION ALL
    SELECT 10 AS rn, '/ui/avatars/10011.jpg' AS url
    UNION ALL
    SELECT 11 AS rn, '/ui/avatars/10012.jpg' AS url
    UNION ALL
    SELECT 12 AS rn, '/ui/avatars/10013.jpg' AS url
    UNION ALL
    SELECT 13 AS rn, '/ui/avatars/10014.jpg' AS url
    UNION ALL
    SELECT 14 AS rn, '/ui/avatars/10015.jpg' AS url
    UNION ALL
    SELECT 15 AS rn, '/ui/avatars/10016.jpg' AS url
    UNION ALL
    SELECT 16 AS rn, '/ui/avatars/10017.jpg' AS url
    UNION ALL
    SELECT 17 AS rn, '/ui/avatars/10018.jpg' AS url
    UNION ALL
    SELECT 18 AS rn, '/ui/avatars/10019.jpg' AS url
    UNION ALL
    SELECT 19 AS rn, '/ui/avatars/10020.jpg' AS url
    UNION ALL
    SELECT 20 AS rn, '/ui/avatars/10021.jpg' AS url
    UNION ALL
    SELECT 21 AS rn, '/ui/avatars/10022.jpg' AS url
    UNION ALL
    SELECT 22 AS rn, '/ui/avatars/10023.jpg' AS url
    UNION ALL
    SELECT 23 AS rn, '/ui/avatars/10024.jpg' AS url
    UNION ALL
    SELECT 24 AS rn, '/ui/avatars/10025.jpg' AS url
    UNION ALL
    SELECT 25 AS rn, '/ui/avatars/10026.jpg' AS url
    UNION ALL
    SELECT 26 AS rn, '/ui/avatars/10027.jpg' AS url
    UNION ALL
    SELECT 27 AS rn, '/ui/avatars/10028.jpg' AS url
    UNION ALL
    SELECT 28 AS rn, '/ui/avatars/10029.jpg' AS url
    UNION ALL
    SELECT 29 AS rn, '/ui/avatars/10030.jpg' AS url
    UNION ALL
    SELECT 30 AS rn, '/ui/avatars/10031.jpg' AS url
    UNION ALL
    SELECT 31 AS rn, '/ui/avatars/10032.jpg' AS url
    UNION ALL
    SELECT 32 AS rn, '/ui/avatars/10033.jpg' AS url
    UNION ALL
    SELECT 33 AS rn, '/ui/avatars/10034.jpg' AS url
    UNION ALL
    SELECT 34 AS rn, '/ui/avatars/10035.jpg' AS url
    UNION ALL
    SELECT 35 AS rn, '/ui/avatars/10036.jpg' AS url
    UNION ALL
    SELECT 36 AS rn, '/ui/avatars/10036.png' AS url
    UNION ALL
    SELECT 37 AS rn, '/ui/avatars/10037.jpg' AS url
    UNION ALL
    SELECT 38 AS rn, '/ui/avatars/10037.png' AS url
    UNION ALL
    SELECT 39 AS rn, '/ui/avatars/10038.jpg' AS url
    UNION ALL
    SELECT 40 AS rn, '/ui/avatars/10038.png' AS url
    UNION ALL
    SELECT 41 AS rn, '/ui/avatars/10039.jpg' AS url
    UNION ALL
    SELECT 42 AS rn, '/ui/avatars/10040.jpg' AS url
    UNION ALL
    SELECT 43 AS rn, '/ui/avatars/10041.jpg' AS url
    UNION ALL
    SELECT 44 AS rn, '/ui/avatars/10042.jpg' AS url
    UNION ALL
    SELECT 45 AS rn, '/ui/avatars/10043.jpg' AS url
    UNION ALL
    SELECT 46 AS rn, '/ui/avatars/10044.jpg' AS url
    UNION ALL
    SELECT 47 AS rn, '/ui/avatars/10045.jpg' AS url
    UNION ALL
    SELECT 48 AS rn, '/ui/avatars/10046.jpg' AS url
    UNION ALL
    SELECT 49 AS rn, '/ui/avatars/10047.jpg' AS url
    UNION ALL
    SELECT 50 AS rn, '/ui/avatars/10048.jpg' AS url
    UNION ALL
    SELECT 51 AS rn, '/ui/avatars/10049.jpg' AS url
    UNION ALL
    SELECT 52 AS rn, '/ui/avatars/10050.jpg' AS url
    UNION ALL
    SELECT 53 AS rn, '/ui/avatars/10051.jpg' AS url
    UNION ALL
    SELECT 54 AS rn, '/ui/avatars/10052.jpg' AS url
    UNION ALL
    SELECT 55 AS rn, '/ui/avatars/10053.jpg' AS url
    UNION ALL
    SELECT 56 AS rn, '/ui/avatars/10054.jpg' AS url
    UNION ALL
    SELECT 57 AS rn, '/ui/avatars/10055.jpg' AS url
    UNION ALL
    SELECT 58 AS rn, '/ui/avatars/10056.jpg' AS url
    UNION ALL
    SELECT 59 AS rn, '/ui/avatars/10057.jpg' AS url
    UNION ALL
    SELECT 60 AS rn, '/ui/avatars/10058.jpg' AS url
    UNION ALL
    SELECT 61 AS rn, '/ui/avatars/10059.jpg' AS url
    UNION ALL
    SELECT 62 AS rn, '/ui/avatars/10060.jpg' AS url
    UNION ALL
    SELECT 63 AS rn, '/ui/avatars/10061.jpg' AS url
    UNION ALL
    SELECT 64 AS rn, '/ui/avatars/10062.jpg' AS url
    UNION ALL
    SELECT 65 AS rn, '/ui/avatars/10063.jpg' AS url
    UNION ALL
    SELECT 66 AS rn, '/ui/avatars/10064.jpg' AS url
    UNION ALL
    SELECT 67 AS rn, '/ui/avatars/10065.jpg' AS url
    UNION ALL
    SELECT 68 AS rn, '/ui/avatars/10066.jpg' AS url
    UNION ALL
    SELECT 69 AS rn, '/ui/avatars/10067.jpg' AS url
    UNION ALL
    SELECT 70 AS rn, '/ui/avatars/10068.jpg' AS url
    UNION ALL
    SELECT 71 AS rn, '/ui/avatars/10069.jpg' AS url
    UNION ALL
    SELECT 72 AS rn, '/ui/avatars/10070.jpg' AS url
    UNION ALL
    SELECT 73 AS rn, '/ui/avatars/10071.jpg' AS url
    UNION ALL
    SELECT 74 AS rn, '/ui/avatars/10072.jpg' AS url
    UNION ALL
    SELECT 75 AS rn, '/ui/avatars/10073.jpg' AS url
    UNION ALL
    SELECT 76 AS rn, '/ui/avatars/10074.jpg' AS url
    UNION ALL
    SELECT 77 AS rn, '/ui/avatars/10075.jpg' AS url
    UNION ALL
    SELECT 78 AS rn, '/ui/avatars/10076.jpg' AS url
    UNION ALL
    SELECT 79 AS rn, '/ui/avatars/10077.jpg' AS url
    UNION ALL
    SELECT 80 AS rn, '/ui/avatars/10079.jpg' AS url
    UNION ALL
    SELECT 81 AS rn, '/ui/avatars/10081.jpg' AS url
    UNION ALL
    SELECT 82 AS rn, '/ui/avatars/10082.jpg' AS url
    UNION ALL
    SELECT 83 AS rn, '/ui/avatars/10083.jpg' AS url
    UNION ALL
    SELECT 84 AS rn, '/ui/avatars/10084.jpg' AS url
    UNION ALL
    SELECT 85 AS rn, '/ui/avatars/10085.jpg' AS url
    UNION ALL
    SELECT 86 AS rn, '/ui/avatars/10086.jpg' AS url
    UNION ALL
    SELECT 87 AS rn, '/ui/avatars/10087.png' AS url
    UNION ALL
    SELECT 88 AS rn, '/ui/avatars/10088.jpg' AS url
    UNION ALL
    SELECT 89 AS rn, '/ui/avatars/10089.jpeg' AS url
    UNION ALL
    SELECT 90 AS rn, '/ui/avatars/10090.jpg' AS url
    UNION ALL
    SELECT 91 AS rn, '/ui/avatars/10091.jpg' AS url
    UNION ALL
    SELECT 92 AS rn, '/ui/avatars/10092.jpg' AS url
    UNION ALL
    SELECT 93 AS rn, '/ui/avatars/10093.jpg' AS url
    UNION ALL
    SELECT 94 AS rn, '/ui/avatars/10094.jpg' AS url
    UNION ALL
    SELECT 95 AS rn, '/ui/avatars/10095.jpg' AS url
    UNION ALL
    SELECT 96 AS rn, '/ui/avatars/10097.png' AS url
    UNION ALL
    SELECT 97 AS rn, '/ui/avatars/10098.jpg' AS url
    UNION ALL
    SELECT 98 AS rn, '/ui/avatars/10099.jpg' AS url
    UNION ALL
    SELECT 99 AS rn, '/ui/avatars/10100.png' AS url
    UNION ALL
    SELECT 100 AS rn, '/ui/avatars/10101.jpg' AS url
    UNION ALL
    SELECT 101 AS rn, '/ui/avatars/10102.png' AS url
    UNION ALL
    SELECT 102 AS rn, '/ui/avatars/10103.png' AS url
    UNION ALL
    SELECT 103 AS rn, '/ui/avatars/10104.png' AS url
    UNION ALL
    SELECT 104 AS rn, '/ui/avatars/10105.png' AS url
    UNION ALL
    SELECT 105 AS rn, '/ui/avatars/10106.png' AS url
    UNION ALL
    SELECT 106 AS rn, '/ui/avatars/10107.png' AS url
    UNION ALL
    SELECT 107 AS rn, '/ui/avatars/10108.png' AS url
    UNION ALL
    SELECT 108 AS rn, '/ui/avatars/10109.png' AS url
    UNION ALL
    SELECT 109 AS rn, '/ui/avatars/10110.png' AS url
    UNION ALL
    SELECT 110 AS rn, '/ui/avatars/10111.jpg' AS url
    UNION ALL
    SELECT 111 AS rn, '/ui/avatars/10112.png' AS url
) a ON a.rn = t.rn
SET u.avatar = a.url
WHERE u.avatar IS NULL OR u.avatar = '';

-- 3) 商家头像统一用自家档口 logo（/ui/logo/stall-<stall_id>.webp）
UPDATE sys_user SET avatar = '/ui/logo/stall-1.webp' WHERE role = 'MERCHANT' AND stall_id = 1;
UPDATE sys_user SET avatar = '/ui/logo/stall-2.webp' WHERE role = 'MERCHANT' AND stall_id = 2;
UPDATE sys_user SET avatar = '/ui/logo/stall-3.webp' WHERE role = 'MERCHANT' AND stall_id = 3;
UPDATE sys_user SET avatar = '/ui/logo/stall-4.webp' WHERE role = 'MERCHANT' AND stall_id = 4;
UPDATE sys_user SET avatar = '/ui/logo/stall-5.webp' WHERE role = 'MERCHANT' AND stall_id = 5;
UPDATE sys_user SET avatar = '/ui/logo/stall-6.webp' WHERE role = 'MERCHANT' AND stall_id = 6;
UPDATE sys_user SET avatar = '/ui/logo/stall-7.webp' WHERE role = 'MERCHANT' AND stall_id = 7;
UPDATE sys_user SET avatar = '/ui/logo/stall-8.webp' WHERE role = 'MERCHANT' AND stall_id = 8;
UPDATE sys_user SET avatar = '/ui/logo/stall-9.webp' WHERE role = 'MERCHANT' AND stall_id = 9;
UPDATE sys_user SET avatar = '/ui/logo/stall-10.webp' WHERE role = 'MERCHANT' AND stall_id = 10;
UPDATE sys_user SET avatar = '/ui/logo/stall-11.webp' WHERE role = 'MERCHANT' AND stall_id = 11;
UPDATE sys_user SET avatar = '/ui/logo/stall-12.webp' WHERE role = 'MERCHANT' AND stall_id = 12;
UPDATE sys_user SET avatar = '/ui/logo/stall-13.webp' WHERE role = 'MERCHANT' AND stall_id = 13;
UPDATE sys_user SET avatar = '/ui/logo/stall-14.webp' WHERE role = 'MERCHANT' AND stall_id = 14;
UPDATE sys_user SET avatar = '/ui/logo/stall-15.webp' WHERE role = 'MERCHANT' AND stall_id = 15;
UPDATE sys_user SET avatar = '/ui/logo/stall-16.webp' WHERE role = 'MERCHANT' AND stall_id = 16;
UPDATE sys_user SET avatar = '/ui/logo/stall-17.webp' WHERE role = 'MERCHANT' AND stall_id = 17;
UPDATE sys_user SET avatar = '/ui/logo/stall-18.webp' WHERE role = 'MERCHANT' AND stall_id = 18;
UPDATE sys_user SET avatar = '/ui/logo/stall-19.webp' WHERE role = 'MERCHANT' AND stall_id = 19;
UPDATE sys_user SET avatar = '/ui/logo/stall-20.webp' WHERE role = 'MERCHANT' AND stall_id = 20;
