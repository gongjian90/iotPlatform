package org.thingsboard.server.common.data.http;

import lombok.Data;

import java.util.List;

@Data
public abstract class AbstractDataDeviceVO extends AbstractDeviceVO{

    private String instance;
    private String idDeviceType;
    private List<Object> settings;
}
