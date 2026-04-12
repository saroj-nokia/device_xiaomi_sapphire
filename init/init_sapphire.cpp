/*
 * Copyright (C) 2023 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <cstdlib>
#include <fstream>
#include <string.h>
#include <unistd.h>
#include <vector>

#include <android-base/properties.h>
#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <sys/_system_properties.h>
#include <sys/sysinfo.h>

#include "property_service.h"
#include "vendor_init.h"

using android::base::GetProperty;
using std::string;

std::vector<std::string> ro_props_default_source_order = {
    "",
    "odm.",
    "product.",
    "system.",
    "system_dlkm.",
    "system_ext.",
    "vendor.",
    "vendor_dlkm.",
};

void property_override(string prop, string value)
{
    auto pi = (prop_info*) __system_property_find(prop.c_str());

    if (pi != nullptr)
        __system_property_update(pi, value.c_str(), value.size());
    else
        __system_property_add(prop.c_str(), prop.size(), value.c_str(), value.size());
}

void load_redmi_sapphire() {
    property_override("bluetooth.device.default_name", "Redmi Note 13");
    property_override("ro.product.brand", "Redmi");
    property_override("ro.product.device", "sapphire");
    property_override("ro.product.manufacturer", "Xiaomi");
    property_override("ro.product.marketname", "Redmi Note 13");
    property_override("ro.product.model", "23129RAA4G");
    property_override("ro.product.mod_device", "sapphire_global");
    property_override("ro.product.name", "sapphire_global");
    property_override("vendor.usb.product_string", "Redmi Note 13");
}

void load_redmi_sapphiren() {
    property_override("bluetooth.device.default_name", "Redmi Note 13 NFC");
    property_override("ro.product.brand", "Redmi");
    property_override("ro.product.device", "sapphiren");
    property_override("ro.product.manufacturer", "Xiaomi");
    property_override("ro.product.marketname", "Redmi Note 13 NFC");
    property_override("ro.product.model", "23124RA7EO");
    property_override("ro.product.mod_device", "sapphiren_global");
    property_override("ro.product.name", "sapphiren_global");
    property_override("vendor.usb.product_string", "Redmi Note 13 NFC");
}

void vendor_load_properties() {
    std::string hwname = GetProperty("ro.boot.hwname", "");
    if (access("/system/bin/recovery", F_OK) != 0) {
    if (hwname == "sapphire") {
        load_redmi_sapphire();
    } else if (hwname == "sapphiren") {
        load_redmi_sapphiren();
       }
    }

    // Set hardware revision
    property_override("ro.boot.hardware.revision", GetProperty("ro.boot.hwversion", "").c_str());

    // Set dalvik heap configuration
    std::string heapstartsize, heapgrowthlimit, heapsize, heapminfree,
			heapmaxfree, heaptargetutilization;

    struct sysinfo sys;
    sysinfo(&sys);

    if (sys.totalram > 12288ull * 1024 * 1024) {
        // from - phone-xhdpi-16384-dalvik-heap.mk
        heapstartsize = "32m";
        heapgrowthlimit = "512m";
        heapsize = "768m";
        heaptargetutilization = "0.3";
        heapminfree = "8m";
        heapmaxfree = "16m";
    } else if (sys.totalram > 8192ull * 1024 * 1024) {
        // from - phone-xhdpi-12288-dalvik-heap.mk
        heapstartsize = "24m";
        heapgrowthlimit = "448m";
        heapsize = "768m";
        heaptargetutilization = "0.35";
        heapminfree = "8m";
        heapmaxfree = "16m";
    } else if (sys.totalram > 6144ull * 1024 * 1024) {
        // from - phone-xhdpi-8192-dalvik-heap.mk
        heapstartsize = "16m";
        heapgrowthlimit = "384m";
        heapsize = "512m";
        heaptargetutilization = "0.4";
        heapminfree = "8m";
        heapmaxfree = "16m";
    } else if (sys.totalram > 4096ull * 1024 * 1024) {
        // from - phone-xhdpi-6144-dalvik-heap.mk
        heapstartsize = "16m";
        heapgrowthlimit = "320m";
        heapsize = "512m";
        heaptargetutilization = "0.5";
        heapminfree = "8m";
        heapmaxfree = "16m";
    } else if (sys.totalram > 2048ull * 1024 * 1024) {
        // from - phone-xhdpi-4096-dalvik-heap.mk
        heapstartsize = "8m";
        heapgrowthlimit = "256m";
        heapsize = "512m";
        heaptargetutilization = "0.6";
        heapminfree = "8m";
        heapmaxfree = "16m";
    } else if (sys.totalram > 1024ull * 1024 * 1024) {
        // from - phone-xhdpi-2048-dalvik-heap.mk
        heapstartsize = "8m";
        heapgrowthlimit = "192m";
        heapsize = "512m";
        heaptargetutilization = "0.75";
        heapminfree = "512k";
        heapmaxfree = "8m";
    } else {
        // from - phone-xhdpi-1024-dalvik-heap.mk
        heapstartsize = "8m";
        heapgrowthlimit = "64m";
        heapsize = "128m";
        heaptargetutilization = "0.75";
        heapminfree = "512k";
        heapmaxfree = "8m";
    }

    property_override("dalvik.vm.heapstartsize", heapstartsize);
    property_override("dalvik.vm.heapgrowthlimit", heapgrowthlimit);
    property_override("dalvik.vm.heapsize", heapsize);
    property_override("dalvik.vm.heaptargetutilization", heaptargetutilization);
    property_override("dalvik.vm.heapminfree", heapminfree);
    property_override("dalvik.vm.heapmaxfree", heapmaxfree);
}
