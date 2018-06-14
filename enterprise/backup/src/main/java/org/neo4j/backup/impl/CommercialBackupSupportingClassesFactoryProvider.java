package org.neo4j.backup.impl;

public class CommercialBackupSupportingClassesFactoryProvider extends BackupSupportingClassesFactoryProvider {
    public CommercialBackupSupportingClassesFactoryProvider() {
        super("commercial-backup-support-provider");
    }

    public BackupSupportingClassesFactory getFactory(BackupModule backupModule) {
        return new CommercialBackupSupportingClassesFactory(backupModule);
    }

    protected int getPriority() {
        return super.getPriority() + 100;
    }
}