/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Turns the FocalTech touch IC "game mode" on or off through the Xiaomi
 * touch interface (/dev/xiaomi-touch), the way MIUI's Game Turbo does.
 *
 * The state comes from vendor.touch.game_mode: a value starting with '1'
 * means on, anything else means off. TouchGrip sets it when a game takes
 * or leaves the foreground, and again after every screen on, because the
 * IC forgets these registers when the panel suspends.
 *
 * Only Touch_Game_Mode and Touch_Active_MODE are changed. Active mode
 * keeps the IC scanning at full rate instead of dropping to its idle
 * scan, which is what makes the first touch after a pause respond late.
 * Thresholds, tolerance and edge filtering stay at the driver defaults.
 */

#define LOG_TAG "touch_game_mode"

#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/system_properties.h>
#include <unistd.h>

#include <log/log.h>

#define TOUCH_DEV_PATH "/dev/xiaomi-touch"
#define TOUCH_MAGIC 'T'
#define SET_CUR_VALUE 0
#define TOUCH_IOC_SET_CUR_VALUE _IO(TOUCH_MAGIC, SET_CUR_VALUE)
/* The driver always copies MAX_BUF_SIZE ints in and out */
#define MAX_BUF_SIZE 256

#define TOUCH_GAME_MODE 0
#define TOUCH_ACTIVE_MODE 1

#define PROP_GAME_MODE "vendor.touch.game_mode"

static int set_mode(int fd, int mode, int value) {
    int buf[MAX_BUF_SIZE];

    memset(buf, 0, sizeof(buf));
    /* buf[0] is the display id, buf[1] the mode, buf[2] the value */
    buf[1] = mode;
    buf[2] = value;
    if (ioctl(fd, TOUCH_IOC_SET_CUR_VALUE, buf) < 0) {
        ALOGE("Failed to set touch mode %d to %d: %s", mode, value, strerror(errno));
        return -1;
    }
    return 0;
}

static int apply(int fd, int enable) {
    int ret = 0;

    if (enable) {
        /* Game mode first: the driver only applies mode registers while it is on */
        ret |= set_mode(fd, TOUCH_GAME_MODE, 1);
        ret |= set_mode(fd, TOUCH_ACTIVE_MODE, 1);
    } else {
        ret |= set_mode(fd, TOUCH_ACTIVE_MODE, 0);
        ret |= set_mode(fd, TOUCH_GAME_MODE, 0);
    }
    ALOGI("Touch game mode %s%s", enable ? "on" : "off", ret ? " (with errors)" : "");
    return ret;
}

int main(void) {
    char applied[PROP_VALUE_MAX] = {0};
    char value[PROP_VALUE_MAX] = {0};
    int fd;
    int ret = 0;
    int i;

    fd = open(TOUCH_DEV_PATH, O_RDWR | O_CLOEXEC);
    if (fd < 0) {
        ALOGE("Failed to open %s: %s", TOUCH_DEV_PATH, strerror(errno));
        return 1;
    }

    /*
     * init does not restart a oneshot service that is still running, so a
     * change made while we run would be lost. Re-read the property until it
     * stops changing (bounded, in case it flips back and forth).
     */
    for (i = 0; i < 5; i++) {
        __system_property_get(PROP_GAME_MODE, value);
        if (i > 0 && strcmp(value, applied) == 0) break;
        ret = apply(fd, value[0] == '1');
        memcpy(applied, value, sizeof(applied));
    }
    close(fd);

    return ret ? 1 : 0;
}
