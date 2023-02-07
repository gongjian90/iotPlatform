/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.cache.newdevice;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class NewDeviceCacheKey implements Serializable {

    private final CustomerId customerId;
    private final DeviceId deviceId;
    private final String deviceName;

    public NewDeviceCacheKey(CustomerId customerId, DeviceId deviceId) {
        this(customerId, deviceId, null);
    }

    public NewDeviceCacheKey(CustomerId customerId, String deviceName) {
        this(customerId, null, deviceName);
    }

    @Override
    public String toString() {
        if (deviceId != null) {
            return customerId + "_" + deviceId;
        } else {
            return customerId + "_n_" + deviceName;
        }
    }

}
