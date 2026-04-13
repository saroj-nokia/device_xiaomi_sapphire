#!/vendor/bin/sh

# Copyright (C) 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0

zram_size=$(getprop persist.vendor.zram.size)

# If not set, use default (managed by post_boot script or fixed value)
if [ -z "$zram_size" ]; then
    exit 0
fi

echo "ZRAM: Requested size ${zram_size}M" > /dev/kmsg

# Disable swap
swapoff /dev/block/zram0 > /dev/null 2>&1

# Reset disksize
echo 1 > /sys/block/zram0/reset

# If size is 0, we just want it off
if [ "$zram_size" = "0" ]; then
    echo "ZRAM: Disabled" > /dev/kmsg
    exit 0
fi

# Set algorithm (prefer lzo-rle, then zstd, then lz4)
if grep -q lzo-rle /sys/block/zram0/comp_algorithm; then
    echo lzo-rle > /sys/block/zram0/comp_algorithm
elif grep -q zstd /sys/block/zram0/comp_algorithm; then
    echo zstd > /sys/block/zram0/comp_algorithm
else
    echo lz4 > /sys/block/zram0/comp_algorithm
fi

# Set size (in MB)
echo "${zram_size}M" > /sys/block/zram0/disksize

# Initialize and enable
mkswap /dev/block/zram0 > /dev/null 2>&1
swapon /dev/block/zram0 -p 32758 > /dev/null 2>&1

echo "ZRAM: Set to ${zram_size}M" > /dev/kmsg
