#
# Copyright (C) 2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from sapphire device
$(call inherit-product, device/xiaomi/sapphire/device.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Gapps
$(call inherit-product-if-exists, vendor/gapps/gms_full.mk)

# Device configs
TARGET_BOOT_ANIMATION_RES = 1080
TARGET_HAS_UDFPS := true

# Inherit some common crDroid stuff
TARGET_FACE_UNLOCK_SUPPORTED := true
TARGET_SUPPORTS_QUICK_TAP  := true
TARGET_SUPPORTS_CALL_RECORDING := true

PRODUCT_NAME := lineage_sapphire
PRODUCT_DEVICE := sapphire
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := Redmi
PRODUCT_MODEL := Redmi Note 13

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi
