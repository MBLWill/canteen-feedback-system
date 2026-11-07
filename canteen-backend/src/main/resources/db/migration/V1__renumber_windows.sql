-- 档口编号体系重构：01-12 → S101/S201/N101 格式（可重复执行，按旧编号定位）
-- 规则：南区1楼=S1xx，南区2楼=S2xx，北区=N1xx；序号按 01-20 顺序
USE canteen_feedback;

-- 南区 1 楼
UPDATE canteen_stall SET window_no = 'S101' WHERE window_no = '01' AND deleted = 0;
UPDATE canteen_stall SET window_no = 'S102' WHERE window_no = '02' AND deleted = 0;
UPDATE canteen_stall SET window_no = 'S103' WHERE window_no = '03' AND deleted = 0;
-- 南区 2 楼
UPDATE canteen_stall SET window_no = 'S201' WHERE window_no = '04' AND deleted = 0;
UPDATE canteen_stall SET window_no = 'S202' WHERE window_no = '05' AND deleted = 0;
UPDATE canteen_stall SET window_no = 'S203' WHERE window_no = '06' AND deleted = 0;
-- 北区 1 楼
UPDATE canteen_stall SET window_no = 'N101' WHERE window_no = '07' AND deleted = 0;
UPDATE canteen_stall SET window_no = 'N102' WHERE window_no = '08' AND deleted = 0;
UPDATE canteen_stall SET window_no = 'N103' WHERE window_no = '09' AND deleted = 0;
UPDATE canteen_stall SET window_no = 'N104' WHERE window_no = '10' AND deleted = 0;
UPDATE canteen_stall SET window_no = 'N105' WHERE window_no = '11' AND deleted = 0;
UPDATE canteen_stall SET window_no = 'N106' WHERE window_no = '12' AND deleted = 0;

SELECT id, window_no, name, area, floor FROM canteen_stall WHERE deleted = 0 ORDER BY area, floor, window_no;
