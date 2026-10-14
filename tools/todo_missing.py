#!/usr/bin/env python3
"""Comments out uses of classes that are not ported yet, with `// TODO(port): needs <Class>` markers.

Usage: tools/todo_missing.py <file.java> ...

Imports of com.tacz.guns classes that do not exist under src/ are commented out, and so is every single-line
statement that mentions one of those classes (multi-line statements are reported, fix those by hand).
`tools/restore_todos.py <Class>` undoes it once the class exists.
"""
import re
import sys
from pathlib import Path

SRC = Path(__file__).resolve().parent.parent / 'src/main/java'


def process(path):
    text = path.read_text()
    lines = text.split('\n')
    missing = set()
    out = []
    for line in lines:
        m = re.match(r'^import com\.tacz\.guns\.([\w.]+);$', line)
        if m and not (SRC / 'com/tacz/guns' / (m.group(1).replace('.', '/') + '.java')).exists():
            name = m.group(1).rsplit('.', 1)[1]
            missing.add(name)
            out.append('// TODO(port): needs %s' % name)
            out.append('//' + line)
            continue
        m = re.match(r'^// TODO\(port\): needs (\w+)$', line)
        if m:
            missing.add(m.group(1))
        out.append(line)
    if not missing:
        return
    result = []
    for line in out:
        if line.lstrip().startswith('//') or line.startswith('import '):
            result.append(line)
            continue
        hits = [n for n in missing if re.search(r'\b%s\b' % re.escape(n), line)]
        if hits:
            stripped = line.strip()
            if stripped.endswith(';') or stripped.endswith('{') and stripped.startswith('if'):
                indent = line[:len(line) - len(line.lstrip())]
                if stripped.endswith(';'):
                    result.append('%s// TODO(port): needs %s' % (indent, hits[0]))
                    result.append('%s// %s' % (indent, stripped))
                    continue
            print('%s: fix by hand: %s' % (path.relative_to(SRC), stripped))
        result.append(line)
    path.write_text('\n'.join(result))


if __name__ == '__main__':
    for arg in sys.argv[1:]:
        process(Path(arg).resolve())
