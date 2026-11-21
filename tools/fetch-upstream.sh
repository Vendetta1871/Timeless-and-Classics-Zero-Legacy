#!/bin/sh
# Clones the upstream sources this port is based on into .upstream/ (git-ignored).
set -e
cd "$(dirname "$0")/.."
# TACZ tag 1.2.0-hotfix-forge-mc1.20.1
TACZ_COMMIT=4a038593b980b901941f54c397afc7854f8b017a
SBM_TAG=2.6.3-forge-mc1.20.1
mkdir -p .upstream
if [ ! -d .upstream/TACZ ]; then
    git clone --filter=blob:none --no-checkout https://github.com/MCModderAnchor/TACZ .upstream/TACZ
    git -C .upstream/TACZ checkout -q "$TACZ_COMMIT"
fi
if [ ! -d .upstream/SimpleBedrockModel ]; then
    git clone --depth 1 -b "$SBM_TAG" https://github.com/MCModderAnchor/SimpleBedrockModel .upstream/SimpleBedrockModel
fi
