#!/bin/sh
# Clones the upstream sources this port is based on into .upstream/ (git-ignored).
set -e
cd "$(dirname "$0")/.."
TACZ_COMMIT=b482eff8c94a733ac8d0910193fca3893954027c
SBM_BRANCH=1.20.1-2.2.x
mkdir -p .upstream
if [ ! -d .upstream/TACZ ]; then
    git clone --filter=blob:none --no-checkout https://github.com/MCModderAnchor/TACZ .upstream/TACZ
    git -C .upstream/TACZ checkout -q "$TACZ_COMMIT"
fi
if [ ! -d .upstream/SimpleBedrockModel ]; then
    git clone --depth 1 -b "$SBM_BRANCH" https://github.com/MCModderAnchor/SimpleBedrockModel .upstream/SimpleBedrockModel
fi
