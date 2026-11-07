"""把分散的 SQL 脚本合并成一份自包含的全量初始化脚本 db/full-setup.sql。"""

import os

DB = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'db')
PARTS = [
    ('schema.sql', '一、建库建表'),
    ('init-data.sql', '二、初始业务数据（档口 / 菜品 / 评价 / 用户 / 收藏 / 留言 / 登录日志）'),
    ('banner-init.sql', '三、首页轮播广告'),
    ('patch-order.sql', '四、预约订餐表与档口扩展字段'),
    ('patch-chat.sql', '五、智能客服知识库与问答日志'),
    ('patch-dish-images.sql', '六、菜品配图路径'),
]


def main():
    header = [
        '-- 校园食堂菜品反馈评价系统 —— 数据库全量初始化脚本（自包含）',
        '-- 用法：mysql -uroot -p < db/full-setup.sql',
        '-- 或：mysql -uroot -p123456 < db/full-setup.sql',
        '--',
        '-- 本文件由 scripts/build-full-sql.py 自动生成，请勿手工修改。',
        '-- 可重复执行：会把库重置为初始状态（评价表等会先清空再灌入）。',
        '',
        'SET NAMES utf8mb4;',
        '',
    ]
    out = list(header)
    for idx, (name, title) in enumerate(PARTS):
        body = open(os.path.join(DB, name), encoding='utf-8').read().strip()
        out.append('-- ' + '=' * 60)
        out.append('-- ' + title + '（来源：db/%s）' % name)
        out.append('-- ' + '=' * 60)
        if idx > 0:  # 首段自带 CREATE DATABASE + USE，提前 USE 会报库不存在
            out.append('USE canteen_feedback;')
        out.append('')
        out.append(body)
        out.append('')

    dst = os.path.join(DB, 'full-setup.sql')
    open(dst, 'w', encoding='utf-8').write('\n'.join(out) + '\n')
    print('已生成:', dst, '%.0f KB' % (os.path.getsize(dst) / 1024))


if __name__ == '__main__':
    main()
