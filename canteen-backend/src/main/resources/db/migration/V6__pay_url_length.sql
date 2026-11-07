-- 加长 pay_record.pay_url：接入支付宝沙箱后，收银台 URL 含 RSA2 签名，长度会超过旧列的
-- VARCHAR(500)/VARCHAR(1000) 上限，导致下单时 "Data too long for column 'pay_url'"。
-- 本脚本幂等：直接 ALTER 到 VARCHAR(4000)，重复执行安全（MySQL 允许重复 MODIFY 到同类型）。

USE canteen_feedback;

ALTER TABLE pay_record
    MODIFY COLUMN pay_url VARCHAR(4000) NULL COMMENT '支付跳转地址或二维码内容（含签名，需足够长）';

SELECT 'pay_url 列已加长到 4000' AS result;
