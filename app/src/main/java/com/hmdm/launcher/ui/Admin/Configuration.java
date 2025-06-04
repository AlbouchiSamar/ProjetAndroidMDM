package com.hmdm.launcher.ui.Admin;

public class Configuration {
    private final Integer id;
    private final int customerId;
    private final int configurationId;
    private final String configurationName;
    private final Integer applicationId;
    private final String applicationName;
    private final int action;
    private final boolean showIcon;
    private final boolean remove;
    private final boolean outdated;
    private final String latestVersionText;
    private final String currentVersionText;
    private final boolean notify;
    private final boolean common;

    public Configuration(Integer id, int customerId, int configurationId, String configurationName,
                         Integer applicationId, String applicationName, int action, boolean showIcon,
                         boolean remove, boolean outdated, String latestVersionText, String currentVersionText,
                         boolean notify, boolean common) {
        this.id = id;
        this.customerId = customerId;
        this.configurationId = configurationId;
        this.configurationName = configurationName;
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.action = action;
        this.showIcon = showIcon;
        this.remove = remove;
        this.outdated = outdated;
        this.latestVersionText = latestVersionText;
        this.currentVersionText = currentVersionText;
        this.notify = notify;
        this.common = common;
    }

    public Integer getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int getConfigurationId() { return configurationId; }
    public String getConfigurationName() { return configurationName; }
    public Integer getApplicationId() { return applicationId; }
    public String getApplicationName() { return applicationName; }
    public int getAction() { return action; }
    public boolean isShowIcon() { return showIcon; }
    public boolean isRemove() { return remove; }
    public boolean isOutdated() { return outdated; }
    public String getLatestVersionText() { return latestVersionText; }
    public String getCurrentVersionText() { return currentVersionText; }
    public boolean isNotify() { return notify; }
    public boolean isCommon() { return common; }
}