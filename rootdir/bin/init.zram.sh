#!/vendor/bin/sh

# Copyright (C) 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0

# Pega o valor enviado como argumento pelo init.rc
zram_size=$1

# Se o argumento estiver vazio, encerra
if [ -z "$zram_size" ]; then
    exit 0
fi

echo "ZRAM: Requested size ${zram_size}M" > /dev/kmsg

# Desativa a swap atual
swapoff /dev/block/zram0 > /dev/null 2>&1

# Reseta o dispositivo zram
echo 1 > /sys/block/zram0/reset

# Se o valor for 0, apenas desativa e sai
if [ "$zram_size" = "0" ]; then
    echo "ZRAM: Disabled" > /dev/kmsg
    exit 0
fi

# Define o algoritmo de compressão
if grep -q lzo-rle /sys/block/zram0/comp_algorithm; then
    echo lzo-rle > /sys/block/zram0/comp_algorithm
elif grep -q zstd /sys/block/zram0/comp_algorithm; then
    echo zstd > /sys/block/zram0/comp_algorithm
else
    echo lz4 > /sys/block/zram0/comp_algorithm
fi

# Define o tamanho (em MB)
echo "${zram_size}M" > /sys/block/zram0/disksize

# Inicializa e ativa a swap
mkswap /dev/block/zram0 > /dev/null 2>&1
swapon /dev/block/zram0 -p 32758 > /dev/null 2>&1

echo "ZRAM: Set to ${zram_size}M" > /dev/kmsg
