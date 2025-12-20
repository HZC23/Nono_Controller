package com.hzc.nonocontroller;

public final class Constants {

    // Command suffixes
    public static final String CMD_SUFFIX = "\n"; // Still used for all commands

    // Prefixes for app_guide.md commands
    public static final String S_PREFIX = "S:";
    public static final String M_PREFIX = "M:";
    public static final String HL_PREFIX = "HL:";
    public static final String MUSIC_PREFIX = "MUSIC:";
    public static final String AVOID_COMMAND = "AVOID"; // Special case command without prefix
    public static final String SENTRY_COMMAND = "SENTRY"; // Special case command without prefix
    public static final String SCAN3D_COMMAND = "SCAN3D"; // Special case command without prefix

    // Actions (from app_guide.md where applicable, otherwise existing)
    // NOTE: Many of the previous ACTION_ constants are now implied by the prefixes or
    // are directly the command itself (e.g., AVOID, SENTRY).
    public static final String ACTION_GOTO_HEADING = "FOLLOW_HEADING"; // From telemetry states, needs mapping
    public static final String ACTION_SCAN_3D = "SCAN3D"; // For 3D scan mode


    // Response prefixes
    public static final String RSP_PREFIX = "RSP:";
    public static final String RSP_MUSIC_FILE_PREFIX = RSP_PREFIX + "MUSIC_FILE:";

    // Serial Monitor
    public static final int MAX_SERIAL_MONITOR_LINES = 200;


    // Values for commands
    public static final String VALUE_ON = "ON";
    public static final String VALUE_OFF = "OFF";
    public static final String VALUE_PLAY = "PLAY";
    public static final String VALUE_STOP = "STOP";

    // Values for CALIBRATE action
    public static final String VALUE_COMPASS = "COMPASS"; // This is likely specific to a 'CMD:' format.

    // Commands that existed in the app but not in the original guide directly, keep for now.
    public static final String ACTION_COMPASS_OFFSET = "COMPASS_OFFSET";
    public static final String ACTION_CALIBRATE_COMPASS = "CALIBRATE:COMPASS"; // Original was CMD:CALIBRATE:COMPASS
    public static final String ACTION_LCD = "LCD";
    public static final String ACTION_ANIM = "ANIM";


    /**
     * Builds a movement command in the format V:velocity;D:direction\n
     * @param velocity A value from -100 to 100.
     * @param direction A value from -100 to 100.
     * @return The formatted movement command string.
     */
    public static String buildMovementCommand(int velocity, int direction) {
        return "V:" + velocity + ";D:" + direction + CMD_SUFFIX;
    }

    /**
     * Builds a command with a prefix and a value.
     * For example: "S:200\n", "HL:ON\n", "MUSIC:PLAY\n"
     * @param prefix The command prefix (e.g., S_PREFIX, M_PREFIX, HL_PREFIX, MUSIC_PREFIX).
     * @param value The value for the command.
     * @return The formatted command string.
     */
    public static String buildPrefixedCommand(String prefix, String value) {
        return prefix + value + CMD_SUFFIX;
    }
    
    /**
     * Builds a simple command without a value, typically for mode changes.
     * For example: "AVOID\n", "SENTRY\n", "SCAN3D\n"
     * @param command The command string itself.
     * @return The formatted command string.
     */
    public static String buildSimpleCommand(String command) {
        return command + CMD_SUFFIX;
    }

    // Keep some buildCommand overloads for other specific commands from the app that don't fit the new pattern easily.
    public static String buildCommand(String action, String value) {
        // This will be used for CMD:ACTION:VALUE type commands that might still exist outside the main guide commands.
        // Example: CMD:LCD:Hello World\n, CMD:ANIM:YES\n
        return "CMD:" + action + ":" + value + CMD_SUFFIX;
    }

    public static String buildCommand(String action, int value) {
        return "CMD:" + action + ":" + value + CMD_SUFFIX;
    }

    public static String buildCommand(String action, float value) {
        return "CMD:" + action + ":" + value + CMD_SUFFIX;
    }
    
    public static String buildCommand(String action) {
        return "CMD:" + action + CMD_SUFFIX;
    }
}