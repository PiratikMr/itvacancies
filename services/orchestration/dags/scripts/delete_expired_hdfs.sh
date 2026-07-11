#!/usr/bin/env bash

execution_date="$1"
expire_days="$2"
hdfs_root="$3"

target=$(date -d "$execution_date - $expire_days days" +%s)

process_path() {
    local search_path="$1"
    local depth="$2"

    if (( depth > 2 )); then return; fi

    local root_path="$search_path"
    hdfs ls "$search_path" | while read -r path; do
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
            process_path "$root_path$path/" $((depth + 1))
        fi
    done
}

process_path "/$hdfs_root/*/Vacancies/" 0
