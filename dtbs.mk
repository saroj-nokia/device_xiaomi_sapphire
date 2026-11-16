#
# Copyright (C) 2026 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# dtb.img and dtbo.img for sapphire / sapphiren, built from
# kernel/xiaomi/sapphire-devicetrees with its build.sh.
#
# vendor/lineage/build/tools/merge_dtbs.py merges the base DTBs with
# fdtoverlay, which inserts the subnodes an overlay adds in reverse order;
# the camera CPAS driver (camera-bus-nodes) and the kgsl power levels depend
# on the stock order. build.sh merges them with libufdt like the stock build.
#
# Included by kernel.mk as both BOARD_CUSTOM_DTBIMG_MK and
# BOARD_CUSTOM_DTBOIMG_MK.

ifndef SAPPHIRE_DTBS_MK_INCLUDED
SAPPHIRE_DTBS_MK_INCLUDED := true

SAPPHIRE_DT_SRC := kernel/xiaomi/sapphire-devicetrees
SAPPHIRE_DT_OUT := $(TARGET_OUT_INTERMEDIATES)/SAPPHIRE_DT_OBJ

SAPPHIRE_DT_TOOLS := \
    $(HOST_OUT_EXECUTABLES)/dtc \
    $(HOST_OUT_EXECUTABLES)/fdtget \
    $(HOST_OUT_EXECUTABLES)/fdtoverlay \
    $(HOST_OUT_EXECUTABLES)/fdtoverlaymerge \
    $(HOST_OUT_EXECUTABLES)/fdtput \
    $(HOST_OUT_EXECUTABLES)/mkdtboimg \
    $(HOST_OUT_EXECUTABLES)/ufdt_apply_overlay

SAPPHIRE_DT_INPUTS := $(shell find $(SAPPHIRE_DT_SRC) -type f \
    -not -path '*/.git/*' -not -path '*/out/*' 2>/dev/null)

$(SAPPHIRE_DT_OUT)/dtb.img: $(SAPPHIRE_DT_TOOLS) $(SAPPHIRE_DT_INPUTS)
	@echo "Building sapphire dtb.img and dtbo.img"
	$(hide) rm -rf $(SAPPHIRE_DT_OUT)
	$(hide) mkdir -p $(SAPPHIRE_DT_OUT)
	$(hide) DTC=$(abspath $(HOST_OUT_EXECUTABLES)/dtc) \
		FDTGET=$(abspath $(HOST_OUT_EXECUTABLES)/fdtget) \
		FDTOVERLAY=$(abspath $(HOST_OUT_EXECUTABLES)/fdtoverlay) \
		FDTOVERLAYMERGE=$(abspath $(HOST_OUT_EXECUTABLES)/fdtoverlaymerge) \
		FDTPUT=$(abspath $(HOST_OUT_EXECUTABLES)/fdtput) \
		MKDTBOIMG=$(abspath $(HOST_OUT_EXECUTABLES)/mkdtboimg) \
		UFDT_APPLY_OVERLAY=$(abspath $(HOST_OUT_EXECUTABLES)/ufdt_apply_overlay) \
		CPP="$(abspath $(TARGET_KERNEL_CLANG_PATH))/bin/clang -E" \
		KERNEL_DIR=$(abspath $(TARGET_KERNEL_SOURCE)) \
		CAMERA_KERNEL_DIR=$(abspath $(TARGET_KERNEL_EXT_MODULE_ROOT))/qcom/opensource/camera-kernel \
		OUT=$(abspath $(SAPPHIRE_DT_OUT)) \
		bash $(SAPPHIRE_DT_SRC)/build.sh

$(SAPPHIRE_DT_OUT)/dtbo.img: $(SAPPHIRE_DT_OUT)/dtb.img

$(INSTALLED_DTBIMAGE_TARGET): $(SAPPHIRE_DT_OUT)/dtb.img
	$(hide) mkdir -p $(dir $@)
	cp $< $@

$(BOARD_PREBUILT_DTBOIMAGE): $(SAPPHIRE_DT_OUT)/dtbo.img
	$(hide) mkdir -p $(dir $@)
	cp $< $@

endif # SAPPHIRE_DTBS_MK_INCLUDED
