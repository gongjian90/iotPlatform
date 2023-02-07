/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   BatteryVenusVO.java
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

@Data
public class BatteryVenusVO {
    private List<List<Object>> bs = new ArrayList<>();
    private List<List<Object>> ac_loads = new ArrayList<>();
    private List<List<Object>> consumption = new ArrayList<>();
    private List<List<Object>> consumption_input = new ArrayList<>();
    private List<List<Object>> consumption_output = new ArrayList<>();
    private List<List<Object>> solar_yield = new ArrayList<>();
    private List<List<Object>> hub_inverter = new ArrayList<>();
    private List<List<Object>> from_to_grid = new ArrayList<>();
    private List<List<Object>> ac_in = new ArrayList<>();
    private List<List<Object>> ac_out = new ArrayList<>();
    private List<List<Object>> ac_genset = new ArrayList<>();
    private List<List<Object>> total_solar_yield = new ArrayList<>();
    @JsonProperty(value = "Pdc")
    private Boolean pdc = false;
    @JsonProperty(value = "dc")
    private Boolean dc = false;
    @JsonProperty(value = "tsT")
    private Boolean tsT = false;
    @JsonProperty(value = "evp")
    private Boolean evp = false;


    /*public static void main(String[] args) {
        BatteryVenusVO batteryVenusVO = new BatteryVenusVO();
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
        batteryVenusVO.setBs(list_bs);
        batteryVenusVO.setTotal_solar_yield(list_total_consumption);
        batteryVenusVO.setAc_out(list_total_consumption);
        batteryVenusVO.setAc_loads(list_total_consumption);
        System.out.println(JSONObject.toJSONString(batteryVenusVO));
    }*/
}
