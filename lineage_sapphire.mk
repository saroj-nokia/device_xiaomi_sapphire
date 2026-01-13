#
# Copyright (C) 2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from sapphire device
$(call inherit-product, device/xiaomi/sapphire/device.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

# EvolutionX Config
TARGET_BOOT_ANIMATION_RES := 1080
TARGET_BUILD_APERTURE_CAMERA := false
TARGET_DISABLE_EPPE := true
TARGET_HAS_UDFPS := true

# Extra stuff (From A14 cfg)
TARGET_SUPPORTS_QUICK_TAP := true
TARGET_ENABLE_BLUR := true
TARGET_USES_MINI_GAPPS := true

PRODUCT_NAME := lineage_sapphire
PRODUCT_DEVICE := sapphire
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := Redmi
PRODUCT_MODEL := Redmi Note 13

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

BUILD_FINGERPRINT := Redmi/sapphiren_global/sapphire:13/TKQ1.221114.001/OS2.0.207.0.VNGMIXM:user/release-keys
