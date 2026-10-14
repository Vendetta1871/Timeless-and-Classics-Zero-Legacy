#!/usr/bin/env python3
"""Restores code that was commented out with `// TODO(port): needs <Class>` once <Class> has been ported.

Usage: tools/restore_todos.py <Class> [<Class> ...]

For each marker line naming one of the classes, the marker is removed and the commented-out lines directly below it
are uncommented; a following `... // placeholder until <Class> is ported` line is dropped. Review the diff and compile.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / 'src/main/java'


def restore(text, names):
    lines = text.split('\n')
    out = []
    i = 0
    changed = False
    while i < len(lines):
        line = lines[i]
        m = re.match(r'^(\s*)// TODO\(port\): needs (\w+)\b(.*)$', line)
        if m and m.group(2) in names and '(' not in m.group(3):
            changed = True
            i += 1
            while i < len(lines) and re.match(r'^\s*//', lines[i]) and 'TODO(port)' not in lines[i]:
                out.append(re.sub(r'^(\s*)// ?', r'\1', lines[i], count=1))
                i += 1
            if i < len(lines) and 'placeholder until' in lines[i]:
                i += 1
            continue
        out.append(line)
        i += 1
    return '\n'.join(out), changed


def main(names):
    for path in ROOT.rglob('*.java'):
        text = path.read_text()
        if 'TODO(port): needs' not in text:
            continue
        new, changed = restore(text, set(names))
        if changed:
            path.write_text(new)
            print(path.relative_to(ROOT))


if __name__ == '__main__':
    main(sys.argv[1:])
