#!/usr/bin/env bash
#
# Reports where the running OAS Buddy window actually is, for the case where the app starts but
# no window appears on screen.
#
# The interesting answer is almost never "the app crashed" -- the process is usually alive with a
# correctly sized, mapped window that something else is covering, or that landed on a workspace or
# monitor you are not looking at. So this script asks, in order: is the process running, does it
# own a window, what geometry does that window have, and does the compositor consider it visible.
#
# The compositor is the authority on the last question, not X11. Under a Wayland session a JavaFX
# window is an XWayland client, and the coordinates X11 reports are XWayland-internal -- they do
# not tell you which monitor or workspace the window is really on. Where hyprctl is available it
# is therefore asked as well, and its answer wins.
#
# Reads only; changes nothing. Everything it prints is safe to paste into a bug report.
#
#   Requires: nothing mandatory. Uses xprop/xwininfo (x11-utils) and hyprctl if present.
#   Usage:    tools/diagnose-window.sh

set -uo pipefail

# Matches the WM_CLASS/class the app sets, and its window title.
readonly APP_CLASS="no.maddin.oasbuddy.desktop.MainApp"
readonly APP_TITLE="OAS Buddy"

section() { printf '\n== %s ==\n' "$1"; }
have() { command -v "$1" >/dev/null 2>&1; }

section "Session"
printf 'XDG_SESSION_TYPE   %s\n' "${XDG_SESSION_TYPE:-unset}"
printf 'XDG_CURRENT_DESKTOP %s\n' "${XDG_CURRENT_DESKTOP:-unset}"
printf 'WAYLAND_DISPLAY    %s\n' "${WAYLAND_DISPLAY:-unset}"
printf 'DISPLAY            %s\n' "${DISPLAY:-unset}"

section "Process"
# The .deb installs a launcher at /opt/oas-buddy/bin, so match that as well as a dev-mode JVM.
pids=$(pgrep -f "oas-buddy|$APP_CLASS" 2>/dev/null | tr '\n' ' ')
if [ -z "${pids// /}" ]; then
    echo "No OAS Buddy process is running -- it exited rather than hid. Run the launcher from a"
    echo "terminal (/opt/oas-buddy/bin/'OAS Buddy') to see the startup error it printed."
    exit 0
fi
echo "running, pid(s): $pids"
ps -o pid,etime,args -p ${pids// /,} 2>/dev/null | tail -n +2

section "Monitors"
if have hyprctl; then
    hyprctl monitors 2>/dev/null | grep -E '^Monitor|active workspace|focused:'
elif have xrandr; then
    xrandr --listmonitors 2>/dev/null
else
    echo "(no hyprctl or xrandr)"
fi

section "X11 view"
if ! have xprop || [ -z "${DISPLAY:-}" ]; then
    echo "(no xprop or no DISPLAY -- skipping)"
else
    found=0
    for w in $(xprop -root _NET_CLIENT_LIST 2>/dev/null | sed 's/.*# //; s/,/ /g'); do
        xprop -id "$w" WM_CLASS 2>/dev/null | grep -q "$APP_CLASS" || continue
        found=1
        echo "window id $w"
        xprop -id "$w" _NET_WM_NAME _NET_WM_PID _NET_WM_STATE _NET_WM_DESKTOP 2>&1 | sed 's/^/  /'
        # Geometry plus map state. "IsViewable" means X11 has it mapped; it says nothing about
        # whether anything is stacked on top, which is why the compositor is asked below.
        xwininfo -id "$w" 2>/dev/null | grep -E 'Absolute|Width:|Height:|Map State' | sed 's/^/  /'
        # A window whose icon never reached the window manager shows the default Java icon in the
        # taskbar, which makes it much harder to spot when hunting for a lost window.
        if xprop -id "$w" _NET_WM_ICON 2>/dev/null | grep -q 'not found'; then
            echo "  _NET_WM_ICON  UNSET (taskbar shows the default icon)"
        else
            echo "  _NET_WM_ICON  set"
        fi
    done
    [ "$found" -eq 1 ] || echo "No X11 window with class $APP_CLASS. Under a pure Wayland session"
    [ "$found" -eq 1 ] || echo "with no XWayland this is expected; trust the compositor view below."
fi

section "Compositor view"
if ! have hyprctl; then
    echo "(not Hyprland, or hyprctl unavailable -- the X11 view above is all we have)"
    exit 0
fi

# hyprctl's own fields answer the question directly: "visible" is false when something covers the
# window, which is the difference X11's "IsViewable" cannot express.
hyprctl clients 2>/dev/null | awk -v title="$APP_TITLE" '
    /^Window /   { inblock = index($0, title) > 0 }
    inblock && /^\t(mapped|hidden|visible|at|size|workspace|floating|fullscreen|monitor|pid|xwayland):/ { print }
    inblock && /^Window / { print }
'

section "Every window, for comparison"
# The comparison is the point. If sibling windows on the same workspace also report visible=false,
# nothing is wrong with OAS Buddy -- they are all behind the same fullscreen window.
if have python3; then
    hyprctl clients -j 2>/dev/null | python3 -c "
import json, sys
rows = json.load(sys.stdin)
print('%-34s %-4s %-4s %-6s %-4s %-8s %-7s %s' % ('CLASS','WS','MON','VISIBLE','FS','FLOATING','XWL','GEOMETRY'))
for c in sorted(rows, key=lambda c: (c['workspace']['id'], c['class'])):
    print('%-34s %-4s %-4s %-6s %-4s %-8s %-7s %s+%s' % (
        c['class'][:34], c['workspace']['id'], c['monitor'], c.get('visible'),
        c['fullscreen'], c['floating'], c['xwayland'], c['size'], c['at']))
"
else
    hyprctl clients 2>/dev/null | grep -E '^Window |visible:|workspace:|fullscreen:'
fi

section "Options that hide a new window"
# A new window opening underneath an existing fullscreen window is the usual explanation. These
# two options decide whether the compositor ever brings it forward.
for opt in misc:on_focus_under_fullscreen misc:focus_on_activate misc:initial_workspace_tracking; do
    printf '%-38s %s\n' "$opt" "$(hyprctl getoption "$opt" 2>&1 | grep -E '^(int|str):' | head -1)"
done

cat <<'HINT'

If OAS Buddy shows visible=false while a fullscreen window sits on the same workspace, the window
is behind it and nothing is wrong with the app. To bring it out:

  hyprctl dispatch focuswindow class:no.maddin.oasbuddy.desktop.MainApp
  hyprctl dispatch movetoworkspace +0,class:no.maddin.oasbuddy.desktop.MainApp

Setting misc:on_focus_under_fullscreen = 1 makes focusing a covered window bring it to the front,
but weigh that against what else it changes: a short-lived toplevel that takes focus (browser
tooltips and splash windows do this) then takes fullscreen with it when it closes. A keybind
running the focuswindow dispatch above costs nothing and breaks nothing.
HINT
