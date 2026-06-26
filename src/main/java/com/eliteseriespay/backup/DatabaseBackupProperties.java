package com.eliteseriespay.backup;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eliteseriespay.database-backup")
public class DatabaseBackupProperties {

    private String databasePath = "./eliteseriespay.db";
    private String backupDirectory = "./backups";
    private int retentionLimit = 20;
    private boolean startupEnabled = true;

    public String getDatabasePath() {
        return databasePath;
    }

    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    public int getRetentionLimit() {
        return retentionLimit;
    }

    public void setRetentionLimit(int retentionLimit) {
        this.retentionLimit = retentionLimit;
    }

    public boolean isStartupEnabled() {
        return startupEnabled;
    }

    public void setStartupEnabled(boolean startupEnabled) {
        this.startupEnabled = startupEnabled;
    }
}
