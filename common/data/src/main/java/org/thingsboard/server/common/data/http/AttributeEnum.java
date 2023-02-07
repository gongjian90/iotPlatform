/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   DataAttributeVO.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/7
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.common.data.http;


import lombok.Getter;

public enum AttributeEnum {

    VOLTAGE("47", "V", "float", "battery","/Dc/0/Voltage","%.2f", "%.2f V", "Voltage"),
    CURRENT("49", "I", "float", "battery","/Dc/0/Current","%.2f", "%.2f A", "Current"),
    CONSUMED_AMPHOURS("50", "CE", "float", "battery","/ConsumedAmphours","%.2f", "%.2f Ah", "Consumed Amphours"),
    STATE_OF_CHARGE("51", "SOC", "float", "battery","/Soc","%.1f", "%.1f %%", "State of charge"),
    TIME_TO_GO("52", "TTG", "float", "battery","/TimeToGo","%.2f", "%.2f h", "Time to go"),
    BATTERY_TEMPERATURE("115", "BT", "float", "battery","/Dc/0/Temperature","%.0f", "%.0f °C", "Battery temperature"),
    LOW_VOLTAGE_ALARM("119", "AL", "enum", "battery","/Alarms/LowVoltage", "%s",  "%s", "Low voltage alarm"),
    HIGH_VOLTAGE_ALARM("120", "AH", "enum", "battery","/Alarms/HighVoltage", "%s",  "%s", "High voltage alarm"),
    LOW_BATTERY_TEMPERATURE_ALARM("124", "ALT", "enum", "battery","/Alarms/LowTemperature", "%s", "%s", "Low battery temperature alarm"),
    HIGH_BATTERY_TEMPERATURE_ALARM("125", "AHT", "enum", "battery","/Alarms/HighTemperature", "%s", "%s", "High battery temperature alarm"),
    MINIMUM_CELL_VOLTAGE("173", "mcV", "float", "bms","/System/MinCellVoltage", "%.2f", "%.2f V", "Minimum cell voltage"),
    MAXIMUM_CELL_VOLTAGE("174", "McV", "float", "bms","/System/MaxCellVoltage", "%.2f", "%.2f V", "Maximum cell voltage"),
    default_("", "", "", "", "","","", "");



    @Getter
    private final String idDataAttribute;
    @Getter
    private final String code;
    @Getter
    private final String dataType;
    @Getter
    private final String dbusServiceType;
    @Getter
    private final String dbusPath;
    @Getter
    private final String formatValueOnly;
    @Getter
    private final String formatWithUnit;
    @Getter
    private final String dataAttributeName;

    AttributeEnum(String idDataAttribute, String code, String dataType, String dbusServiceType, String dbusPath,
                  String formatValueOnly, String formatWithUnit, String dataAttributeName){
        this.idDataAttribute = idDataAttribute;
        this.code = code;
        this.dataType = dataType;
        this.dbusServiceType = dbusServiceType;
        this.dbusPath = dbusPath;
        this.formatValueOnly = formatValueOnly;
        this.formatWithUnit = formatWithUnit;
        this.dataAttributeName = dataAttributeName;
    }
}
