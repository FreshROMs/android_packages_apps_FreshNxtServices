package cf.tenseventyseven.fresh.utils;

import android.provider.DeviceConfig;

public class Maverick {
    private static final String MAVERICK_STATE = "fresh_maverick_state";
    static class MaverickState {
        public static final int OFF = 0;
        public static final int WHEN_LOCKED = 1;
        public static final int ALWAYS = 2;
    }

    public static void setMaverickState(int newState) {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_CONFIGURATION,
                MAVERICK_STATE, Integer.toString(newState), true);
    }

    public static int getMaverickState() {
        return DeviceConfig.getInt(DeviceConfig.NAMESPACE_CONFIGURATION, MAVERICK_STATE, MaverickState.OFF);
    }
}
