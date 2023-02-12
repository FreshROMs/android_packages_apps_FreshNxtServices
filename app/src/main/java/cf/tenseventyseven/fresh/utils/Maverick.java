package cf.tenseventyseven.fresh.utils;

import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;

public class Maverick {
    public static final String MAVERICK_STATE = "fresh_maverick_state";
    public static final String MAVERICK_CURRENT_STATE = "fresh_maverick_current_state";
    public static final String MAVERICK_ALWAYS_ENABLED = "fresh_maverick_always_enabled";
    public static class MaverickState {
        public static final int OFF = 0;
        public static final int WHEN_LOCKED = 1;
        public static final int ALWAYS = 2;
    }

    public static void setMaverickState(Context context, int newState) {
        if (getMaverickState() == MaverickState.OFF && newState == MaverickState.WHEN_LOCKED && getMaverickAlwaysEnabled(context))
            newState = MaverickState.ALWAYS;

        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_CONFIGURATION,
                MAVERICK_STATE, Integer.toString(newState), true);
        Settings.Secure.putInt(context.getContentResolver(), MAVERICK_STATE, newState);

        if (newState != MaverickState.OFF)
            Settings.Secure.putInt(context.getContentResolver(), MAVERICK_ALWAYS_ENABLED, (newState == MaverickState.ALWAYS) ? 1 : 0);
    }

    public static void setMaverickCurrentState(int newState) {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_CONFIGURATION,
                MAVERICK_CURRENT_STATE, Integer.toString(newState), true);
    }

    public static int getMaverickState() {
        return DeviceConfig.getInt(DeviceConfig.NAMESPACE_CONFIGURATION, MAVERICK_STATE, MaverickState.OFF);
    }

    public static boolean getMaverickAlwaysEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), MAVERICK_ALWAYS_ENABLED, MaverickState.OFF) == 1;
    }

    public static boolean isMaverickEnabled() {
        return getMaverickState() > 0;
    }
}
