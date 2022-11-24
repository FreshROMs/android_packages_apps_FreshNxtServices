package io.tenseventyseven.fresh.ota;

import android.annotation.SuppressLint;
import android.text.format.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.tenseventyseven.fresh.utils.Tools;

public class SoftwareUpdate {
    // OTA install state
    public static final int OTA_INSTALL_STATE_NOT_STARTED = -1;
    public static final int OTA_INSTALL_STATE_DOWNLOADED = 1;
    public static final int OTA_INSTALL_STATE_DOWNLOADING = 2;
    public static final int OTA_INSTALL_STATE_FAILED = 3;
    public static final int OTA_INSTALL_STATE_PAUSED = 4;
    public static final int OTA_INSTALL_STATE_CANCELLED = 5;
    public static final int OTA_INSTALL_STATE_VERIFYING = 6;
    public static final int OTA_INSTALL_STATE_FAILED_VERIFICATION = 7;
    public static final int OTA_INSTALL_STATE_LOST_CONNECTION = 8;
    public static final int OTA_INSTALL_STATE_INSTALLING = 9;
    public static final int OTA_INSTALL_STATE_SUCCESS = 10;
    public static final int OTA_INSTALL_STATE_UNKNOWN = 11;

    private long dateTime;
    private long versionCode;
    private String versionName;
    private String spl;
    private String md5Hash;
    private String releaseType;
    private String fileUrl;
    private long fileSize;
    private String changelog;
    private String updatedApps;
    private String downloadId;
    private int response = 0;

    public int getResponse() {
        return response;
    }

    public void setResponse(int response) {
        this.response = response;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public long getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(long versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getSpl() {
        return spl;
    }

    public void setSpl(String spl) {
        this.spl = spl;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public String getReleaseType() {
        return releaseType;
    }

    public void setReleaseType(String releaseType) {
        this.releaseType = releaseType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public String getUpdatedApps() {
        return updatedApps;
    }

    public void setUpdatedApps(String updatedApps) {
        this.updatedApps = updatedApps;
    }

    public String getFullVersion() {
        return String.format("%s/%s/%s", versionCode, dateTime, releaseType);
    }

    public String getFormattedReleaseType() {
        return Tools.capitalizeString(releaseType);
    }

    public String getFormattedVersion() {
        if (releaseType.equalsIgnoreCase("release"))
            return String.format("%s", versionName);
        else
            return String.format("%s %s", versionName, getFormattedReleaseType());
    }

    public String getReleaseName() {
        if (releaseType.equalsIgnoreCase("release"))
            return String.format("%s %s", "Fresh", versionName);
        else
            return String.format("%s %s %s", "Fresh", versionName, getFormattedReleaseType());
    }

    @SuppressLint("SimpleDateFormat")
    public String getSplString() {
        if (!"".equals(spl)) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchDate = template.parse(spl);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                spl = DateFormat.format(format, patchDate).toString();
            } catch (ParseException e) {
                // broken parse; fall through and use the raw string
            }
            return spl;
        } else {
            return null;
        }
    }

    public String getFileSizeFormat() {
        return UpdateUtils.getFormattedFileSize(fileSize);
    }

    public String getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(String downloadId) {
        this.downloadId = downloadId;
    }
}
