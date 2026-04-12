#!/vendor/bin/sh

# Copyright (C) 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0

zram_size=$(getprop persist.vendor.zram.size)

# If not set, use default (managed by post_boot script or fixed value)
if [ -z "$zram_size" ]; then
    exit 0
fi

# Disable swap
swapoff /dev/block/zram0 > /dev/null 2>&1

# Reset disksize
echo 1 > /sys/block/zram0/reset

# If size is 0, we just want it off
if [ "$zram_size" = "0" ]; then
    echo "ZRAM disabled"
    exit 0
fi

# Set algorithm (try zstd, fallback to lz4)
if grep -q zstd /sys/block/zram0/comp_algorithm; then
    echo zstd > /sys/block/zram0/comp_algorithm
else
    echo lz4 > /sys/block/zram0/comp_algorithm
fi

# Set size (in MB)
echo "${zram_size}M" > /sys/block/zram0/disksize

# Initialize and enable
mkswap /dev/block/zram0 > /dev/null 2>&1
swapon /dev/block/zram0 -p 32758 > /dev/null 2>&1

echo "ZRAM set to ${zram_size}M"
