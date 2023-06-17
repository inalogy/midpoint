package com.evolveum.midpoint.ninja.action.upgrade;

public class UpgradeConstants {

    public static final String SUPPORTED_VERSION_LTS = "4.4.5";

    public static final String SUPPORTED_VERSION_FEATURE = "4.7.1";

    public static final String SUPPORTED_VERSION_TARGET = "latest";   // todo change to 4.8

    public static final String[] SUPPORTED_VERSIONS = { SUPPORTED_VERSION_LTS, SUPPORTED_VERSION_FEATURE, "4.8-SNAPSHOT" }; // todo remove viliam

    public static final String UPGRADE_TEMP_DIRECTORY = ".upgrade";
}