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
package org.thingsboard.server.cache.installation;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.InstallationId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class InstallationCacheKey implements Serializable {

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final InstallationId installationId;
    private final String portalId;

    public InstallationCacheKey(TenantId tenantId, String portalId) {
        this(tenantId, null, null, portalId);
    }
    public InstallationCacheKey(TenantId tenantId, CustomerId customerId, InstallationId installationId) {
        this(tenantId, customerId, installationId, null);
    }
    public InstallationCacheKey(TenantId tenantId, CustomerId customerId, String portalId) {
        this(tenantId, customerId, null, portalId);
    }

    @Override
    public String toString() {
        String result = tenantId + "_" + customerId + "_" + installationId + "_" + portalId;
        if (customerId == null) {
            result = tenantId + "_" + installationId + "_" + portalId;
        }
        if (installationId == null) {
            result = tenantId + "_" + customerId + "_" + portalId;
        }
        if (portalId == null) {
            result = tenantId + "_" + customerId + "_" + installationId;
        }
        return result;
    }
}
