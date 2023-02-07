/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   BattaryVO.java
 * Author: gongjian   ID:     Version:    Date: 2023/1/3
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.common.data.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Historical data 对应数据节点 System overview
 * @author gongjian
 */
@Data
public class BatteryLiveFeedVO {
    // battery 中的soc指标 Historical data
    private List<List<Object>> bs = new ArrayList<>();
    private List<List<Object>> bv = new ArrayList<>();
    // Solar 光伏消耗
    private List<List<Object>> total_solar_yield = new ArrayList<>();
    // Consumption 电池总耗
    private List<List<Object>> total_consumption = new ArrayList<>();
    private List<List<Object>> total_genset = new ArrayList<>();
    // 电网去电
    private List<List<Object>> grid_history_to = new ArrayList<>();
    // 电网来电
    private List<List<Object>> grid_history_from = new ArrayList<>();
    private List<List<Object>> history_charged_energy = new ArrayList<>();
    private List<List<Object>> history_discharged_energy = new ArrayList<>();

    @JsonProperty(value = "Pdc")
    private Boolean pdc = false;
    @JsonProperty(value = "iOI1")
    private Boolean iOI1 = false;

    /*public static void main(String[] args) {
        BatteryLiveFeedVO batteryLiveFeedVO = new BatteryLiveFeedVO();
        List<Object> list = new ArrayList<>();
        list.add(1672574400000L);
        list.add(56.2);
        list.add(56);
        list.add(57);
        List<Object> list_1 = new ArrayList<>();
        list_1.add(1672574400000L);
        list_1.add(56.2);
        list_1.add(56);
        list_1.add(57);
        List<Object> list1 = new ArrayList<>();
        list1.add(1672574400000L);
        list1.add(0.4733123779296875);
        List<Object> list1_1 = new ArrayList<>();
        list1_1.add(1672574400000L);
        list1_1.add(0.4733123779296875);
        List<List<Object>> list_bs = new ArrayList<>();
        list_bs.add(list);
        list_bs.add(list_1);
        List<List<Object>> list_total_consumption = new ArrayList<>();
        list_total_consumption.add(list1);
        list_total_consumption.add(list1_1);
        batteryLiveFeedVO.setBs(list_bs);
        batteryLiveFeedVO.setTotal_consumption(list_total_consumption);
        batteryLiveFeedVO.setPdc(false);
        batteryLiveFeedVO.setIOI1(false);

        System.out.println(JSONObject.toJSONString(batteryLiveFeedVO));
    }*/
}
