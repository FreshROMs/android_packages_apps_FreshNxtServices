package io.tensevntysevn.fresh.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import io.tensevntysevn.fresh.ExperienceUtils;

public class OverlayService {

    private static final String mOverlayService = "cmd overlay ";

    public static void setOverlayState(@NonNull String packageName, Boolean enable) {
        final String serviceMode = enable ? "enable " : "disable ";
        Log.i("FRSH/OverlayService", "Setting package: "+packageName+" to "+enable.toString());

        try {
            Runtime.getRuntime().exec(mOverlayService + serviceMode + packageName);
        } catch (Exception e) {
            Log.i("FRSH/OverlayService", "Failed to set state!");
            e.printStackTrace();
        }
    }

    public static boolean getOverlayState(Context context, @NonNull String packageName) {
        final String serviceMode = "list ";
        boolean overlayState = false;
        char[] buffer = new char[4096];
        StringBuilder shellBuffer = new StringBuilder();
        int read;
        String shellResp;

        boolean packageInstalled = ExperienceUtils.isPackageInstalled(context, packageName);

        if (packageInstalled) {
            try {
                Process shell = Runtime.getRuntime().exec(mOverlayService + serviceMode + packageName);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(shell.getInputStream()));

                while ((read = reader.read(buffer)) > 0) {
                    shellBuffer.append(buffer, 0, read);
                }

                reader.close();

                shell.waitFor();
                shellResp = shellBuffer.toString();
                String enabledX = Character.toString(shellResp.charAt(1));

                if (enabledX.contains("x")) {
                    overlayState = true;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return overlayState;
    }
}
