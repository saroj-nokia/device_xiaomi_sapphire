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

# Include our private certificate
-include vendor/lineage-priv/keys/keys.mk

# Gapps
-include vendor/gapps/arm64/arm64-vendor.mk

# AudioFX
TARGET_EXCLUDES_AUDIOFX := true

# Device configs
TARGET_BOOT_ANIMATION_RES = 1080
TARGET_HAS_UDFPS := true
WITH_GMS := true

PRODUCT_NAME := lineage_sapphire
PRODUCT_DEVICE := sapphire
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := Redmi
PRODUCT_MODEL := Redmi Note 13

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="sapphire_global-user 15 AQ3A.240829.003 OS2.0.210.0.VNGMIXM release-keys" \
    BuildFingerprint=Redmi/sapphire_global/sapphire:15/AQ3A.240829.003/OS2.0.210.0.VNGMIXM:user/release-keys
