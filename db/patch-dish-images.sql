-- 菜品专属配图：图片放在 canteen-frontend/public/dishes/<id>.jpg
UPDATE dish
SET image = CONCAT('/dishes/', id, '.jpg')
WHERE deleted = 0
  AND (image IS NULL OR image = '');
