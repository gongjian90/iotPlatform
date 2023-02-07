package org.thingsboard.server.common.data.http;

import lombok.Getter;

public enum MetaEnum {

    VOLTAGE("47", "V", "Voltage", "%.2f", "%.2f V", "Battery Voltage"),
    STARTER_BATTERY_VOLTAGE("48", "VS", "Starter battery voltage", "%.2f", "%.2f V", null),
    CURRENT("49", "I", "Current", "%.2f", "%.2f A", "Battery Current"),
    CONSUMED_AMPHOURS("50", "CE", "Consumed Amphours", "%.2f", "%.2f Ah", null),
    STATE_OF_CHARGE("51", "SOC", "State of charge", "%.1f", "%.1f %%", null),
    TIME_TO_GO("52", "TTG", "Time to go", "%.2f", "%.2f h", null),
    RELAY_STATUS("54", "Relay", "Relay status", "%.s", "%.s", null),
    BATTERY_TEMPERATURE("115", "BT", "Battery temperature", "%.0F", "%.0F \\u00b0C", null),
    MID_POINT_VOLTAGE("116", "VM", "Mid-point voltage of the battery bank", "%.2f", "%.2f V", "Battery Voltage"),
    MID_POINT_DEVIATION("117", "VMD", "Mid-point deviation of the battery bank", "%.1f", "%.1f %%", null),
    LOW_VOLTAGE_ALARM("119", "AL", "Low voltage alarm", "%.s", "%.s", null),
    HIGH_VOLTAGE_ALARM("120", "AH", "High voltage alarm", "%.s", "%.s", null),
    LOW_STARTER_VOLTAGE_ALARM("121", "ALS", "Low starter-voltage alarm", "%.s", "%.s", null),
    HIGH_STARTER_VOLTAGE_ALARM("122", "AHS", "High starter-voltage alarm", "%.s", "%.s", null),
    LOW_STATE_OF_CHARGE_ALARM("123", "ASoc", "Low state-of-charge alarm", "%.s", "%.s", null),
    LOW_BATTERY_TEMPERATURE_ALARM("124", "ALT", "Low battery temperature alarm", "%.s", "%.s", null),
    HIGH_BATTERY_TEMPERATURE_ALARM("125", "AHT", "High battery temperature alarm", "%.s", "%.s", null),
    MID_VOLTAGE_ALARM("126", "AM", "Mid-voltage alarm", "%.s", "%.s", null),
    LOW_FUSED_VOLTAGE_ALARM("155", "ALF", "Low fused-voltage alarm", "%.s", "%.s", null),
    HIGH_FUSED_VOLTAGE_ALARM("156", "AHF", "High fused-voltage alarm", "%.s", "%.s", null),
    FUSE_BLOWN_ALARM("157", "AFB", "Fuse blown alarm", "%.s", "%.s", null),
    HIGH_INTERNAL_TEMPERATURE_ALARM("158", "AHIT", "High internal-temperature alarm", "%.s", "%.s", null),
    MINIMUM_CELL_VOLTAGE("173", "mcV", "Minimum cell voltage", "%.2f", "%.2f V", null),
    MAXIMUM_CELL_VOLTAGE("174", "McV", "Maximum cell voltage", "%.2f", "%.2f V", null),
    EXTERNAL_RELAY("182", "eRelay", "IO; external relay", "%.s", "%.s", null),
    CHARGE_VOLTAGE_LIMIT("274", "mvc", "CVL - Charge Voltage Limit", "%.1f", "%.1f V", "Battery Voltage"),
    CHARGE_CURRENT_LIMIT("276", "mcc", "CCL - Charge Current Limit", "%.1f", "%.1f A", "Battery Current"),
    DISCHARGE_CURRENT_LIMIT("277", "mdc", "DCL - Discharge Current Limit", "%.1f", "%.1f A", "Battery Current"),
    CELL_IMBALANCE_ALARM("286", "ACI", "Cell Imbalance alarm", "%.s", "%.s", null),
    HIGH_CHARGE_CURRENT_ALARM("287", "AHC", "High charge current alarm", "%.s", "%.s", null),
    HIGH_DISCHARGE_CURRENT_ALARM("288", "AHD", "High discharge current alarm", "%.s", "%.s", null),
    INTERNAL_ERROR_ALARM("289", "AIE", "Internal error alarm", "%.s", "%.s", null),
    HIGH_CHARGE_TEMPERATURE_ALARM("459", "AHCT", "High charge temperature alarm", "%.s", "%.s", null),
    LOW_CHARGE_TEMPERATURE_ALARM("460", "ALCT", "Low charge temperature alarm", "%.s", "%.s", null),
    LOW_CELL_VOLTAGE("522", "ALCV", "Low cell voltage", "%.s", "%.s", null),
    CHARGE_BLOCKED("739", "Abc", "Charge blocked", "%.s", "%.s", null),
    DISCHARGE_BLOCKED("740", "Abd", "Discharge blocked", "%.s", "%.s", null),
    DEFAULT("", "", "", "", "", "");


    @Getter
    private final String idDataAttribute;
    @Getter
    private final String code;
    @Getter
    private final String description;
    @Getter
    private final String formatValueOnly;
    @Getter
    private final String formatWithUnit;
    @Getter
    private final String axisGroup;

    MetaEnum(String idDataAttribute, String code, String description, String formatValueOnly, String formatWithUnit, String axisGroup) {
        this.idDataAttribute = idDataAttribute;
        this.code = code;
        this.description = description;
        this.formatValueOnly = formatValueOnly;
        this.formatWithUnit = formatWithUnit;
        this.axisGroup = axisGroup;
    }
}
