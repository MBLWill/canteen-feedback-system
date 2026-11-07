"""生成一份可分发的项目副本：去掉依赖缓存与本机 IDE 配置，保留源码、SQL、图片与构建产物。"""

import os
import shutil

SRC = r'D:\PRJ2026\qimozuoye'
DST = r'D:\PRJ2026\canteen-feedback-system'

EXCLUDE_DIRS = {'node_modules', 'target', 'dist-demo', 'dish-images',
                '.idea', '.vscode', '.git', 'mock'}
EXCLUDE_FILES = {'backend.log', 'frontend.log', 'jdk17-smoke.log',
                 '.env.demo', 'DemoBar.vue', 'gen-mock-snapshot.mjs'}


def main():
    if os.path.exists(DST):
        shutil.rmtree(DST)
    os.makedirs(DST)

    files = 0
    size = 0
    for root, dirs, names in os.walk(SRC):
        dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]
        rel = os.path.relpath(root, SRC)
        out = DST if rel == '.' else os.path.join(DST, rel)
        os.makedirs(out, exist_ok=True)
        for n in names:
            if n in EXCLUDE_FILES:
                continue
            s = os.path.join(root, n)
            shutil.copy2(s, os.path.join(out, n))
            files += 1
            size += os.path.getsize(s)

    print('副本目录:', DST)
    print('文件数:', files, '体积: %.1f MB' % (size / 1024 / 1024))


if __name__ == '__main__':
    main()
