#!/usr/bin/env bash
set -euo pipefail

execution_date="$1"
archive_days="$2"
hdfs_root="$3"
remote="$4"
rclone_conf="$5"

target=$(date -d "$execution_date - $archive_days days" +%s)

workdir="$(mktemp -d)"
stage_root="$workdir/stage"
trap 'rm -rf "$workdir"' EXIT

cp "$rclone_conf" "$workdir/rclone.conf"
chmod 600 "$workdir/rclone.conf"
rclone_cmd=(rclone --config "$workdir/rclone.conf")

declare -A day_paths

list_names() {
    hdfs ls "$1" 2>/dev/null | while read -r name; do
        [[ -z "$name" || "$name" == /* ]] && continue
        echo "$name"
    done
}

collect_days() {
    local platform kind base name

    while read -r platform; do
        [[ -n "$platform" && "$platform" != "Archive" ]] || continue

        for kind in Vacancies Rates Alive; do
            base="/$hdfs_root/$platform/$kind"

            while read -r name; do
                [[ "$name" == ????-??-??* ]] || continue
                day_paths["${name:0:10}"]+="$base/$name"$'\n'
            done < <(list_names "$base/")
        done
    done < <(list_names "/$hdfs_root/")
}

archive_day() {
    local day="$1"

    local day_ts
    day_ts=$(date -d "$day" +%s 2>/dev/null) || return 0
    (( day_ts < target )) || return 0

    local hdfs_dir="/$hdfs_root/Archive/${day:0:7}"
    local hdfs_archive="$hdfs_dir/$day.tar.gz"
    local hdfs_manifest="$hdfs_dir/$day.manifest"

    local paths stored
    paths=$(printf '%s' "${day_paths[$day]}" | sort)
    stored=$(hdfs cat "$hdfs_manifest" 2>/dev/null || true)

    if [[ -n "$stored" && "$stored" == "$paths" ]]; then
        echo "skip    $day"
        return 0
    fi

    local folders
    folders=$(printf '%s\n' "$paths" | grep -c . || true)

    if [[ -n "$stored" ]]; then
        echo "rebuild $day (состав суток изменился)"
    else
        echo "pack    $day (папок: $folders)"
    fi

    rm -rf "$stage_root"
    mkdir -p "$stage_root"

    local src rel
    while read -r src; do
        [[ -n "$src" ]] || continue
        rel="${src#/$hdfs_root/}"
        mkdir -p "$stage_root/$day/$(dirname "$rel")"
        hdfs get "$src" "$stage_root/$day/$rel"
    done <<< "$paths"

    local tarball="$workdir/$day.tar.gz"
    local manifest="$workdir/$day.manifest"
    tar -czf "$tarball" -C "$stage_root" "$day"
    printf '%s' "$paths" > "$manifest"

    "${rclone_cmd[@]}" copyto "$tarball" "$remote/${day:0:7}/$day.tar.gz"

    local local_size remote_size
    local_size=$(stat -c %s "$tarball")
    remote_size=$("${rclone_cmd[@]}" lsf --format s "$remote/${day:0:7}/$day.tar.gz" | head -n1)

    if [[ "$local_size" != "$remote_size" ]]; then
        echo "ОШИБКА: $day.tar.gz на Диске $remote_size Б, локально $local_size Б" >&2
        return 1
    fi

    hdfs mkdir -p "$hdfs_dir"
    hdfs rm "$hdfs_archive" >/dev/null 2>&1 || true
    hdfs rm "$hdfs_manifest" >/dev/null 2>&1 || true
    hdfs put "$tarball" "$hdfs_archive"
    hdfs put "$manifest" "$hdfs_manifest"

    rm -rf "$stage_root" "$tarball" "$manifest"
    echo "done    $day.tar.gz ($(numfmt --to=iec "$local_size"))"
}

collect_days

for day in $(printf '%s\n' "${!day_paths[@]}" | sort); do
    archive_day "$day"
done
