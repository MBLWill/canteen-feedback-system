-- 同步知识库：预约订餐已整合进购物车，更新 ORDER 类 FAQ 文案（可重复执行）
USE canteen_feedback;

UPDATE sys_faq SET
  question = '怎么下单订餐',
  answer = '在「菜品浏览」点菜品卡片右下角的 + 加入购物车，或进入菜品详情页点「加入购物车」；然后点右下角悬浮球切到「购物车」页，选好档口点「结算」，填取餐时间（7 天内）和备注，提交并支付即可。订单在「我的订单」里查看。',
  keywords = '下单,订餐,购物车,加购,结算,支付,怎么买,怎么点,怎么订,提前点,留饭,预约'
WHERE category = 'ORDER' AND question = '怎么预约订餐' AND deleted = 0;

UPDATE sys_faq SET
  question = '订单可以取消吗',
  answer = '可以。在「我的订单」里找到未支付的待取餐订单，点「取消订单」即可；已支付订单请联系档口或管理员处理。',
  keywords = '取消订单,退订,不要了,怎么取消,取消预约,取消'
WHERE category = 'ORDER' AND question = '预约可以取消吗' AND deleted = 0;

UPDATE sys_faq SET
  question = '最多可以提前多久下单'
WHERE category = 'ORDER' AND question = '最多可以提前多久预约' AND deleted = 0;

UPDATE sys_faq SET
  question = '下单了没去取会怎样'
WHERE category = 'ORDER' AND question = '预约了没去取会怎样' AND deleted = 0;
