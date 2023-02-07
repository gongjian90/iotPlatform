/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   TelemetryKeysConstants.java
 * Author: gongjian   ID:     Version:    Date: 2023/1/3
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.controller;

public class TelemetryKeysConstants {

    public static final String DEVICE_INSTANCE = "DeviceInstance";
    public static final String PRODUCT_ID = "ProductId";
    public static final String PRODUCT_NAME = "ProductName";
    public static final String FIRMWARE_VERSION = "FirmwareVersion";
    public static final String CONNECTED = "Connected";
    public static final String CUSTOM_NAME = "CustomName";

    public static final String CHARGE_REQUEST = "Info/ChargeRequest";
    public static final String FULL_CHARGE_REQUEST = "Info/FullChargeRequest";
    public static final String MAX_CHARGE_CURRENT = "Info/MaxChargeCurrent";
    public static final String MAX_CHARGE_VOLTAGE = "Info/MaxChargeVoltage";
    public static final String MAX_DISCHARGE_CURRENT = "Info/MaxDischargeCurrent";

    public static final String SOC = "Soc";
    public static final String SOH = "Soh";
    public static final String CURRENT = "Current";
    public static final String VOLTAGES = "Voltages";
    public static final String VOLTAGE_0 = "Voltage/0";
    public static final String VOLTAGE_1 = "Voltage/1";
    public static final String VOLTAGE_2 = "Voltage/2";
    public static final String VOLTAGE_3 = "Voltage/3";
    public static final String VOLTAGE_4 = "Voltage/4";
    public static final String VOLTAGE_5 = "Voltage/5";
    public static final String VOLTAGE_6 = "Voltage/6";
    public static final String VOLTAGE_7 = "Voltage/7";
    public static final String VOLTAGE_8 = "Voltage/8";
    public static final String VOLTAGE_9 = "Voltage/9";
    public static final String VOLTAGE_10 = "Voltage/10";
    public static final String VOLTAGE_11 = "Voltage/11";
    public static final String VOLTAGE_12 = "Voltage/12";
    public static final String VOLTAGE_13 = "Voltage/13";
    public static final String VOLTAGE_14 = "Voltage/14";
    public static final String VOLTAGE_15 = "Voltage/15";
    public static final String VOLTAGE_SUM = "VoltageSum";
    public static final String TEMPERATURES = "Temperatures";
    public static final String TEMPERATURE_0 = "Temperature/0";
    public static final String TEMPERATURE_1 = "Temperature/1";
    public static final String TEMPERATURE_2 = "Temperature/2";
    public static final String TEMPERATURE_3 = "Temperature/3";
    public static final String TEMPERATURE_4 = "Temperature/4";
    public static final String TEMPERATURE_5 = "Temperature/5";
    public static final String TEMPERATURE_6 = "Temperature/6";
    public static final String TEMPERATURE_7 = "Temperature/7";
    public static final String TEMPERATURE_8 = "Temperature/8";
    public static final String TEMPERATURE_9 = "Temperature/9";
    public static final String TEMPERATURE_10 = "Temperature/10";
    public static final String TEMPERATURE_11 = "Temperature/11";
    public static final String TEMPERATURE_12 = "Temperature/12";
    public static final String TEMPERATURE_SUM = "TemperatureSum";
    public static final String TEMPERATURES_ENV = "TemperaturesEnv";
    public static final String TEMPERATURES_MOS = "TemperaturesMos";

    public static final String CAPACITY = "Capacity";
    public static final String FULL_CAPACITY = "FullCapacity";
    public static final String INSTALLED_CAPACITY = "InstalledCapacity";

    public static final String MAX_CELL_TEMPERATURE = "System/MaxCellTemperature";
    public static final String MAX_CELL_VOLTAGE = "System/MaxCellVoltage";
    public static final String MAX_TEMPERATURE_CELL_ID = "System/MaxTemperatureCellId";
    public static final String MAX_VOLTAGE_CELL_ID = "System/MaxVoltageCellId";
    public static final String MIN_CELL_TEMPERATURE = "System/MinCellTemperature";
    public static final String MIN_CELL_VOLTAGE = "System/MinCellVoltage";
    public static final String MIN_TEMPERATURE_CELL_ID = "System/MinTemperatureCellId";
    public static final String MIN_VOLTAGE_CELL_ID = "System/MinVoltageCellId";
    public static final String NR_OF_MODULES_BLOCKING_CHARGE = "System/NrOfModulesBlockingCharge";
    public static final String NR_OF_MODULES_BLOCKING_DISCHARGE = "System/NrOfModulesBlockingDischarge";
    public static final String NR_OF_MODULES_OFFLINE = "System/NrOfModulesOffline";
    public static final String NR_OF_MODULES_ONLINE = "System/NrOfModulesOnline";

    public static final String CHARGED_ENERGY = "History/ChargedEnergy";
    public static final String DISCHARGED_ENERGY = "History/DischargedEnergy";

    public static final String CONNECTION = "Mgmt/Connection";

    public static final String PARAMS_ALARM_VOLTAGE_HIGH_CELL_VOLTAGE = "Params/Alarm/Voltage/HighCellVoltage";
    public static final String PARAMS_ALARM_VOLTAGE_LOW_CELL_VOLTAGE = "Params/Alarm/Voltage/LowCellVoltage";
    public static final String PARAMS_ALARM_VOLTAGE_HIGH_PACK_VOLTAGE = "Params/Alarm/Voltage/HighPackVoltage";
    public static final String PARAMS_ALARM_VOLTAGE_LOW_PACK_VOLTAGE = "Params/Alarm/Voltage/LowPackVoltage";
    public static final String PARAMS_ALARM_VOLTAGE_DIFFER_VOLTAGE = "Params/Alarm/Voltage/DifferVoltage";
    public static final String PARAMS_ALARM_CURRENT_HIGH_CHG_CURRENT1 = "Params/Alarm/Current/HighChgCurrent1";
    public static final String PARAMS_ALARM_CURRENT_HIGH_CHG_CURRENT2 = "Params/Alarm/Current/HighChgCurrent2";
    public static final String PARAMS_ALARM_CURRENT_HIGH_DISCH_CURRENT1 = "Params/Alarm/Current/HighDischCurrent1";
    public static final String PARAMS_ALARM_CURRENT_HIGH_DISCH_CURRENT2 = "Params/Alarm/Current/HighDischCurrent2";
    public static final String PARAMS_ALARM_CURRENT_LOW_CHG_CURRENT1 = "Params/Alarm/Current/LowChgCurrent1";
    public static final String PARAMS_ALARM_CURRENT_LOW_CHG_CURRENT2 = "Params/Alarm/Current/LowChgCurrent2";
    public static final String PARAMS_ALARM_CURRENT_LOW_DISCH_CURRENT1 = "Params/Alarm/Current/LowDischCurrent1";
    public static final String PARAMS_ALARM_CURRENT_LOW_DISCH_CURRENT2 = "Params/Alarm/Current/LowDischCurrent2";
    public static final String PARAMS_ALARM_TEMPERATURE_HIGH_CHG_TEMPERATURE = "Params/Alarm/Temperature/HighChgTemperature";
    public static final String PARAMS_ALARM_TEMPERATURE_HIGH_DISCH_TEMPERATURE = "Params/Alarm/Temperature/HighDischTemperature";
    public static final String PARAMS_ALARM_TEMPERATURE_HIGH_ENV_TEMPERATURE = "Params/Alarm/Temperature/HighEnvTemperature";
    public static final String PARAMS_ALARM_TEMPERATURE_HIGH_MOS_TEMPERATURE = "Params/Alarm/Temperature/HighMosTemperature";
    public static final String PARAMS_ALARM_TEMPERATURE_LOW_CHG_TEMPERATURE = "Params/Alarm/Temperature/LowChgTemperature";
    public static final String PARAMS_ALARM_TEMPERATURE_LOW_DISCH_TEMPERATURE = "Params/Alarm/Temperature/LowDischTemperature";
    public static final String PARAMS_ALARM_TEMPERATURE_LOW_ENV_TEMPERATURE = "Params/Alarm/Temperature/LowEnvTemperature";
    public static final String PARAMS_ALARM_TEMPERATURE_LOW_MOS_TEMPERATURE = "Params/Alarm/Temperature/LowMosTemperature";
    public static final String PARAMS_WARNING_VOLTAGE_HIGH_CELL_VOLTAGE = "Params/Warning/Voltage/HighCellVoltage";
    public static final String PARAMS_WARNING_VOLTAGE_LOW_CELL_VOLTAGE = "Params/Warning/Voltage/LowCellVoltage";
    public static final String PARAMS_WARNING_VOLTAGE_HIGH_PACK_VOLTAGE = "Params/Warning/Voltage/HighPackVoltage";
    public static final String PARAMS_WARNING_VOLTAGE_LOW_PACK_VOLTAGE = "Params/Warning/Voltage/LowPackVoltage";
    public static final String PARAMS_WARNING_VOLTAGE_DIFFER_VOLTAGE = "Params/Warning/Voltage/DifferVoltage";
    public static final String PARAMS_WARNING_CURRENT_HIGH_CHG_CURRENT1 = "Params/Warning/Current/HighChgCurrent1";
    public static final String PARAMS_WARNING_CURRENT_HIGH_CHG_CURRENT2 = "Params/Warning/Current/HighChgCurrent2";
    public static final String PARAMS_WARNING_CURRENT_HIGH_DISCH_CURRENT1 = "Params/Warning/Current/HighDischCurrent1";
    public static final String PARAMS_WARNING_CURRENT_HIGH_DISCH_CURRENT2 = "Params/Warning/Current/HighDischCurrent2";
    public static final String PARAMS_WARNING_CURRENT_LOW_CHG_CURRENT1 = "Params/Warning/Current/LowChgCurrent1";
    public static final String PARAMS_WARNING_CURRENT_LOW_CHG_CURRENT2 = "Params/Warning/Current/LowChgCurrent2";
    public static final String PARAMS_WARNING_CURRENT_LOW_DISCH_CURRENT1 = "Params/Warning/Current/LowDischCurrent1";
    public static final String PARAMS_WARNING_CURRENT_LOW_DISCH_CURRENT2 = "Params/Warning/Current/LowDischCurrent2";
    public static final String PARAMS_WARNING_TEMPERATURE_HIGH_CHG_TEMPERATURE = "Params/Warning/Temperature/HighChgTemperature";
    public static final String PARAMS_WARNING_TEMPERATURE_HIGH_DISCH_TEMPERATURE = "Params/Warning/Temperature/HighDischTemperature";
    public static final String PARAMS_WARNING_TEMPERATURE_HIGH_ENV_TEMPERATURE = "Params/Warning/Temperature/HighEnvTemperature";
    public static final String PARAMS_WARNING_TEMPERATURE_HIGH_MOS_TEMPERATURE = "Params/Warning/Temperature/HighMosTemperature";
    public static final String PARAMS_WARNING_TEMPERATURE_LOW_CHG_TEMPERATURE = "Params/Warning/Temperature/LowChgTemperature";
    public static final String PARAMS_WARNING_TEMPERATURE_LOW_DISCH_TEMPERATURE = "Params/Warning/Temperature/LowDischTemperature";
    public static final String PARAMS_WARNING_TEMPERATURE_LOW_ENV_TEMPERATURE = "Params/Warning/Temperature/LowEnvTemperature";
    public static final String PARAMS_WARNING_TEMPERATURE_LOW_MOS_TEMPERATURE = "Params/Warning/Temperature/LowMosTemperature";
    public static final String PARAMS_SOC = "Params/Soc";
    public static final String PARAMS_SLEEP_CELL_VOLTAGE = "Params/Sleep/CellVoltage";
    public static final String PARAMS_BALANCE_CELL_VOLTAGE = "Params/Balance/CellVoltage";
    public static final String PARAMS_BALANCE_DIFFER_VOLTAGE = "Params/Balance/DifferVoltage";
    public static final String PARAMS_CC_CV_CC = "Params/CcCV/Cc";
    public static final String PARAMS_CC_CV_CV = "Params/CcCV/CV";

    public static final String ALARMS_VOLTAGE_HIGH_CELL_VOLTAGE = "Alarms/Voltage/HighCellVoltage";
    public static final String ALARMS_VOLTAGE_LOW_CELL_VOLTAGE = "Alarms/Voltage/LowCellVoltage";
    public static final String ALARMS_VOLTAGE_HIGH_PACK_VOLTAGE = "Alarms/Voltage/HighPackVoltage";
    public static final String ALARMS_VOLTAGE_LOW_PACK_VOLTAGE = "Alarms/Voltage/LowPackVoltage";
    public static final String ALARMS_VOLTAGE_DIFFER_VOLTAGE = "Alarms/Voltage/DifferVoltage";
    public static final String ALARMS_CURRENT_HIGH_CHG_CURRENT1 = "Alarms/Current/HighChgCurrent1";
    public static final String ALARMS_CURRENT_HIGH_CHG_CURRENT2 = "Alarms/Current/HighChgCurrent2";
    public static final String ALARMS_CURRENT_HIGH_DISCH_CURRENT1 = "Alarms/Current/HighDischCurrent1";
    public static final String ALARMS_CURRENT_HIGH_DISCH_CURRENT2 = "Alarms/Current/HighDischCurrent2";
    public static final String ALARMS_CURRENT_LOW_CHG_CURRENT1 = "Alarms/Current/LowChgCurrent1";
    public static final String ALARMS_CURRENT_LOW_CHG_CURRENT2 = "Alarms/Current/LowChgCurrent2";
    public static final String ALARMS_CURRENT_LOW_DISCH_CURRENT1 = "Alarms/Current/LowDischCurrent1";
    public static final String ALARMS_CURRENT_LOW_DISCH_CURRENT2 = "Alarms/Current/LowDischCurrent2";
    public static final String ALARMS_TEMPERATURE_HIGH_CHG_TEMPERATURE = "Alarms/Temperature/HighChgTemperature";
    public static final String ALARMS_TEMPERATURE_HIGH_DISCH_TEMPERATURE = "Alarms/Temperature/HighDischTemperature";
    public static final String ALARMS_TEMPERATURE_HIGH_ENV_TEMPERATURE = "Alarms/Temperature/HighEnvTemperature";
    public static final String ALARMS_TEMPERATURE_HIGH_MOS_TEMPERATURE = "Alarms/Temperature/HighMosTemperature";
    public static final String ALARMS_TEMPERATURE_LOW_CHG_TEMPERATURE = "Alarms/Temperature/LowChgTemperature";
    public static final String ALARMS_TEMPERATURE_LOW_DISCH_TEMPERATURE = "Alarms/Temperature/LowDischTemperature";
    public static final String ALARMS_TEMPERATURE_LOW_ENV_TEMPERATURE = "Alarms/Temperature/LowEnvTemperature";
    public static final String ALARMS_TEMPERATURE_LOW_MOS_TEMPERATURE = "Alarms/Temperature/LowMosTemperature";

    public static final String WARNING_VOLTAGE_HIGH_CELL_VOLTAGE= "Warning/Voltage/HighCellVoltage";
    public static final String WARNING_VOLTAGE_LOW_CELL_VOLTAGE= "Warning/Voltage/LowCellVoltage";
    public static final String WARNING_VOLTAGE_HIGH_PACK_VOLTAGE= "Warning/Voltage/HighPackVoltage";
    public static final String WARNING_VOLTAGE_LOW_PACK_VOLTAGE= "Warning/Voltage/LowPackVoltage";
    public static final String WARNING_VOLTAGE_DIFFER_VOLTAGE= "Warning/Voltage/DifferVoltage";
    public static final String WARNING_CURRENT_HIGH_CHG_CURRENT1= "Warning/Current/HighChgCurrent1";
    public static final String WARNING_CURRENT_HIGH_CHG_CURRENT2= "Warning/Current/HighChgCurrent2";
    public static final String WARNING_CURRENT_HIGH_DISCH_CURRENT1= "Warning/Current/HighDischCurrent1";
    public static final String WARNING_CURRENT_HIGH_DISCH_CURRENT2= "Warning/Current/HighDischCurrent2";
    public static final String WARNING_CURRENT_LOW_CHG_CURRENT1= "Warning/Current/LowChgCurrent1";
    public static final String WARNING_CURRENT_LOW_CHG_CURRENT2= "Warning/Current/LowChgCurrent2";
    public static final String WARNING_CURRENT_LOW_DISCH_CURRENT1= "Warning/Current/LowDischCurrent1";
    public static final String WARNING_CURRENT_LOW_DISCH_CURRENT2= "Warning/Current/LowDischCurrent2";
    public static final String WARNING_TEMPERATURE_HIGH_CHG_TEMPERATURE= "Warning/Temperature/HighChgTemperature";
    public static final String WARNING_TEMPERATURE_HIGH_DISCH_TEMPERATURE= "Warning/Temperature/HighDischTemperature";
    public static final String WARNING_TEMPERATURE_HIGH_ENV_TEMPERATURE= "Warning/Temperature/HighEnvTemperature";
    public static final String WARNING_TEMPERATURE_HIGH_MOS_TEMPERATURE= "Warning/Temperature/HighMosTemperature";
    public static final String WARNING_TEMPERATURE_LOW_CHG_TEMPERATURE= "Warning/Temperature/LowChgTemperature";
    public static final String WARNING_TEMPERATURE_LOW_DISCH_TEMPERATURE= "Warning/Temperature/LowDischTemperature";
    public static final String WARNING_TEMPERATURE_LOW_ENV_TEMPERATURE= "Warning/Temperature/LowEnvTemperature";
    public static final String WARNING_TEMPERATURE_LOW_MOS_TEMPERATURE= "Warning/Temperature/LowMosTemperature";

    public static final String FAULTS_MOS_CHG = "Faults/MosChg";
    public static final String FAULTS_MOS_DISCH = "Faults/MosDisch";
    public static final String FAULTS_HEATING = "Faults/Heating";
    public static final String FAULTS_LIMITER = "Faults/Limiter";
    public static final String FAULTS_SAMPLING = "Faults/Sampling";
    public static final String FAULTS_CELL = "Faults/Cell";
    public static final String FAULTS_NTC = "Faults/Ntc";
    public static final String FAULTS_AFE = "Faults/Afe";

    public static final String STATUS_CHG_LOCK = "Status/ChgLock";
    public static final String STATUS_DISCH_LOCK = "Status/DischLock";
    public static final String STATUS_ANTI_THEFT_LOCK = "Status/AntiTheftLock";
    public static final String STATUS_PRE_CHG_MOS = "Status/PreChgMos";
}
