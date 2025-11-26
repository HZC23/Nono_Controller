package com.hzc.nonocontroller;

public final class Constants {

    // Command prefixes
    public static final String CMD_PREFIX = "CMD:";
    public static final String CMD_SUFFIX = "\n";

    // Actions
    public static final String ACTION_MOVE = "MOVE";
    public static final String ACTION_MODE = "MODE";
    public static final String ACTION_GOTO = "GOTO";
    public static final String ACTION_SPEED = "SPEED";
    public static final String ACTION_SCAN = "SCAN";
    public static final String ACTION_LIGHT = "LIGHT";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_RESUME = "RESUME";
    public static final String ACTION_COMPASS_OFFSET = "COMPASS_OFFSET";
    public static final String ACTION_CALIBRATE = "CALIBRATE";
    public static final String ACTION_LCD = "LCD";

    // Serial Monitor
    public static final int MAX_SERIAL_MONITOR_LINES = 200;


    // Values for MOVE action
    public static final String VALUE_FWD = "FWD";
    public static final String VALUE_BWD = "BWD";
    public static final String VALUE_LEFT = "LEFT";
    public static final String VALUE_RIGHT = "RIGHT";
    public static final String VALUE_STOP = "STOP";

    // Values for MODE action
    public static final String VALUE_AVOID = "AVOID";
    public static final String VALUE_SENTRY = "SENTRY";

    // Values for SCAN action
    public static final String VALUE_START = "START";

    // Values for LIGHT action
    public static final String VALUE_ON = "ON";
    public static final String VALUE_OFF = "OFF";

    // Values for CALIBRATE action
    public static final String VALUE_COMPASS = "COMPASS";

    // Directions for onDirectionalButton
    public static final String DIRECTION_UP = "UP";
    public static final String DIRECTION_DOWN = "DOWN";
    public static final String DIRECTION_LEFT = "LEFT";
    public static final String DIRECTION_RIGHT = "RIGHT";


    public static String buildCommand(String action, String value) {
        return CMD_PREFIX + action + ":" + value + CMD_SUFFIX;
    }

    public static String buildCommand(String action, int value) {
        return CMD_PREFIX + action + ":" + value + CMD_SUFFIX;
    }

    public static String buildCommand(String action, float value) {
        return CMD_PREFIX + action + ":" + value + CMD_SUFFIX;
    }

    public static String buildCommand(String action) {
        return CMD_PREFIX + action + CMD_SUFFIX;
    }
}
