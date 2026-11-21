#!/usr/bin/env python3
"""Merges upstream TACZ changes between two commits into the already ported sources.

For every upstream Java file modified between OLD and NEW that exists in the port, runs a three-way merge:
base = port.py(upstream file at OLD), theirs = port.py(upstream file at NEW), ours = the ported file.
The mechanical 1.20 -> 1.12 rewrites of port.py cancel out, so only the real upstream changes are applied;
overlapping edits are left as diff3 conflict markers (<<<<<<< port / ||||||| base / ======= / >>>>>>> upstream).

Usage: tools/merge_upstream.py OLD [NEW] [--dry-run] [path relative to com/tacz/guns ...]
NEW defaults to the commit checked out in .upstream/TACZ. Prints new (A) and deleted (D) upstream files for
manual handling; new files are started with tools/port.py as usual.
"""
import importlib.util
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
_spec = importlib.util.spec_from_file_location('port', ROOT / 'tools/port.py')
port = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(port)

REPO = port.UPSTREAM.parents[5]  # .upstream/TACZ
PREFIX = 'src/main/java/com/tacz/guns/'


def git(*args):
    return subprocess.run(['git', '-C', str(REPO), *args], capture_output=True, text=True, check=True).stdout


def show(rev, rel):
    return git('show', '%s:%s%s' % (rev, PREFIX, rel))


def main(argv):
    dry = '--dry-run' in argv
    args = [a for a in argv if not a.startswith('--')]
    if not args:
        print(__doc__)
        return 1
    old = args[0]
    new = args[1] if len(args) > 1 and not args[1].endswith('.java') else 'HEAD'
    only = {a.removeprefix('com/tacz/guns/') for a in args[1:] if a.endswith('.java')}
    status = git('diff', '-M', '--name-status', old, new, '--', PREFIX).splitlines()
    clean, conflicted, missing, added, deleted, renamed = [], [], [], [], [], []
    for line in status:
        parts = line.split('\t')
        kind = parts[0][0]
        rel = parts[-1].removeprefix(PREFIX)
        if only and rel not in only:
            continue
        if kind == 'A':
            added.append(rel)
            continue
        if kind == 'D':
            deleted.append(rel)
            continue
        if kind == 'R':
            renamed.append('%s -> %s' % (parts[1].removeprefix(PREFIX), rel))
            continue
        target = port.TARGET / rel
        if not target.is_file():
            missing.append(rel)
            continue
        base = port.port_source(show(old, rel))
        theirs = port.port_source(show(new, rel))
        with tempfile.TemporaryDirectory() as tmp:
            b = Path(tmp, 'base')
            t = Path(tmp, 'theirs')
            o = Path(tmp, 'ours')
            b.write_text(base, encoding='utf-8')
            t.write_text(theirs, encoding='utf-8')
            o.write_text(target.read_text(encoding='utf-8'), encoding='utf-8')
            res = subprocess.run(['git', 'merge-file', '--diff3', '-L', 'port', '-L', 'base', '-L', 'upstream',
                                  str(o), str(b), str(t)], capture_output=True, text=True)
            if res.returncode < 0 or res.returncode > 127:
                print('merge failed: %s: %s' % (rel, res.stderr), file=sys.stderr)
                continue
            if not dry:
                target.write_text(o.read_text(encoding='utf-8'), encoding='utf-8')
        (conflicted if res.returncode else clean).append((rel, res.returncode))
    print('merged cleanly: %d' % len(clean))
    print('with conflicts: %d' % len(conflicted))
    for rel, n in sorted(conflicted, key=lambda c: -c[1]):
        print('  C %s (%d)' % (rel, n))
    for title, items in (('not ported, skipped', missing), ('renamed upstream', renamed),
                         ('deleted upstream', deleted), ('new upstream', added)):
        if items:
            print('%s: %d' % (title, len(items)))
            for rel in items:
                print('  %s' % rel)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
