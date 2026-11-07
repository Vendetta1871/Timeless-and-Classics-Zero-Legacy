#!/bin/sh
# Headless autotest: runs the client autotest (TACZ_AUTOTEST=1, see AGENTS.md) inside a monitorless gamescope on
# the eGPU, so no window opens on the desktop. Screenshots land in run/screenshots (run/obfuscated/screenshots).
#   tools/autotest.sh                       - runClient
#   tools/autotest.sh runObfClient          - any Gradle task and arguments
#   TACZ_AUTOTEST_TARGET=dummy tools/autotest.sh   - the TACZ_AUTOTEST_* variables pass through
#   AUTOTEST_SIZE="1920 1080"               - window size (default 854 480, the vanilla default window)
#   AUTOTEST_GPU=10de:1db1                  - Vulkan device for gamescope (default: the V100)
#   AUTOTEST_WINDOW=1                       - on the desktop instead, without gamescope
# The log is build/autotest.log; the script exits with the Gradle exit code.
cd "$(dirname "$0")/.." || exit 1
task=${1:-runClient}
[ $# -gt 0 ] && shift
uid=$(id -u)
export XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR:-/run/user/$uid}
export DBUS_SESSION_BUS_ADDRESS=${DBUS_SESSION_BUS_ADDRESS:-unix:path=$XDG_RUNTIME_DIR/bus}
size=${AUTOTEST_SIZE:-854 480}
w=${size% *} h=${size#* }
mkdir -p build
if [ -n "$AUTOTEST_WINDOW" ]; then
    TACZ_AUTOTEST=1 ./gw "$task" "$@" > build/autotest.log 2>&1 < /dev/null
else
    # gamescope starts its own Xwayland and hands its DISPLAY to the client; the desktop session is not used.
    # gamescope itself may crash on shutdown after the client exited, so the Gradle exit code is passed in a file.
    rm -f build/autotest.status
    # The extra sh (not exec'ed, hence "; true") reports that crash into the log instead of the terminal.
    sh -c '"$@"; true' sh env -u DISPLAY -u WAYLAND_DISPLAY XDG_SESSION_TYPE=wayland TACZ_AUTOTEST=1 \
        gamescope --backend headless --prefer-vk-device "${AUTOTEST_GPU:-10de:1db1}" -W "$w" -H "$h" -w "$w" -h "$h" -r 1000 -- \
        sh -c './gw "$@"; echo $? > build/autotest.status' sh "$task" "$@" > build/autotest.log 2>&1 < /dev/null
fi
status=$?
[ -f build/autotest.status ] && status=$(cat build/autotest.status)
grep -E '\[dev\] screenshot|BUILD (SUCCESSFUL|FAILED)|Exception' build/autotest.log | head -20
exit $status
