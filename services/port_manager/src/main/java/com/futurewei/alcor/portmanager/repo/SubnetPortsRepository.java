/*
MIT License
Copyright(c) 2020 Futurewei Cloud

    Permission is hereby granted,
    free of charge, to any person obtaining a copy of this software and associated documentation files(the "Software"), to deal in the Software without restriction,
    including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and / or sell copies of the Software, and to permit persons
    to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.futurewei.alcor.portmanager.repo;

import com.futurewei.alcor.common.db.CacheException;
import com.futurewei.alcor.common.db.CacheFactory;
import com.futurewei.alcor.common.db.ICache;
import com.futurewei.alcor.common.db.ignite.IgniteICache;
import com.futurewei.alcor.common.stats.DurationStatistics;
import com.futurewei.alcor.portmanager.entity.SubnetPortIds;
import com.futurewei.alcor.portmanager.exception.FixedIpsInvalid;
import com.futurewei.alcor.web.entity.port.PortEntity;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SubnetPortsRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SubnetPortsRepository.class);
    private static final String GATEWAY_PORT_DEVICE_OWNER = "network:router_interface";

    private CacheFactory cacheFactory;
    private IgniteICache<String, String> portIdSubnetIdCache;

    public SubnetPortsRepository(CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
        this.portIdSubnetIdCache= (IgniteICache)cacheFactory.getCache(String.class);
    }

    /***
     * Add port entry with their subnetId
     * @param portEntities
     * @throws Exception
     */
    public void addSubnetPortIds(List<PortEntity> portEntities) throws Exception {
        //Store the mapping between subnet id and port id
        for (PortEntity portEntity: portEntities) {
            if (GATEWAY_PORT_DEVICE_OWNER.equals(portEntity.getDeviceOwner())) {
                continue;
            }

            List<PortEntity.FixedIp> fixedIps = portEntity.getFixedIps();
            if (fixedIps == null) {
                LOG.warn("Port:{} has no ip address", portEntity.getId());
                continue;
            }

            for (PortEntity.FixedIp fixedIp: fixedIps) {
                portIdSubnetIdCache.put(fixedIp.getSubnetId() + "/" + portEntity.getId(), fixedIp.getSubnetId());
            }
        }
    }

    public void updateSubnetPortIds(PortEntity oldPortEntity, PortEntity newPortEntity) throws Exception {
        //Update Subnet port ids cache
        if (oldPortEntity.getFixedIps() == null || newPortEntity.getFixedIps() == null) {
            LOG.error("Can not find fixed ip in port entity");
            throw new FixedIpsInvalid();
        }

        if (GATEWAY_PORT_DEVICE_OWNER.equals(newPortEntity.getDeviceOwner())) {
            return;
        }

        oldPortEntity.getFixedIps().forEach( item ->
                {
                    try {
                        portIdSubnetIdCache.remove(item.getSubnetId() + "/" + oldPortEntity.getId());
                    } catch (CacheException e) {
                        e.printStackTrace();
                    }
                }
        );

        newPortEntity.getFixedIps().forEach( item ->
                {
                    try {
                        portIdSubnetIdCache.put(item.getSubnetId() + "/" + newPortEntity.getId(), item.getSubnetId());
                    } catch (CacheException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    public void deleteSubnetPortIds(PortEntity portEntity) throws Exception {
        if (portEntity.getFixedIps() == null) {
            LOG.error("Can not find fixed ip in port entity");
            throw new FixedIpsInvalid();
        }

        portEntity.getFixedIps().forEach( item ->
                {
                    try {
                        portIdSubnetIdCache.remove(item.getSubnetId() + "/" + portEntity.getId());
                    } catch (CacheException e) {
                        e.printStackTrace();
                    }
                }
        );

    }

    @DurationStatistics
    public int getSubnetPortNumber(String subnetId) throws CacheException {
        IgniteBiPredicate<String, String> filter = (key, val) -> val.equals(subnetId);
        return portIdSubnetIdCache.getAll(filter).size();
    }
}