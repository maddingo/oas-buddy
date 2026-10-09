#!/usr/bin/env bash
# Plots net lines added over time (insertions minus deletions, summed commit by commit).
# Reads only. Needs git, awk and gnuplot.
#
#   tools/lines.sh                 open the plot in a window
#   tools/lines.sh loc.png         write a PNG instead
#   tools/lines.sh loc.png '*.java'  restrict to files matching a pathspec
set -euo pipefail

output="${1:-}"
pathspec="${2:-}"

data="$(mktemp)"
trap 'rm -f "$data"' EXIT

git log --reverse --numstat --format='C %ad' --date=short ${pathspec:+-- "$pathspec"} \
  | awk '/^C /{d=$2} NF==3 && $1!="-"{t+=$1-$2; v[d]=t} END{for(k in v) print k, v[k]}' \
  | sort > "$data"

if [ -n "$output" ]; then
  terminal="set terminal png size 900,500; set output \"$output\";"
  persist=()
else
  terminal=""
  persist=(-persist)
fi

gnuplot "${persist[@]}" -e "$terminal set xdata time; set timefmt '%Y-%m-%d'; set format x '%b %d'; set xtics rotate by -45; set grid; set title 'Net lines added over time'; set ylabel 'lines'; plot '$data' using 1:2 with linespoints title ''"
