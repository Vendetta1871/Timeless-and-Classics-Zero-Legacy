#!/usr/bin/env python3
"""Reports common (server-side) code that calls client-only vanilla methods.

1.12.2 strips @SideOnly(Side.CLIENT) members on the dedicated server, so e.g. MathHelper.frac or the JsonElement
overload of JsonUtils.isString throw NoSuchMethodError there. This checks static calls `Class.method(` against the
client-only static methods of the decompiled Minecraft sources (build/rfg/minecraft-src), skipping client packages and
members annotated @SideOnly(Side.CLIENT). Instance calls are not checked (no type information).

Usage: tools/check_sides.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MC = ROOT / 'build/rfg/minecraft-src/java/net/minecraft'
SRC = ROOT / 'src/main/java/com/tacz/guns'
CLIENT_PREFIXES = ('client/', 'mixin/client/', 'legacy/client/', 'legacy/render/', 'legacy/fp/', 'legacy/debug/', 'compat/')
STATIC_DECL = re.compile(r'@SideOnly\(Side\.CLIENT\)\s*(?:@\w+(?:\([^)]*\))?\s*)*public\s+static\s+(?:final\s+)?(?:<[^>]+>\s*)?[\w<>\[\],.? ]+\s+(\w+)\s*\(([^)]*)\)')


def count_args(text):
    """Top-level comma count + 1 of an argument or parameter list."""
    depth, count = 0, 1
    for ch in text:
        if ch in '(<[{':
            depth += 1
        elif ch in ')>]}':
            depth -= 1
        elif ch == ',' and depth == 0:
            count += 1
    return count


def call_args(line, start):
    """Argument count of the call whose '(' is at line[start]."""
    depth, i = 0, start
    while i < len(line):
        if line[i] == '(':
            depth += 1
        elif line[i] == ')':
            depth -= 1
            if depth == 0:
                inner = line[start + 1:i].strip()
                return 0 if not inner else count_args(inner)
        i += 1
    return None


def client_statics():
    found = set()
    for path in MC.rglob('*.java'):
        if path.relative_to(MC).as_posix().startswith('client/'):
            continue
        for match in STATIC_DECL.finditer(path.read_text(errors='replace')):
            params = match.group(2).strip()
            found.add((path.stem, match.group(1), 0 if not params else count_args(params)))
    return found


def main():
    if not MC.is_dir():
        sys.exit('run ./gw compileJava first (needs build/rfg/minecraft-src)')
    statics = client_statics()
    problems = 0
    for path in sorted(SRC.rglob('*.java')):
        rel = path.relative_to(SRC).as_posix()
        if rel.startswith(CLIENT_PREFIXES):
            continue
        skip = None
        for number, line in enumerate(path.read_text().split('\n'), 1):
            if skip is not None:
                if line.startswith(skip + '}'):
                    skip = None
                continue
            if '@SideOnly(Side.CLIENT)' in line:
                skip = line[:len(line) - len(line.lstrip())]
                continue
            if line.strip().startswith(('//', '*', 'import')):
                continue
            for match in re.finditer(r'\b([A-Z]\w*)\.(\w+)\(', line):
                arity = call_args(line, match.end() - 1)
                if (match.group(1), match.group(2), arity) in statics:
                    print('%s:%d: %s.%s is client-only' % (rel, number, match.group(1), match.group(2)))
                    problems += 1
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
