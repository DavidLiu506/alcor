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
import com.futurewei.alcor.common.stats.DurationStatistics;
import com.futurewei.alcor.common.utils.CommonUtil;
import com.futurewei.alcor.portmanager.entity.SubnetPortIds;
import com.futurewei.alcor.portmanager.exception.FixedIpsInvalid;
import com.futurewei.alcor.web.entity.dataplane.NeighborInfo;
import com.futurewei.alcor.web.entity.port.PortEntity;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.util.*;
import java.util.stream.Collectors;

public class SubnetPortsRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SubnetPortsRepository.class);
    private static final String GATEWAY_PORT_DEVICE_OWNER = "network:router_interface";

    private CacheFactory cacheFactory;
    private ICache<String, SubnetPortIds> subnetPortIdsCache;

    public SubnetPortsRepository(CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
        this.subnetPortIdsCache= cacheFactory.getCache(SubnetPortIds.class);
    }
    /*
    private List<SubnetPortIds> getSubnetPortIds(List<PortEntity> portEntities) throws CacheException {
        Map<String, SubnetPortIds> subnetPortIdsMap = new HashMap<>();
        List<SubnetPortIds> subnetPortIdsList = new ArrayList<>();
        Set<String> subnetIds = portEntities.stream().filter(item -> GATEWAY_PORT_DEVICE_OWNER.equals(item.getDeviceOwner())).flatMap(item -> item
                                                                                                               .getFixedIps()
                                                                                                               .stream()
                                                                                                               .filter(fixedIp -> fixedIp != null)
                                                                                                               .map(fixedIp -> fixedIp.getSubnetId()))
                                                                                                               .collect(Collectors.toSet());
        for (String subnetId : subnetIds) {
            SubnetPortIds subnetPortIds = new SubnetPortIds(subnetId, new HashSet<>());
            IgniteBiPredicate<String, String> filter = (key, value) -> key.contains(subnetId);
            List<String> portIds = this.subnetPortIdsCache.query(new ScanQuery<>(filter));
            for (String portId : portIds) {
                String[] subnetPort = portId.split("/");
                if (subnetPort.length != 2) {
                    continue;
                }
                subnetPortIds.getPortIds().add(portId);
            }
        }

        return subnetPortIdsList;
    }
     */


    public void addSubnetPortIds(List<PortEntity> portEntities) throws Exception {
        //Store the mapping between subnet id and port id
        //List<SubnetPortIds> subnetPortIdsList = getSubnetPortIds(portEntities);

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
                String subnetId = fixedIp.getSubnetId();
                CacheConfiguration cfg = CommonUtil.getCacheConfiguration(subnetId);
                ICache<String, String> cache = cacheFactory.getCache(String.class, cfg);
                cache.put(portEntity.getId(), "1");
                System.out.println("SubnetId: " + subnetId);
            }
        }
    }

    public void updateSubnetPortIds(PortEntity oldPortEntity, PortEntity newPortEntity) throws Exception {
        //Update Subnet port ids cache
        if (oldPortEntity.getFixedIps() == null || newPortEntity.getFixedIps() == null) {
            LOG.error("Can not find fixed ip in port entity");
            throw new FixedIpsInvalid();
        }

        if (GATEWAY_PORT_DEVICE_OWNER.equals(oldPortEntity.getDeviceOwner()) || GATEWAY_PORT_DEVICE_OWNER.equals(newPortEntity.getDeviceOwner())) {
            return;
        }

        oldPortEntity.getFixedIps().forEach(item -> {
            try {
                CacheConfiguration cfg = CommonUtil.getCacheConfiguration(item.getSubnetId());
                ICache<String, String> cache = cacheFactory.getCache(String.class, cfg);
                cache.remove(oldPortEntity.getId());
            } catch (CacheException e) {
                e.printStackTrace();
            }
        });

        newPortEntity.getFixedIps().forEach(item -> {
            try {
                CacheConfiguration cfg = CommonUtil.getCacheConfiguration(item.getSubnetId());
                ICache<String, String> cache = cacheFactory.getCache(String.class, cfg);
                cache.put(newPortEntity.getId(), "1");
            } catch (CacheException e) {
                e.printStackTrace();
            }
        });
    }

    public void deleteSubnetPortIds(PortEntity portEntity) throws Exception {
        if (portEntity.getFixedIps() == null) {
            LOG.error("Can not find fixed ip in port entity");
            throw new FixedIpsInvalid();
        }

        List<String> subnetIds = portEntity.getFixedIps().stream()
                .map(PortEntity.FixedIp::getSubnetId)
                .collect(Collectors.toList());

        //Delete port ids from subnetPortIdsCache
        portEntity.getFixedIps().forEach(item -> {
            try {
                CacheConfiguration cfg = CommonUtil.getCacheConfiguration(item.getSubnetId());
                ICache<String, String> cache = cacheFactory.getCache(String.class, cfg);
                cache.remove(portEntity.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @DurationStatistics
    public int getSubnetPortNumber(String subnetId) throws CacheException {
        CacheConfiguration cfg = CommonUtil.getCacheConfiguration(subnetId);
        return (int)cacheFactory.getCache(String.class, cfg).size();
    }

    @DurationStatistics
    public void createSubnetCache(String subnetId) throws CacheException {
        CacheConfiguration cfg = CommonUtil.getCacheConfiguration(subnetId);
        cacheFactory.getCache(String.class, cfg);
    }
}
