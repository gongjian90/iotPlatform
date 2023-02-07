/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   NewTelemetryController.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/17
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.controller;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.http.*;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.telemetry.AttributeData;
import org.thingsboard.server.service.telemetry.TsData;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.*;
import static org.thingsboard.server.controller.TelemetryKeysConstants.*;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_KEYS_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping(TbUrlConstants.NEW_TELEMETRY_URL_PREFIX)
@Slf4j
public class NewTelemetryController extends BaseController {

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("new-telemetry-controller"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @ApiOperation(value = "Get attributes (getAttributes)",
            notes = "Returns all attributes that belong to specified entity. Use optional 'keys' parameter to return specific attributes."
                    + "\n Example of the result: \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + ATTRIBUTE_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/attributes", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributes(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = ATTRIBUTES_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_ATTRIBUTES, entityType, entityIdStr,
                    (result, tenantId, entityId) -> getAttributeValuesCallback(result, user, entityId, null, keysStr));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get time-series data (getTimesSeries)",
            notes = "Returns a range of time-series values for specified entity. " +
                    "Returns not aggregated data by default. " +
                    "Use aggregation function ('agg') and aggregation interval ('interval') to enable aggregation of the results on the database / server side. " +
                    "The aggregation is generally more efficient then fetching all records. \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + TS_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/timeSeries", method = RequestMethod.GET, params = {"keys", "startTs", "endTs"})
    @ResponseBody
    public DeferredResult<ResponseEntity> getTimesSeries(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = TELEMETRY_KEYS_BASE_DESCRIPTION, required = true) @RequestParam(name = "keys") String keys,
            @ApiParam(value = "A long value representing the start timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "startTs") Long startTs,
            @ApiParam(value = "A long value representing the end timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "endTs") Long endTs,
            @ApiParam(value = "A long value representing the aggregation interval range in milliseconds.")
            @RequestParam(name = "interval", defaultValue = "0") Long interval,
            @ApiParam(value = "An integer value that represents a max number of timeseries data points to fetch." +
                    " This parameter is used only in the case if 'agg' parameter is set to 'NONE'.", defaultValue = "100")
            @RequestParam(name = "limit", defaultValue = "100") Integer limit,
            @ApiParam(value = "A string value representing the aggregation function. " +
                    "If the interval is not specified, 'agg' parameter will use 'NONE' value.",
                    allowableValues = "MIN, MAX, AVG, SUM, COUNT, NONE")
            @RequestParam(name = "agg", defaultValue = "NONE") String aggStr,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(name = "orderBy", defaultValue = "DESC") String orderBy,
            @ApiParam(value = STRICT_DATA_TYPES_DESCRIPTION)
            @RequestParam(name = "useStrictDataTypes", required = false, defaultValue = "false") Boolean useStrictDataTypes) throws ThingsboardException {
        try {
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, entityType, entityIdStr,
                    (result, tenantId, entityId) -> {
                        // If interval is 0, convert this to a NONE aggregation, which is probably what the user really wanted
                        Aggregation agg = interval == 0L ? Aggregation.valueOf(Aggregation.NONE.name()) : Aggregation.valueOf(aggStr);
                        List<ReadTsKvQuery> queries = toKeysList(keys).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, agg, orderBy))
                                .collect(Collectors.toList());

                        Futures.addCallback(tsService.findAll(tenantId, entityId, queries), getTsKvListCallback(result, useStrictDataTypes), MoreExecutors.directExecutor());
                    });
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void getAttributeValuesCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user, EntityId entityId, String scope, String keys) {
        List<String> keyList = toKeysList(keys);
        FutureCallback<List<AttributeKvEntry>> callback = getAttributeValuesToResponseCallback(result, user, scope, entityId, keyList);
        if (!StringUtils.isEmpty(scope)) {
            if (keyList != null && !keyList.isEmpty()) {
                Futures.addCallback(attributesService.find(user.getTenantId(), entityId, scope, keyList), callback, MoreExecutors.directExecutor());
            } else {
                Futures.addCallback(attributesService.findAll(user.getTenantId(), entityId, scope), callback, MoreExecutors.directExecutor());
            }
        } else {
            List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
            for (String tmpScope : DataConstants.allScopes()) {
                if (keyList != null && !keyList.isEmpty()) {
                    futures.add(attributesService.find(user.getTenantId(), entityId, tmpScope, keyList));
                } else {
                    futures.add(attributesService.findAll(user.getTenantId(), entityId, tmpScope));
                }
            }
            ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);
            Futures.addCallback(future, callback, MoreExecutors.directExecutor());
        }
    }

    private FutureCallback<List<AttributeKvEntry>> getAttributeValuesToResponseCallback(final DeferredResult<ResponseEntity> response,
                                                                                        final SecurityUser user, final String scope,
                                                                                        final EntityId entityId, final List<String> keyList) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> attributes) {
                List<AttributeData> values = attributes.stream().map(attribute ->
                        new AttributeData(attribute.getLastUpdateTs(), attribute.getKey(), getKvValue(attribute))
                ).collect(Collectors.toList());
                logAttributesRead(user, entityId, scope, keyList, null);
                response.setResult(new ResponseEntity<>(R.success(values), HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                logAttributesRead(user, entityId, scope, keyList, e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private ListenableFuture<List<AttributeKvEntry>> mergeAllAttributesFutures(List<ListenableFuture<List<AttributeKvEntry>>> futures) {
        return Futures.transform(Futures.successfulAsList(futures),
                (Function<? super List<List<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
                    List<AttributeKvEntry> tmp = new ArrayList<>();
                    if (input != null) {
                        input.forEach(tmp::addAll);
                    }
                    return tmp;
                }, executor);
    }

    private FutureCallback<List<TsKvEntry>> getTsKvListCallback(final DeferredResult<ResponseEntity> response, Boolean useStrictDataTypes) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                Map<String, List<TsData>> result = new LinkedHashMap<>();
                for (TsKvEntry entry : data) {
                    Object value = useStrictDataTypes ? getKvValue(entry) : entry.getValueAsString();
                    result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(new TsData(entry.getTs(), value));
                }
                response.setResult(new ResponseEntity<>(R.success(result), HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch historical data", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    @ApiOperation(value = "Get time-series data (getTimeSeriesLiveFeed)",
            notes = "Returns a range of time-series values for specified installations. "
                    + MARKDOWN_CODE_BLOCK_START
                    + TS_LIVE_FEED_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installations/{entityId}/stats/live_feed", method = RequestMethod.GET, params = {"startTs", "endTs"})
    @ResponseBody
    public DeferredResult<ResponseEntity> getTimeSeriesLiveFeed(
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = "A long value representing the start timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "startTs") Long startTs,
            @ApiParam(value = "A long value representing the end timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "endTs") Long endTs,
            @ApiParam(value = "A long value representing the aggregation interval range in milliseconds. Default one hour")
            @RequestParam(name = "interval", defaultValue = "3600000") Long interval) throws ThingsboardException {
        try {
            Integer limit = 3600 * 24;
            String orderBy = "asc";
            Boolean useStrictDataTypes = true;
            String keys = SOC + "," + CHARGED_ENERGY + "," + DISCHARGED_ENERGY;
            // entityId = idSite
            // TODO
            // 1小时时间间隔，
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, "DEVICE", entityIdStr,
                    (result, tenantId, entityId) -> {
                        // If interval is 0, convert this to a NONE aggregation, which is probably what the user really wanted
                        List<ReadTsKvQuery> queries = toKeysList(keys).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, Aggregation.AVG, orderBy))
                                .collect(Collectors.toList());
                        queries.addAll(toKeysList(SOC).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, Aggregation.MIN, orderBy))
                                .collect(Collectors.toList()));
                        queries.addAll(toKeysList(SOC).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, Aggregation.MAX, orderBy))
                                .collect(Collectors.toList()));
                        queries.addAll(toKeysList(SOC).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, endTs - startTs, limit, Aggregation.SUM, orderBy))
                                .collect(Collectors.toList()));
                        queries.addAll(toKeysList(CHARGED_ENERGY + "," + DISCHARGED_ENERGY).stream().map(key -> new BaseReadTsKvQuery(key, endTs-interval, endTs, interval, 1, Aggregation.NONE, "desc"))
                                .collect(Collectors.toList()));
                        Futures.addCallback(tsService.findAll(tenantId, entityId, queries), getLiveFeedTsKvListCallback(result, useStrictDataTypes), MoreExecutors.directExecutor());
                    });
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private FutureCallback<List<TsKvEntry>> getLiveFeedTsKvListCallback(final DeferredResult<ResponseEntity> response, Boolean useStrictDataTypes) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                Map<Long, List<Object>> socResult = new LinkedHashMap<>();
                Map<Long, List<Object>> consumptionResult = new LinkedHashMap<>();
                BatteryLiveFeedVO batteryLiveFeedVO = new BatteryLiveFeedVO();
                BatteryLiveFeedTotalVO batteryLiveFeedTotalVO = new BatteryLiveFeedTotalVO();
                List<List<Object>> socList = new ArrayList<>();
                List<List<Object>> consumptionList = new ArrayList<>();
                List<List<Object>> chargeList = new ArrayList<>();
                List<List<Object>> dischargeList = new ArrayList<>();
                for (TsKvEntry entry : data) {
                    Object value = useStrictDataTypes ? getKvValue(entry) : entry.getValueAsString();
                    ArrayList<Object> objects = new ArrayList<>();
                    objects.add(entry.getTs());
                    objects.add(value);
                    ArrayList<Object> subObjects = new ArrayList<>(); // 目的：readOnly
                    subObjects.add(entry.getTs());
                    subObjects.add(value);
                    Aggregation aggType = entry.getAggregation();
                    switch(entry.getKey()) {
                        case SOC:
                            switch (aggType) {
                                case AVG:
                                case MIN:
                                case MAX:
                                    if (socResult.containsKey(entry.getTs())) {
                                        List<Object> objectList = socResult.get(entry.getTs());
                                        objectList.add(value);
                                    } else {
                                        socResult.put(entry.getTs(), objects);
                                    }
                                    break;
                                case SUM:
                                    batteryLiveFeedTotalVO.setBs(new BigDecimal(String.valueOf(value)));
                                    break;
                            }
                            break;
                        case CHARGED_ENERGY:
                            if (null == aggType) {
                                batteryLiveFeedTotalVO.setHistory_charged_energy(new BigDecimal(String.valueOf(value)));
                                break;
                            }
                            switch (aggType) {
                                case AVG:
                                case MIN:
                                case MAX:
                                    if (consumptionResult.containsKey(entry.getTs())) {
                                        List<Object> objectList = consumptionResult.get(entry.getTs());
                                        objectList.set(1, new BigDecimal(String.valueOf(objectList.get(1))).add(new BigDecimal(String.valueOf(value))));
                                    } else {
                                        consumptionResult.put(entry.getTs(), objects);
                                    }
                                    chargeList.add(subObjects);
                                    break;
                                case NONE:
                                    batteryLiveFeedTotalVO.setHistory_charged_energy(new BigDecimal(String.valueOf(value)));
                                    break;
                            }
                            break;
                        case DISCHARGED_ENERGY:
                            if (null == aggType) {
                                batteryLiveFeedTotalVO.setHistory_discharged_energy(new BigDecimal(String.valueOf(value)));
                                break;
                            }
                            switch (aggType) {
                                case AVG:
                                case MIN:
                                case MAX:
                                    if (consumptionResult.containsKey(entry.getTs())) {
                                        List<Object> objectList = consumptionResult.get(entry.getTs());
                                        objectList.set(1, new BigDecimal(String.valueOf(objectList.get(1))).add(new BigDecimal(String.valueOf(value))));
                                    } else {
                                        consumptionResult.put(entry.getTs(), objects);
                                    }
                                    dischargeList.add(subObjects);
                                    break;
                                case NONE:
                                    batteryLiveFeedTotalVO.setHistory_discharged_energy(new BigDecimal(String.valueOf(value)));
                                    break;
                            }
                            break;
                    }
                }
                mapToList(socResult, socList);
                mapToList(consumptionResult, consumptionList);
                batteryLiveFeedVO.setBs(socList);
                batteryLiveFeedVO.setTotal_consumption(consumptionList);
                batteryLiveFeedVO.setHistory_charged_energy(chargeList);
                batteryLiveFeedVO.setHistory_discharged_energy(dischargeList);
                batteryLiveFeedTotalVO.setTotal_consumption(batteryLiveFeedTotalVO.getHistory_charged_energy().add(batteryLiveFeedTotalVO.getHistory_discharged_energy()));
                response.setResult(new ResponseEntity<>(Rts.success(batteryLiveFeedVO, batteryLiveFeedTotalVO), HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch historical data", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    @ApiOperation(value = "Get time-series data (getTimeSeriesVenus)",
            notes = "Returns a range of time-series values for specified installations. "
                    + MARKDOWN_CODE_BLOCK_START
                    + TS_LIVE_FEED_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installations/{entityId}/stats/venus", method = RequestMethod.GET, params = {"startTs", "endTs"})
    @ResponseBody
    public DeferredResult<ResponseEntity> getTimeSeriesVenus(
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = "A long value representing the start timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "startTs") Long startTs,
            @ApiParam(value = "A long value representing the end timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "endTs") Long endTs,
            @ApiParam(value = "A long value representing the aggregation interval range in milliseconds. Default one hour")
            @RequestParam(name = "interval", defaultValue = "900000") Long interval) throws ThingsboardException {
        try {
            Integer limit = 3600 * 24;
            String orderBy = "asc";
            Boolean useStrictDataTypes = true;
            String keys = SOC;
            // entityId = idSite
            // TODO
            // 15分钟时间间隔
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, "DEVICE", entityIdStr,
                    (result, tenantId, entityId) -> {
                        // If interval is 0, convert this to a NONE aggregation, which is probably what the user really wanted
                        List<ReadTsKvQuery> queries = toKeysList(keys).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, Aggregation.AVG, orderBy))
                                .collect(Collectors.toList());
                        queries.addAll(toKeysList(keys).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, endTs - startTs, limit, Aggregation.SUM, orderBy))
                                .collect(Collectors.toList()));
                        Futures.addCallback(tsService.findAll(tenantId, entityId, queries), getVenusTsKvListCallback(result, useStrictDataTypes), MoreExecutors.directExecutor());
                    });
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private FutureCallback<List<TsKvEntry>> getVenusTsKvListCallback(final DeferredResult<ResponseEntity> response, Boolean useStrictDataTypes) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                Map<Long, List<Object>> socResult = new LinkedHashMap<>();
                BatteryVenusVO batteryVenusVO = new BatteryVenusVO();
                BatteryVenusTotalVO batteryVenusTotalVO = new BatteryVenusTotalVO();
                List<List<Object>> socList = new ArrayList<>();
                for (TsKvEntry entry : data) {
                    Object value = useStrictDataTypes ? getKvValue(entry) : entry.getValueAsString();
                    ArrayList<Object> objects = new ArrayList<>();
                    objects.add(entry.getTs());
                    objects.add(value);
                    Aggregation aggType = entry.getAggregation();
                    switch(entry.getKey()) {
                        case SOC:
                            switch (aggType) {
                                case AVG:
                                case MIN:
                                case MAX:
                                    if (socResult.containsKey(entry.getTs())) {
                                        List<Object> objectList = socResult.get(entry.getTs());
                                        objectList.add(value);
                                    } else {
                                        socResult.put(entry.getTs(), objects);
                                    }
                                    break;
                                case SUM:
                                    batteryVenusTotalVO.setBs(new BigDecimal(String.valueOf(value)));
                                    break;
                            }
                            break;
                    }
                }
                for (Long key : socResult.keySet()) {
                    socList.add(socResult.get(key));
                }
                batteryVenusVO.setBs(socList);
                response.setResult(new ResponseEntity<>(Rts.success(batteryVenusVO, batteryVenusTotalVO), HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch historical data", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    @ApiOperation(value = "Get latest time-series value (getLatestTimeseries)",
            notes = "Returns all time-series that belong to specified entity. Use optional 'keys' parameter to return specific time-series." +
                    " The result is a JSON object. The format of the values depends on the 'useStrictDataTypes' parameter." +
                    " By default, all time-series values are converted to strings: \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + LATEST_TS_NON_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n However, it is possible to request the values without conversion ('useStrictDataTypes'=true): \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + LATEST_TS_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installations/{entityId}/stats/latest", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getLatestTimeSeries(
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        try {
            String entityType = "DEVICE";
            Boolean useStrictDataTypes = true;
            String keysStr = SOC + "," + TEMPERATURES + "," + VOLTAGES + "," + CURRENT;
            SecurityUser user = getCurrentUser();
            // entityId = idSite
            // TODO
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, entityType, entityIdStr,
                    (result, tenantId, entityId) -> getLatestTimeseriesValuesCallback(result, user, entityId, keysStr, useStrictDataTypes));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void getLatestTimeseriesValuesCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user,
                                                   EntityId entityId, String keys, Boolean useStrictDataTypes) {
        ListenableFuture<List<TsKvEntry>> future;
        if (StringUtils.isEmpty(keys)) {
            future = tsService.findAllLatest(user.getTenantId(), entityId);
        } else {
            future = tsService.findLatest(user.getTenantId(), entityId, toKeysList(keys));
        }
        Futures.addCallback(future, getLatestTsKvListCallback(result, useStrictDataTypes,
                deviceService.findDeviceById(user.getTenantId(), new DeviceId(entityId.getId()))), MoreExecutors.directExecutor());
    }

    private FutureCallback<List<TsKvEntry>> getLatestTsKvListCallback(final DeferredResult<ResponseEntity> response, Boolean useStrictDataTypes, Device device) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                BatteryLatestVO batteryLatestVO = new BatteryLatestVO();
                batteryLatestVO.setName(device.getName());
                batteryLatestVO.setId(device.getId().toString());
                for (TsKvEntry entry : data) {
                    Object value = useStrictDataTypes ? getKvValue(entry) : entry.getValueAsString();
                    switch(entry.getKey()) {
                        case SOC:
                            batteryLatestVO.setSoc(new BigDecimal(String.valueOf(value)));
                            break;
                        case CURRENT:
                            batteryLatestVO.setCurrent(new BigDecimal(String.valueOf(value)));
                            break;
                        case VOLTAGES:
                            batteryLatestVO.setVoltage(StringUtils.listStrAndAdd(String.valueOf(value))); // 返回16串电压之和
                            break;
                        case TEMPERATURES:
                            batteryLatestVO.setTemperature(StringUtils.listStrAndAdd(String.valueOf(value))); // 返回10串温度之和
                            break;
                    }
                }
                //功率 = 电压 x 电流
                batteryLatestVO.setPower(batteryLatestVO.getCurrent().multiply(batteryLatestVO.getVoltage()));
                response.setResult(new ResponseEntity<>(R.success(batteryLatestVO), HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch historical data", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }



    @ApiOperation(value = "Get time-series data (getTimeSeriesGraph)",
            notes = "Returns a range of time-series values for specified installations. "
                    + MARKDOWN_CODE_BLOCK_START
                    + TS_LIVE_FEED_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installations/{entityId}/widgets/Graph", method = RequestMethod.GET, params = {"startTs", "endTs"})
    @ResponseBody
    public DeferredResult<ResponseEntity> getTimeSeriesGraph(
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = "A long value representing the start timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "startTs") Long startTs,
            @ApiParam(value = "A long value representing the end timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "endTs") Long endTs,
            @ApiParam(value = "A long value representing the aggregation interval range in milliseconds. Default four and a half of minutes")
            @RequestParam(name = "interval", defaultValue = "270000") Long interval) throws ThingsboardException {
        try {
            Integer limit = 3600 * 24;
            String orderBy = "asc";
            Boolean useStrictDataTypes = true;
            String keys = SOC + "," + CURRENT + "," + VOLTAGE_SUM + "," + TEMPERATURE_SUM + "," +
                    VOLTAGE_0 + "," +
                    VOLTAGE_1 + "," +
                    TEMPERATURE_0 + "," +
                    TEMPERATURE_1 + "," +
                    MAX_CELL_VOLTAGE + "," +
                    MIN_CELL_VOLTAGE + "," +
                    MAX_CHARGE_VOLTAGE + "," +
                    MAX_CHARGE_CURRENT + "," +
                    MAX_DISCHARGE_CURRENT;
            SecurityUser currentUser = getCurrentUser();
            TenantId currentUserTenantId = currentUser.getTenantId();
            // 4.5分钟时间间隔，
            return accessValidator.validateEntityAndCallback(currentUser, Operation.READ_TELEMETRY, "DEVICE", entityIdStr,
                    (result, tenantId, entityId) -> {
                        // If interval is 0, convert this to a NONE aggregation, which is probably what the user really wanted
                        List<ReadTsKvQuery> queries = toKeysList(keys).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, Aggregation.AVG, orderBy))
                                .collect(Collectors.toList());
                        queries.addAll(toKeysList(keys).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, Aggregation.MIN, orderBy))
                                .collect(Collectors.toList()));
                        queries.addAll(toKeysList(keys).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, Aggregation.MAX, orderBy))
                                .collect(Collectors.toList()));
                        Futures.addCallback(tsService.findAll(tenantId, entityId, queries), getGraphTsKvListCallback(result, useStrictDataTypes,
                                deviceService.findDeviceById(currentUserTenantId, new DeviceId(entityId.getId()))), MoreExecutors.directExecutor());
                    });
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    private FutureCallback<List<TsKvEntry>> getGraphTsKvListCallback(final DeferredResult<ResponseEntity> response, Boolean useStrictDataTypes, Device device) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                Map<Long, List<Object>> socResult = new LinkedHashMap<>();
                Map<Long, List<Object>> currentResult = new LinkedHashMap<>();
                Map<Long, List<Object>> mincvResult = new LinkedHashMap<>();
                Map<Long, List<Object>> maxcvResult = new LinkedHashMap<>();
                Map<Long, List<Object>> cvlResult = new LinkedHashMap<>();
                Map<Long, List<Object>> cclResult = new LinkedHashMap<>();
                Map<Long, List<Object>> dclResult = new LinkedHashMap<>();
                Map<Long, List<Object>> voltageResult = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage0Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage1Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage2Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage3Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage4Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage5Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage6Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage7Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage8Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage9Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage10Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage11Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage12Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage13Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage14Result = new LinkedHashMap<>();
                Map<Long, List<Object>> voltage15Result = new LinkedHashMap<>();
                Map<Long, List<Object>> tempResult = new LinkedHashMap<>();
                Map<Long, List<Object>> temp0Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp1Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp2Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp3Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp4Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp5Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp6Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp7Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp8Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp9Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp10Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp11Result = new LinkedHashMap<>();
                Map<Long, List<Object>> temp12Result = new LinkedHashMap<>();
                BatteryGraphVO batteryGraphVO = new BatteryGraphVO();
                List<List<Object>> socList = new ArrayList<>();
                List<List<Object>> currentList = new ArrayList<>();
                List<List<Object>> mincvList = new ArrayList<>();
                List<List<Object>> maxcvList = new ArrayList<>();
                List<List<Object>> cvlList = new ArrayList<>();
                List<List<Object>> cclList = new ArrayList<>();
                List<List<Object>> dclList = new ArrayList<>();
                List<List<Object>> voltageList = new ArrayList<>();
                List<List<Object>> voltage0List = new ArrayList<>();
                List<List<Object>> voltage1List = new ArrayList<>();
                List<List<Object>> voltage2List = new ArrayList<>();
                List<List<Object>> voltage3List = new ArrayList<>();
                List<List<Object>> voltage4List = new ArrayList<>();
                List<List<Object>> voltage5List = new ArrayList<>();
                List<List<Object>> voltage6List = new ArrayList<>();
                List<List<Object>> voltage7List = new ArrayList<>();
                List<List<Object>> voltage8List = new ArrayList<>();
                List<List<Object>> voltage9List = new ArrayList<>();
                List<List<Object>> voltage10List = new ArrayList<>();
                List<List<Object>> voltage11List = new ArrayList<>();
                List<List<Object>> voltage12List = new ArrayList<>();
                List<List<Object>> voltage13List = new ArrayList<>();
                List<List<Object>> voltage14List = new ArrayList<>();
                List<List<Object>> voltage15List = new ArrayList<>();
                List<List<Object>> tempList = new ArrayList<>();
                List<List<Object>> temp0List = new ArrayList<>();
                List<List<Object>> temp1List = new ArrayList<>();
                List<List<Object>> temp2List = new ArrayList<>();
                List<List<Object>> temp3List = new ArrayList<>();
                List<List<Object>> temp4List = new ArrayList<>();
                List<List<Object>> temp5List = new ArrayList<>();
                List<List<Object>> temp6List = new ArrayList<>();
                List<List<Object>> temp7List = new ArrayList<>();
                List<List<Object>> temp8List = new ArrayList<>();
                List<List<Object>> temp9List = new ArrayList<>();
                List<List<Object>> temp10List = new ArrayList<>();
                List<List<Object>> temp11List = new ArrayList<>();
                List<List<Object>> temp12List = new ArrayList<>();
                List<String> brackets = StringUtils.matchStringInSquareBrackets(device.getName());
                for (TsKvEntry entry : data) {
                    Object value = useStrictDataTypes ? getKvValue(entry) : entry.getValueAsString();
                    ArrayList<Object> objects = new ArrayList<>();
                    objects.add(entry.getTs() / 1000);
                    objects.add(value);
                    switch(entry.getKey()) {
                        case SOC:
                            if (socResult.containsKey(entry.getTs())) {
                                List<Object> objectList = socResult.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                socResult.put(entry.getTs(), objects);
                            }
                            break;
                        case CURRENT:
                            if (currentResult.containsKey(entry.getTs())) {
                                List<Object> objectList = currentResult.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                currentResult.put(entry.getTs(), objects);
                            }
                            break;
                        case MAX_CELL_VOLTAGE:
                            if (maxcvResult.containsKey(entry.getTs())) {
                                List<Object> objectList = maxcvResult.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                maxcvResult.put(entry.getTs(), objects);
                            }
                            break;
                        case MIN_CELL_VOLTAGE:
                            if (mincvResult.containsKey(entry.getTs())) {
                                List<Object> objectList = mincvResult.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                mincvResult.put(entry.getTs(), objects);
                            }
                            break;
                        case MAX_CHARGE_CURRENT:
                            if (cclResult.containsKey(entry.getTs())) {
                                List<Object> objectList = cclResult.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                cclResult.put(entry.getTs(), objects);
                            }
                            break;
                        case MAX_CHARGE_VOLTAGE:
                            if (cvlResult.containsKey(entry.getTs())) {
                                List<Object> objectList = cvlResult.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                cvlResult.put(entry.getTs(), objects);
                            }
                            break;
                        case MAX_DISCHARGE_CURRENT:
                            if (dclResult.containsKey(entry.getTs())) {
                                List<Object> objectList = dclResult.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                dclResult.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_SUM:
                            if (voltageResult.containsKey(entry.getTs())) {
                                List<Object> objectList = voltageResult.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltageResult.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_0:
                            if (voltage0Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage0Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage0Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_1:
                            if (voltage1Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage1Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage1Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_2:
                            if (voltage2Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage2Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage2Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_3:
                            if (voltage3Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage3Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage3Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_4:
                            if (voltage4Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage4Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage4Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_5:
                            if (voltage5Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage5Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage5Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_6:
                            if (voltage6Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage6Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage6Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_7:
                            if (voltage7Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage7Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage7Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_8:
                            if (voltage8Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage8Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage8Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_9:
                            if (voltage9Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage9Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage9Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_10:
                            if (voltage10Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage10Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage10Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_11:
                            if (voltage11Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage11Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage11Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_12:
                            if (voltage12Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage12Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage12Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_13:
                            if (voltage13Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage13Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage13Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_14:
                            if (voltage14Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage14Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage14Result.put(entry.getTs(), objects);
                            }
                            break;
                        case VOLTAGE_15:
                            if (voltage15Result.containsKey(entry.getTs())) {
                                List<Object> objectList = voltage15Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                voltage15Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_SUM:
                            if (tempResult.containsKey(entry.getTs())) {
                                List<Object> objectList = tempResult.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                tempResult.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_0:
                            if (temp0Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp0Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp0Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_1:
                            if (temp1Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp1Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp1Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_2:
                            if (temp2Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp2Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp2Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_3:
                            if (temp3Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp3Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp3Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_4:
                            if (temp4Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp4Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp4Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_5:
                            if (temp5Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp5Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp5Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_6:
                            if (temp6Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp6Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp6Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_7:
                            if (temp7Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp7Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp7Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_8:
                            if (temp8Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp8Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp8Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_9:
                            if (temp9Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp9Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp9Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_10:
                            if (temp10Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp10Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp10Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_11:
                            if (temp11Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp11Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp11Result.put(entry.getTs(), objects);
                            }
                            break;
                        case TEMPERATURE_12:
                            if (temp12Result.containsKey(entry.getTs())) {
                                List<Object> objectList = temp12Result.get(entry.getTs());
                                objectList.add(value);
                            } else {
                                temp12Result.put(entry.getTs(), objects);
                            }
                            break;
                    }
                }
                mapToList(socResult, socList);
                mapToList(currentResult, currentList);
                mapToList(maxcvResult, maxcvList);
                mapToList(mincvResult, mincvList);
                mapToList(cclResult, cclList);
                mapToList(cvlResult, cvlList);
                mapToList(dclResult, dclList);
                mapToList(voltageResult, voltageList);
                mapToList(voltage0Result, voltage0List);
                mapToList(voltage1Result, voltage1List);
                mapToList(voltage2Result, voltage2List);
                mapToList(voltage3Result, voltage3List);
                mapToList(voltage4Result, voltage4List);
                mapToList(voltage5Result, voltage5List);
                mapToList(voltage6Result, voltage6List);
                mapToList(voltage7Result, voltage7List);
                mapToList(voltage8Result, voltage8List);
                mapToList(voltage9Result, voltage9List);
                mapToList(voltage10Result, voltage10List);
                mapToList(voltage11Result, voltage11List);
                mapToList(voltage12Result, voltage12List);
                mapToList(voltage13Result, voltage13List);
                mapToList(voltage14Result, voltage14List);
                mapToList(voltage15Result, voltage15List);
                mapToList(tempResult, tempList);
                mapToList(temp0Result, temp0List);
                mapToList(temp1Result, temp1List);
                mapToList(temp2Result, temp2List);
                mapToList(temp3Result, temp3List);
                mapToList(temp4Result, temp4List);
                mapToList(temp5Result, temp5List);
                mapToList(temp6Result, temp6List);
                mapToList(temp7Result, temp7List);
                mapToList(temp8Result, temp8List);
                mapToList(temp9Result, temp9List);
                mapToList(temp10Result, temp10List);
                mapToList(temp11Result, temp11List);
                mapToList(temp12Result, temp12List);
                Map<String, List<List<Object>>> dataMap = new LinkedHashMap<>();
                dataMap.put(MetaEnum.STATE_OF_CHARGE.getIdDataAttribute(), socList);
                dataMap.put(MetaEnum.CURRENT.getIdDataAttribute(), currentList);
                dataMap.put(MetaEnum.MAXIMUM_CELL_VOLTAGE.getIdDataAttribute(), maxcvList);
                dataMap.put(MetaEnum.MINIMUM_CELL_VOLTAGE.getIdDataAttribute(), mincvList);
                dataMap.put(MetaEnum.CHARGE_VOLTAGE_LIMIT.getIdDataAttribute(), cvlList);
                dataMap.put(MetaEnum.CHARGE_CURRENT_LIMIT.getIdDataAttribute(), cclList);
                dataMap.put(MetaEnum.DISCHARGE_CURRENT_LIMIT.getIdDataAttribute(), dclList);
                dataMap.put(MetaEnum.VOLTAGE.getIdDataAttribute(), voltageList);
//                dataMap.put(MetaEnum.VOLTAGE_0.getIdDataAttribute(), voltage0List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_2.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
//                dataMap.put(MetaEnum.VOLTAGE_1.getIdDataAttribute(), voltage1List);
                dataMap.put(MetaEnum.BATTERY_TEMPERATURE.getIdDataAttribute(), tempList);
                batteryGraphVO.setData(dataMap);
                batteryGraphVO.setMeta(createGraphMetaMap());
                batteryGraphVO.setInstance(Integer.valueOf(brackets.get(0)));
                response.setResult(new ResponseEntity<>(R.success(batteryGraphVO), HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch historical data", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private void mapToList(Map<Long, List<Object>> map, List<List<Object>> list) {
        for (Long key : map.keySet()) {
            list.add(map.get(key));
        }
    }

    private Map<String, MetaVO> createGraphMetaMap() {
        Map<String, MetaVO> metaVOMap = new LinkedHashMap<>();
        metaVOMap.put(MetaEnum.STATE_OF_CHARGE.getIdDataAttribute(), new MetaVO(MetaEnum.STATE_OF_CHARGE.getCode(),
                MetaEnum.STATE_OF_CHARGE.getDescription(), MetaEnum.STATE_OF_CHARGE.getFormatValueOnly(),
                MetaEnum.STATE_OF_CHARGE.getFormatWithUnit(), MetaEnum.STATE_OF_CHARGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.CURRENT.getIdDataAttribute(), new MetaVO(MetaEnum.CURRENT.getCode(),
                        MetaEnum.CURRENT.getDescription(), MetaEnum.CURRENT.getFormatValueOnly(),
                        MetaEnum.CURRENT.getFormatWithUnit(), MetaEnum.CURRENT.getAxisGroup()));
        metaVOMap.put(MetaEnum.VOLTAGE.getIdDataAttribute(), new MetaVO(MetaEnum.VOLTAGE.getCode(),
                MetaEnum.VOLTAGE.getDescription(), MetaEnum.VOLTAGE.getFormatValueOnly(),
                MetaEnum.VOLTAGE.getFormatWithUnit(), MetaEnum.VOLTAGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.BATTERY_TEMPERATURE.getIdDataAttribute(), new MetaVO(MetaEnum.BATTERY_TEMPERATURE.getCode(),
                MetaEnum.BATTERY_TEMPERATURE.getDescription(), MetaEnum.BATTERY_TEMPERATURE.getFormatValueOnly(),
                MetaEnum.BATTERY_TEMPERATURE.getFormatWithUnit(), MetaEnum.BATTERY_TEMPERATURE.getAxisGroup()));
        metaVOMap.put(MetaEnum.MAXIMUM_CELL_VOLTAGE.getIdDataAttribute(), new MetaVO(MetaEnum.MAXIMUM_CELL_VOLTAGE.getCode(),
                MetaEnum.MAXIMUM_CELL_VOLTAGE.getDescription(), MetaEnum.MAXIMUM_CELL_VOLTAGE.getFormatValueOnly(),
                MetaEnum.MAXIMUM_CELL_VOLTAGE.getFormatWithUnit(), MetaEnum.MAXIMUM_CELL_VOLTAGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.MINIMUM_CELL_VOLTAGE.getIdDataAttribute(), new MetaVO(MetaEnum.MINIMUM_CELL_VOLTAGE.getCode(),
                MetaEnum.MINIMUM_CELL_VOLTAGE.getDescription(), MetaEnum.MINIMUM_CELL_VOLTAGE.getFormatValueOnly(),
                MetaEnum.MINIMUM_CELL_VOLTAGE.getFormatWithUnit(), MetaEnum.MINIMUM_CELL_VOLTAGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.CHARGE_CURRENT_LIMIT.getIdDataAttribute(), new MetaVO(MetaEnum.CHARGE_CURRENT_LIMIT.getCode(),
                MetaEnum.CHARGE_CURRENT_LIMIT.getDescription(), MetaEnum.CHARGE_CURRENT_LIMIT.getFormatValueOnly(),
                MetaEnum.CHARGE_CURRENT_LIMIT.getFormatWithUnit(), MetaEnum.CHARGE_CURRENT_LIMIT.getAxisGroup()));
        metaVOMap.put(MetaEnum.CHARGE_VOLTAGE_LIMIT.getIdDataAttribute(), new MetaVO(MetaEnum.CHARGE_VOLTAGE_LIMIT.getCode(),
                MetaEnum.CHARGE_VOLTAGE_LIMIT.getDescription(), MetaEnum.CHARGE_VOLTAGE_LIMIT.getFormatValueOnly(),
                MetaEnum.CHARGE_VOLTAGE_LIMIT.getFormatWithUnit(), MetaEnum.CHARGE_VOLTAGE_LIMIT.getAxisGroup()));
        metaVOMap.put(MetaEnum.DISCHARGE_CURRENT_LIMIT.getIdDataAttribute(), new MetaVO(MetaEnum.DISCHARGE_CURRENT_LIMIT.getCode(),
                MetaEnum.DISCHARGE_CURRENT_LIMIT.getDescription(), MetaEnum.DISCHARGE_CURRENT_LIMIT.getFormatValueOnly(),
                MetaEnum.DISCHARGE_CURRENT_LIMIT.getFormatWithUnit(), MetaEnum.DISCHARGE_CURRENT_LIMIT.getAxisGroup()));
        return metaVOMap;
    }

    @ApiOperation(value = "Get latest time-series value (getBatterySummary)",
            notes = "Returns all time-series that belong to specified entity. Use optional 'keys' parameter to return specific time-series." +
                    " The result is a JSON object. The format of the values depends on the 'useStrictDataTypes' parameter." +
                    " By default, all time-series values are converted to strings: \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + LATEST_TS_NON_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n However, it is possible to request the values without conversion ('useStrictDataTypes'=true): \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + LATEST_TS_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installations/{entityId}/widgets/BatterySummary", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getBatterySummary(
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        try {
            String entityType = "DEVICE";

            Boolean useStrictDataTypes = true;
            String keysStr = SOC + "," +    // 51
                    CURRENT + "," +         // 47
                    VOLTAGE_SUM + "," +     // 49
                    TEMPERATURE_SUM + "," + // 115
                    MAX_CELL_VOLTAGE + "," +    // 174
                    MIN_CELL_VOLTAGE + "," +    // 173
                    CHARGED_ENERGY + "," +      // 50A
                    DISCHARGED_ENERGY;          // 50B
            SecurityUser user = getCurrentUser();
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, entityType, entityIdStr,
                    (result, tenantId, entityId) -> getBatterySummaryCallback(result, user, entityId, keysStr, useStrictDataTypes));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void getBatterySummaryCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user,
                                                   EntityId entityId, String keys, Boolean useStrictDataTypes) {
        ListenableFuture<List<TsKvEntry>> future;
        if (StringUtils.isEmpty(keys)) {
            future = tsService.findAllLatest(user.getTenantId(), entityId);
        } else {
            future = tsService.findLatest(user.getTenantId(), entityId, toKeysList(keys));
        }
        Futures.addCallback(future, getLatestTsKvBatterySummaryCallback(result, useStrictDataTypes,
                deviceService.findDeviceById(user.getTenantId(), new DeviceId(entityId.getId()))), MoreExecutors.directExecutor());
    }

    private FutureCallback<List<TsKvEntry>> getLatestTsKvBatterySummaryCallback(final DeferredResult<ResponseEntity> response, Boolean useStrictDataTypes, Device device) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                List<Integer> attributeOrder = Arrays.asList(119, 120, 121, 122, 123, 124, 125, 126, 155, 156, 158, 47, 49, 50, 51, 52, 48, 115, 116, 117, 54, 182, 173, 174);
                BatterySummaryVO batterySummaryVO = new BatterySummaryVO();
                Map<String, Object> dataMap = new HashMap<>();
                List<String> brackets = StringUtils.matchStringInSquareBrackets(device.getName());
                String secondsAgo = "";
                for (TsKvEntry entry : data) {
                    DataAttributeVO dataAttributeVO = new DataAttributeVO();
                    Object value = useStrictDataTypes ? getKvValue(entry) : entry.getValueAsString();
                    String valueStr = "";
                    String newValueStr = "";
                    String valueStrUnit = "";
                    String newValueStrUnit = "";
                    secondsAgo = String.valueOf((System.currentTimeMillis() - entry.getTs()) / 1000);
                    switch(entry.getKey()) {
                        case SOC:
                            valueStr = formatValue(String.valueOf(value), AttributeEnum.STATE_OF_CHARGE.getFormatValueOnly());
                            valueStrUnit = formatValue(String.valueOf(value), AttributeEnum.STATE_OF_CHARGE.getFormatWithUnit());
                            dataAttributeVO.setCode(AttributeEnum.STATE_OF_CHARGE.getCode());
                            dataAttributeVO.setIdDataAttribute(AttributeEnum.STATE_OF_CHARGE.getIdDataAttribute());
                            dataAttributeVO.setSecondsAgo(secondsAgo); // 未知逻辑
                            dataAttributeVO.setSecondsToNextLog("60"); // 未知逻辑
                            dataAttributeVO.setValue(valueStr);
                            dataAttributeVO.setValueFloat(valueStr);
                            dataAttributeVO.setDataType(AttributeEnum.STATE_OF_CHARGE.getDataType()); // 根据soc所知
                            dataAttributeVO.setDbusServiceType(AttributeEnum.STATE_OF_CHARGE.getDbusServiceType());
                            dataAttributeVO.setDbusPath(AttributeEnum.STATE_OF_CHARGE.getDbusPath()); // 未知逻辑
                            dataAttributeVO.setInstance(Integer.valueOf(brackets.get(0))); // 未知逻辑
                            dataAttributeVO.setValueString(null); // 未知逻辑
                            dataAttributeVO.setValueEnum(null); // 未知逻辑
                            dataAttributeVO.setNameEnum(null); // 未知逻辑
                            dataAttributeVO.setIsValid("1"); // 未知逻辑
                            dataAttributeVO.setIsKeyIdDataAttribute("1"); // 未知逻辑
                            dataAttributeVO.setFormatValueOnly(AttributeEnum.STATE_OF_CHARGE.getFormatValueOnly());
                            dataAttributeVO.setFormatWithUnit(AttributeEnum.STATE_OF_CHARGE.getFormatWithUnit());
                            dataAttributeVO.setDataAttributeName(AttributeEnum.STATE_OF_CHARGE.getDataAttributeName());
                            dataAttributeVO.setRawValue(valueStr);
                            dataAttributeVO.setFormattedValue(valueStrUnit);
                            dataAttributeVO.setValueFormattedValueOnly(valueStr);
                            dataAttributeVO.setValueFormattedWithUnit(valueStrUnit);
                            dataAttributeVO.setHasOldData(false); // 未知逻辑
                            dataMap.put(AttributeEnum.STATE_OF_CHARGE.getIdDataAttribute(), dataAttributeVO);
                            break;
                        case CURRENT:
                            valueStr = formatValue(String.valueOf(value), AttributeEnum.CURRENT.getFormatValueOnly());
                            valueStrUnit = formatValue(String.valueOf(value), AttributeEnum.CURRENT.getFormatWithUnit());
                            dataAttributeVO.setCode(AttributeEnum.CURRENT.getCode());
                            dataAttributeVO.setIdDataAttribute(AttributeEnum.CURRENT.getIdDataAttribute());
                            dataAttributeVO.setSecondsAgo(secondsAgo); // 未知逻辑
                            dataAttributeVO.setSecondsToNextLog("60"); // 未知逻辑
                            dataAttributeVO.setValue(valueStr);
                            dataAttributeVO.setValueFloat(valueStr); // 根据soc所知
                            dataAttributeVO.setDataType(AttributeEnum.CURRENT.getDataType()); // 根据soc所知
                            dataAttributeVO.setDbusServiceType(AttributeEnum.CURRENT.getDbusServiceType());
                            dataAttributeVO.setDbusPath(AttributeEnum.CURRENT.getDbusPath()); // 未知逻辑
                            dataAttributeVO.setInstance(Integer.valueOf(brackets.get(0))); // 未知逻辑
                            dataAttributeVO.setValueString(null); // 未知逻辑
                            dataAttributeVO.setValueEnum(null); // 未知逻辑
                            dataAttributeVO.setNameEnum(null); // 未知逻辑
                            dataAttributeVO.setIsValid("1"); // 未知逻辑
                            dataAttributeVO.setIsKeyIdDataAttribute("0"); // 未知逻辑
                            dataAttributeVO.setFormatValueOnly(AttributeEnum.CURRENT.getFormatValueOnly());
                            dataAttributeVO.setFormatWithUnit(AttributeEnum.CURRENT.getFormatWithUnit());
                            dataAttributeVO.setDataAttributeName(AttributeEnum.CURRENT.getDataAttributeName());
                            dataAttributeVO.setRawValue(valueStr);
                            dataAttributeVO.setFormattedValue(valueStrUnit);
                            dataAttributeVO.setValueFormattedValueOnly(valueStr);
                            dataAttributeVO.setValueFormattedWithUnit(valueStrUnit);
                            dataAttributeVO.setHasOldData(false); // 未知逻辑
                            dataMap.put(AttributeEnum.CURRENT.getIdDataAttribute(), dataAttributeVO);
                            break;
                        case VOLTAGE_SUM:
                            valueStr = formatValue(String.valueOf(value), AttributeEnum.VOLTAGE.getFormatValueOnly());
                            valueStrUnit = formatValue(String.valueOf(value), AttributeEnum.VOLTAGE.getFormatWithUnit());
                            dataAttributeVO.setCode(AttributeEnum.VOLTAGE.getCode());
                            dataAttributeVO.setIdDataAttribute(AttributeEnum.VOLTAGE.getIdDataAttribute());
                            dataAttributeVO.setSecondsAgo(secondsAgo); // 未知逻辑
                            dataAttributeVO.setSecondsToNextLog("60"); // 未知逻辑
                            dataAttributeVO.setValue(valueStr);
                            dataAttributeVO.setValueFloat(valueStr); // 根据soc所知
                            dataAttributeVO.setDataType(AttributeEnum.VOLTAGE.getDataType()); // 根据soc所知
                            dataAttributeVO.setDbusServiceType(AttributeEnum.VOLTAGE.getDbusServiceType());
                            dataAttributeVO.setDbusPath(AttributeEnum.VOLTAGE.getDbusPath()); // 未知逻辑
                            dataAttributeVO.setInstance(Integer.valueOf(brackets.get(0))); // 未知逻辑
                            dataAttributeVO.setValueString(null); // 未知逻辑
                            dataAttributeVO.setValueEnum(null); // 未知逻辑
                            dataAttributeVO.setNameEnum(null); // 未知逻辑
                            dataAttributeVO.setIsValid("1"); // 未知逻辑
                            dataAttributeVO.setIsKeyIdDataAttribute("1"); // 未知逻辑
                            dataAttributeVO.setFormatValueOnly(AttributeEnum.VOLTAGE.getFormatValueOnly());
                            dataAttributeVO.setFormatWithUnit(AttributeEnum.VOLTAGE.getFormatWithUnit());
                            dataAttributeVO.setDataAttributeName(AttributeEnum.VOLTAGE.getDataAttributeName());
                            dataAttributeVO.setRawValue(valueStr);
                            dataAttributeVO.setFormattedValue(valueStrUnit);
                            dataAttributeVO.setValueFormattedValueOnly(valueStr);
                            dataAttributeVO.setValueFormattedWithUnit(valueStrUnit);
                            dataAttributeVO.setHasOldData(false); // 未知逻辑
                            dataMap.put(AttributeEnum.VOLTAGE.getIdDataAttribute(), dataAttributeVO);
                            break;
                        case TEMPERATURE_SUM:
                            valueStr = formatValue(String.valueOf(value), AttributeEnum.BATTERY_TEMPERATURE.getFormatValueOnly());
                            valueStrUnit = formatValue(String.valueOf(value), AttributeEnum.BATTERY_TEMPERATURE.getFormatWithUnit());
                            dataAttributeVO.setCode(AttributeEnum.BATTERY_TEMPERATURE.getCode());
                            dataAttributeVO.setIdDataAttribute(AttributeEnum.BATTERY_TEMPERATURE.getIdDataAttribute());
                            dataAttributeVO.setSecondsAgo(secondsAgo); // 未知逻辑
                            dataAttributeVO.setSecondsToNextLog("60"); // 未知逻辑
                            dataAttributeVO.setValue(valueStr);
                            dataAttributeVO.setValueFloat(valueStr); // 根据soc所知
                            dataAttributeVO.setDataType(AttributeEnum.BATTERY_TEMPERATURE.getDataType()); // 根据soc所知
                            dataAttributeVO.setDbusServiceType(AttributeEnum.BATTERY_TEMPERATURE.getDbusServiceType());
                            dataAttributeVO.setDbusPath(AttributeEnum.BATTERY_TEMPERATURE.getDbusPath()); // 未知逻辑
                            dataAttributeVO.setInstance(Integer.valueOf(brackets.get(0))); // 未知逻辑
                            dataAttributeVO.setValueString(null); // 未知逻辑
                            dataAttributeVO.setValueEnum(null); // 未知逻辑
                            dataAttributeVO.setNameEnum(null); // 未知逻辑
                            dataAttributeVO.setIsValid("1"); // 未知逻辑
                            dataAttributeVO.setIsKeyIdDataAttribute("0"); // 未知逻辑
                            dataAttributeVO.setFormatValueOnly(AttributeEnum.BATTERY_TEMPERATURE.getFormatValueOnly());
                            dataAttributeVO.setFormatWithUnit(AttributeEnum.BATTERY_TEMPERATURE.getFormatWithUnit());
                            dataAttributeVO.setDataAttributeName(AttributeEnum.BATTERY_TEMPERATURE.getDataAttributeName());
                            dataAttributeVO.setRawValue(valueStr);
                            dataAttributeVO.setFormattedValue(valueStrUnit);
                            dataAttributeVO.setValueFormattedValueOnly(valueStr);
                            dataAttributeVO.setValueFormattedWithUnit(valueStr);
                            dataAttributeVO.setHasOldData(false); // 未知逻辑
                            dataMap.put(AttributeEnum.BATTERY_TEMPERATURE.getIdDataAttribute(), dataAttributeVO);
                            break;
                        case MAX_CELL_VOLTAGE:
                            valueStr = formatValue(String.valueOf(value), AttributeEnum.MAXIMUM_CELL_VOLTAGE.getFormatValueOnly());
                            valueStrUnit = formatValue(String.valueOf(value), AttributeEnum.MAXIMUM_CELL_VOLTAGE.getFormatWithUnit());
                            dataAttributeVO.setCode(AttributeEnum.MAXIMUM_CELL_VOLTAGE.getCode());
                            dataAttributeVO.setIdDataAttribute(AttributeEnum.MAXIMUM_CELL_VOLTAGE.getIdDataAttribute());
                            dataAttributeVO.setSecondsAgo(secondsAgo); // 未知逻辑
                            dataAttributeVO.setSecondsToNextLog("60"); // 未知逻辑
                            dataAttributeVO.setValue(valueStr);
                            dataAttributeVO.setValueFloat(valueStr); // 根据soc所知
                            dataAttributeVO.setDataType(AttributeEnum.MAXIMUM_CELL_VOLTAGE.getDataType()); // 根据soc所知
                            dataAttributeVO.setDbusServiceType(AttributeEnum.MAXIMUM_CELL_VOLTAGE.getDbusServiceType());
                            dataAttributeVO.setDbusPath(AttributeEnum.MAXIMUM_CELL_VOLTAGE.getDbusPath()); // 未知逻辑
                            dataAttributeVO.setInstance(Integer.valueOf(brackets.get(0))); // 未知逻辑
                            dataAttributeVO.setValueString(null); // 未知逻辑
                            dataAttributeVO.setValueEnum(null); // 未知逻辑
                            dataAttributeVO.setNameEnum(null); // 未知逻辑
                            dataAttributeVO.setIsValid("1"); // 未知逻辑
                            dataAttributeVO.setIsKeyIdDataAttribute("0"); // 未知逻辑
                            dataAttributeVO.setFormatValueOnly(AttributeEnum.MAXIMUM_CELL_VOLTAGE.getFormatValueOnly());
                            dataAttributeVO.setFormatWithUnit(AttributeEnum.MAXIMUM_CELL_VOLTAGE.getFormatWithUnit());
                            dataAttributeVO.setDataAttributeName(AttributeEnum.MAXIMUM_CELL_VOLTAGE.getDataAttributeName());
                            dataAttributeVO.setRawValue(valueStr);
                            dataAttributeVO.setFormattedValue(valueStrUnit);
                            dataAttributeVO.setValueFormattedValueOnly(valueStr);
                            dataAttributeVO.setValueFormattedWithUnit(valueStrUnit);
                            dataAttributeVO.setHasOldData(false); // 未知逻辑
                            dataMap.put(AttributeEnum.MAXIMUM_CELL_VOLTAGE.getIdDataAttribute(), dataAttributeVO);
                            break;
                        case MIN_CELL_VOLTAGE:
                            valueStr = formatValue(String.valueOf(value), AttributeEnum.MINIMUM_CELL_VOLTAGE.getFormatValueOnly());
                            valueStrUnit = formatValue(String.valueOf(value), AttributeEnum.MINIMUM_CELL_VOLTAGE.getFormatWithUnit());
                            dataAttributeVO.setCode(AttributeEnum.MINIMUM_CELL_VOLTAGE.getCode());
                            dataAttributeVO.setIdDataAttribute(AttributeEnum.MINIMUM_CELL_VOLTAGE.getIdDataAttribute());
                            dataAttributeVO.setSecondsAgo(secondsAgo); // 未知逻辑
                            dataAttributeVO.setSecondsToNextLog("60"); // 未知逻辑
                            dataAttributeVO.setValue(valueStr);
                            dataAttributeVO.setValueFloat(valueStr); // 根据soc所知
                            dataAttributeVO.setDataType(AttributeEnum.MINIMUM_CELL_VOLTAGE.getDataType()); // 根据soc所知
                            dataAttributeVO.setDbusServiceType(AttributeEnum.MINIMUM_CELL_VOLTAGE.getDbusServiceType());
                            dataAttributeVO.setDbusPath(AttributeEnum.MINIMUM_CELL_VOLTAGE.getDbusPath()); // 未知逻辑
                            dataAttributeVO.setInstance(Integer.valueOf(brackets.get(0))); // 未知逻辑
                            dataAttributeVO.setValueString(null); // 未知逻辑
                            dataAttributeVO.setValueEnum(null); // 未知逻辑
                            dataAttributeVO.setNameEnum(null); // 未知逻辑
                            dataAttributeVO.setIsValid("1"); // 未知逻辑
                            dataAttributeVO.setIsKeyIdDataAttribute("0"); // 未知逻辑
                            dataAttributeVO.setFormatValueOnly(AttributeEnum.MINIMUM_CELL_VOLTAGE.getFormatValueOnly());
                            dataAttributeVO.setFormatWithUnit(AttributeEnum.MINIMUM_CELL_VOLTAGE.getFormatWithUnit());
                            dataAttributeVO.setDataAttributeName(AttributeEnum.MINIMUM_CELL_VOLTAGE.getDataAttributeName());
                            dataAttributeVO.setRawValue(valueStr);
                            dataAttributeVO.setFormattedValue(valueStrUnit);
                            dataAttributeVO.setValueFormattedValueOnly(valueStr);
                            dataAttributeVO.setValueFormattedWithUnit(valueStrUnit);
                            dataAttributeVO.setHasOldData(false); // 未知逻辑
                            dataMap.put(AttributeEnum.MINIMUM_CELL_VOLTAGE.getIdDataAttribute(), dataAttributeVO);
                            break;
                        case CHARGED_ENERGY:
                        case DISCHARGED_ENERGY:
                            if (dataMap.containsKey(AttributeEnum.CONSUMED_AMPHOURS.getIdDataAttribute())) {
                                DataAttributeVO old = (DataAttributeVO)dataMap.get(AttributeEnum.CONSUMED_AMPHOURS.getIdDataAttribute());
                                newValueStr = add(old.getValue(), String.valueOf(value), AttributeEnum.CONSUMED_AMPHOURS.getFormatValueOnly());
                                newValueStrUnit = add(old.getValue(), String.valueOf(value), AttributeEnum.CONSUMED_AMPHOURS.getFormatWithUnit());
                                old.setValue(newValueStr);
                                old.setValueFloat(newValueStr);
                                old.setValueFormattedValueOnly(newValueStr);
                                old.setRawValue(newValueStr);
                                old.setFormattedValue(newValueStrUnit);
                                old.setValueFormattedWithUnit(newValueStrUnit);
                            } else {
                                valueStr = formatValue(String.valueOf(value), AttributeEnum.CONSUMED_AMPHOURS.getFormatValueOnly());
                                valueStrUnit = formatValue(String.valueOf(value), AttributeEnum.CONSUMED_AMPHOURS.getFormatWithUnit());
                                dataAttributeVO.setCode(AttributeEnum.CONSUMED_AMPHOURS.getCode());
                                dataAttributeVO.setIdDataAttribute(AttributeEnum.CONSUMED_AMPHOURS.getIdDataAttribute());
                                dataAttributeVO.setSecondsAgo(secondsAgo); // 未知逻辑
                                dataAttributeVO.setSecondsToNextLog("60"); // 未知逻辑
                                dataAttributeVO.setValue(valueStr);
                                dataAttributeVO.setValueFloat(valueStr); // 根据soc所知
                                dataAttributeVO.setDataType(AttributeEnum.CONSUMED_AMPHOURS.getDataType()); // 根据soc所知
                                dataAttributeVO.setDbusServiceType(AttributeEnum.CONSUMED_AMPHOURS.getDbusServiceType());
                                dataAttributeVO.setDbusPath(AttributeEnum.CONSUMED_AMPHOURS.getDbusPath()); // 未知逻辑
                                dataAttributeVO.setInstance(Integer.valueOf(brackets.get(0))); // 未知逻辑
                                dataAttributeVO.setValueString(null); // 未知逻辑
                                dataAttributeVO.setValueEnum(null); // 未知逻辑
                                dataAttributeVO.setNameEnum(null); // 未知逻辑
                                dataAttributeVO.setIsValid("1"); // 未知逻辑
                                dataAttributeVO.setIsKeyIdDataAttribute("0"); // 未知逻辑
                                dataAttributeVO.setFormatValueOnly(AttributeEnum.CONSUMED_AMPHOURS.getFormatValueOnly());
                                dataAttributeVO.setFormatWithUnit(AttributeEnum.CONSUMED_AMPHOURS.getFormatWithUnit());
                                dataAttributeVO.setDataAttributeName(AttributeEnum.CONSUMED_AMPHOURS.getDataAttributeName());
                                dataAttributeVO.setRawValue(valueStr);
                                dataAttributeVO.setFormattedValue(valueStrUnit);
                                dataAttributeVO.setValueFormattedValueOnly(valueStr);
                                dataAttributeVO.setValueFormattedWithUnit(valueStrUnit);
                                dataAttributeVO.setHasOldData(false); // 未知逻辑
                                dataMap.put(AttributeEnum.CONSUMED_AMPHOURS.getIdDataAttribute(), dataAttributeVO);
                            }
                            break;
                    }
                }
                dataMap.put("hasOldData", false); // 未知逻辑
                dataMap.put("secondsAgo", new DataSecondsAgoVO(secondsAgo, String.format("%ss", secondsAgo)));
                batterySummaryVO.setData(dataMap);
                batterySummaryVO.setMeta(createBatterySummaryMetaMap());
                batterySummaryVO.setAttributeOrder(attributeOrder);
                response.setResult(new ResponseEntity<>(R.success(batterySummaryVO), HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch historical data", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private Map<String, MetaVO> createBatterySummaryMetaMap() {
        Map<String, MetaVO> metaVOMap = new LinkedHashMap<>();
        metaVOMap.put(MetaEnum.VOLTAGE.getIdDataAttribute(), new MetaVO(MetaEnum.VOLTAGE.getCode(),
                MetaEnum.VOLTAGE.getDescription(), MetaEnum.VOLTAGE.getFormatValueOnly(),
                MetaEnum.VOLTAGE.getFormatWithUnit(), MetaEnum.VOLTAGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.STARTER_BATTERY_VOLTAGE.getIdDataAttribute(), new MetaVO(MetaEnum.STARTER_BATTERY_VOLTAGE.getCode(),
                MetaEnum.STARTER_BATTERY_VOLTAGE.getDescription(), MetaEnum.STARTER_BATTERY_VOLTAGE.getFormatValueOnly(),
                MetaEnum.STARTER_BATTERY_VOLTAGE.getFormatWithUnit(), MetaEnum.STARTER_BATTERY_VOLTAGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.CURRENT.getIdDataAttribute(), new MetaVO(MetaEnum.CURRENT.getCode(),
                MetaEnum.CURRENT.getDescription(), MetaEnum.CURRENT.getFormatValueOnly(),
                MetaEnum.CURRENT.getFormatWithUnit(), MetaEnum.CURRENT.getAxisGroup()));
        metaVOMap.put(MetaEnum.CONSUMED_AMPHOURS.getIdDataAttribute(), new MetaVO(MetaEnum.CONSUMED_AMPHOURS.getCode(),
                MetaEnum.CONSUMED_AMPHOURS.getDescription(), MetaEnum.CONSUMED_AMPHOURS.getFormatValueOnly(),
                MetaEnum.CONSUMED_AMPHOURS.getFormatWithUnit(), MetaEnum.CONSUMED_AMPHOURS.getAxisGroup()));
        metaVOMap.put(MetaEnum.STATE_OF_CHARGE.getIdDataAttribute(), new MetaVO(MetaEnum.STATE_OF_CHARGE.getCode(),
                MetaEnum.STATE_OF_CHARGE.getDescription(), MetaEnum.STATE_OF_CHARGE.getFormatValueOnly(),
                MetaEnum.STATE_OF_CHARGE.getFormatWithUnit(), MetaEnum.STATE_OF_CHARGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.TIME_TO_GO.getIdDataAttribute(), new MetaVO(MetaEnum.TIME_TO_GO.getCode(),
                MetaEnum.TIME_TO_GO.getDescription(), MetaEnum.TIME_TO_GO.getFormatValueOnly(),
                MetaEnum.TIME_TO_GO.getFormatWithUnit(), MetaEnum.TIME_TO_GO.getAxisGroup()));
        metaVOMap.put(MetaEnum.RELAY_STATUS.getIdDataAttribute(), new MetaVO(MetaEnum.RELAY_STATUS.getCode(),
                MetaEnum.RELAY_STATUS.getDescription(), MetaEnum.RELAY_STATUS.getFormatValueOnly(),
                MetaEnum.RELAY_STATUS.getFormatWithUnit(), MetaEnum.RELAY_STATUS.getAxisGroup()));
        metaVOMap.put(MetaEnum.BATTERY_TEMPERATURE.getIdDataAttribute(), new MetaVO(MetaEnum.BATTERY_TEMPERATURE.getCode(),
                MetaEnum.BATTERY_TEMPERATURE.getDescription(), MetaEnum.BATTERY_TEMPERATURE.getFormatValueOnly(),
                MetaEnum.BATTERY_TEMPERATURE.getFormatWithUnit(), MetaEnum.BATTERY_TEMPERATURE.getAxisGroup()));
        metaVOMap.put(MetaEnum.MID_POINT_VOLTAGE.getIdDataAttribute(), new MetaVO(MetaEnum.MID_POINT_VOLTAGE.getCode(),
                MetaEnum.MID_POINT_VOLTAGE.getDescription(), MetaEnum.MID_POINT_VOLTAGE.getFormatValueOnly(),
                MetaEnum.MID_POINT_VOLTAGE.getFormatWithUnit(), MetaEnum.MID_POINT_VOLTAGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.LOW_STARTER_VOLTAGE_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.LOW_STARTER_VOLTAGE_ALARM.getCode(),
                MetaEnum.LOW_STARTER_VOLTAGE_ALARM.getDescription(), MetaEnum.LOW_STARTER_VOLTAGE_ALARM.getFormatValueOnly(),
                MetaEnum.LOW_STARTER_VOLTAGE_ALARM.getFormatWithUnit(), MetaEnum.LOW_STARTER_VOLTAGE_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.HIGH_STARTER_VOLTAGE_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.HIGH_STARTER_VOLTAGE_ALARM.getCode(),
                MetaEnum.HIGH_STARTER_VOLTAGE_ALARM.getDescription(), MetaEnum.HIGH_STARTER_VOLTAGE_ALARM.getFormatValueOnly(),
                MetaEnum.HIGH_STARTER_VOLTAGE_ALARM.getFormatWithUnit(), MetaEnum.HIGH_STARTER_VOLTAGE_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.LOW_STATE_OF_CHARGE_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.LOW_STATE_OF_CHARGE_ALARM.getCode(),
                MetaEnum.LOW_STATE_OF_CHARGE_ALARM.getDescription(), MetaEnum.LOW_STATE_OF_CHARGE_ALARM.getFormatValueOnly(),
                MetaEnum.LOW_STATE_OF_CHARGE_ALARM.getFormatWithUnit(), MetaEnum.LOW_STATE_OF_CHARGE_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.LOW_BATTERY_TEMPERATURE_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.LOW_BATTERY_TEMPERATURE_ALARM.getCode(),
                MetaEnum.LOW_BATTERY_TEMPERATURE_ALARM.getDescription(), MetaEnum.LOW_BATTERY_TEMPERATURE_ALARM.getFormatValueOnly(),
                MetaEnum.LOW_BATTERY_TEMPERATURE_ALARM.getFormatWithUnit(), MetaEnum.LOW_BATTERY_TEMPERATURE_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.HIGH_BATTERY_TEMPERATURE_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.HIGH_BATTERY_TEMPERATURE_ALARM.getCode(),
                MetaEnum.HIGH_BATTERY_TEMPERATURE_ALARM.getDescription(), MetaEnum.HIGH_BATTERY_TEMPERATURE_ALARM.getFormatValueOnly(),
                MetaEnum.HIGH_BATTERY_TEMPERATURE_ALARM.getFormatWithUnit(), MetaEnum.HIGH_BATTERY_TEMPERATURE_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.MID_VOLTAGE_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.MID_VOLTAGE_ALARM.getCode(),
                MetaEnum.MID_VOLTAGE_ALARM.getDescription(), MetaEnum.MID_VOLTAGE_ALARM.getFormatValueOnly(),
                MetaEnum.MID_VOLTAGE_ALARM.getFormatWithUnit(), MetaEnum.MID_VOLTAGE_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.LOW_FUSED_VOLTAGE_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.LOW_FUSED_VOLTAGE_ALARM.getCode(),
                MetaEnum.LOW_FUSED_VOLTAGE_ALARM.getDescription(), MetaEnum.LOW_FUSED_VOLTAGE_ALARM.getFormatValueOnly(),
                MetaEnum.LOW_FUSED_VOLTAGE_ALARM.getFormatWithUnit(), MetaEnum.LOW_FUSED_VOLTAGE_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.HIGH_FUSED_VOLTAGE_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.HIGH_FUSED_VOLTAGE_ALARM.getCode(),
                MetaEnum.HIGH_FUSED_VOLTAGE_ALARM.getDescription(), MetaEnum.HIGH_FUSED_VOLTAGE_ALARM.getFormatValueOnly(),
                MetaEnum.HIGH_FUSED_VOLTAGE_ALARM.getFormatWithUnit(), MetaEnum.HIGH_FUSED_VOLTAGE_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.FUSE_BLOWN_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.FUSE_BLOWN_ALARM.getCode(),
                MetaEnum.FUSE_BLOWN_ALARM.getDescription(), MetaEnum.FUSE_BLOWN_ALARM.getFormatValueOnly(),
                MetaEnum.FUSE_BLOWN_ALARM.getFormatWithUnit(), MetaEnum.FUSE_BLOWN_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.HIGH_INTERNAL_TEMPERATURE_ALARM.getIdDataAttribute(), new MetaVO(MetaEnum.HIGH_INTERNAL_TEMPERATURE_ALARM.getCode(),
                MetaEnum.HIGH_INTERNAL_TEMPERATURE_ALARM.getDescription(), MetaEnum.HIGH_INTERNAL_TEMPERATURE_ALARM.getFormatValueOnly(),
                MetaEnum.HIGH_INTERNAL_TEMPERATURE_ALARM.getFormatWithUnit(), MetaEnum.HIGH_INTERNAL_TEMPERATURE_ALARM.getAxisGroup()));
        metaVOMap.put(MetaEnum.MAXIMUM_CELL_VOLTAGE.getIdDataAttribute(), new MetaVO(MetaEnum.MAXIMUM_CELL_VOLTAGE.getCode(),
                MetaEnum.MAXIMUM_CELL_VOLTAGE.getDescription(), MetaEnum.MAXIMUM_CELL_VOLTAGE.getFormatValueOnly(),
                MetaEnum.MAXIMUM_CELL_VOLTAGE.getFormatWithUnit(), MetaEnum.MAXIMUM_CELL_VOLTAGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.MINIMUM_CELL_VOLTAGE.getIdDataAttribute(), new MetaVO(MetaEnum.MINIMUM_CELL_VOLTAGE.getCode(),
                MetaEnum.MINIMUM_CELL_VOLTAGE.getDescription(), MetaEnum.MINIMUM_CELL_VOLTAGE.getFormatValueOnly(),
                MetaEnum.MINIMUM_CELL_VOLTAGE.getFormatWithUnit(), MetaEnum.MINIMUM_CELL_VOLTAGE.getAxisGroup()));
        metaVOMap.put(MetaEnum.EXTERNAL_RELAY.getIdDataAttribute(), new MetaVO(MetaEnum.EXTERNAL_RELAY.getCode(),
                MetaEnum.EXTERNAL_RELAY.getDescription(), MetaEnum.EXTERNAL_RELAY.getFormatValueOnly(),
                MetaEnum.EXTERNAL_RELAY.getFormatWithUnit(), MetaEnum.EXTERNAL_RELAY.getAxisGroup()));
        return metaVOMap;
    }

    private String formatValue(String value, String format) {
        if (value.matches("^[-+]?(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)$")) {
            Double ff = Double.parseDouble(value);
            return String.format(format, ff);
        }
        return String.format(format, value);
    }
    private String add(String oldValue, String value, String format) {
        BigDecimal add = new BigDecimal(oldValue).add(new BigDecimal(value));
        return formatValue(add.toString(), format);
    }
}
