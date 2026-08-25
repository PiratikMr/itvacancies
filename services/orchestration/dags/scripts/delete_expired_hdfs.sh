#!/usr/bin/env bash

execution_date="$1"
expire_days="$2"
hdfs_root="$3"

target=$(date -d "$execution_date - $expire_days days" +%s)

process_path() {
    local depth="$1"
    shift

    if (( depth > 2 )); then return; fi

    local root_path="$1"
    hdfs ls "$@" | while read -r path; do
        if [[ -z "$path" ]]; then
            continue
        elif [[ $path == /* ]]; then
            root_path="${path::-1}"
        elif [[ $path == ????-??-??* ]]; then
            local date_parts="${path:0:10}"
            local folder_ts
            folder_ts=$(date -d "$date_parts" +%s 2>/dev/null) || continue

            if (( folder_ts < target )); then
                hdfs rm -r "$root_path$path"
                echo "deleted $root_path$path"
            fi
        else
            process_path $((depth + 1)) "$root_path$path/"
        fi
    done
}

process_path 0 "/$hdfs_root/*/Vacancies/" "/$hdfs_root/*/Rates/"
