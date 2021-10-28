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
package com.futurewei.alcor.privateipmanager.entity;

import com.futurewei.alcor.common.db.CacheException;
import com.futurewei.alcor.common.db.ICache;
import com.futurewei.alcor.privateipmanager.allocator.IpAddrAllocator;
import com.futurewei.alcor.privateipmanager.allocator.Ipv4AddrAllocator;
import com.futurewei.alcor.privateipmanager.allocator.Ipv6AddrAllocator;
import com.futurewei.alcor.privateipmanager.exception.IpAddrAllocNotFoundException;
import com.futurewei.alcor.privateipmanager.exception.IpAddrConflictException;
import com.futurewei.alcor.privateipmanager.exception.IpAddrInvalidException;
import com.futurewei.alcor.privateipmanager.utils.Ipv4AddrUtil;
import com.futurewei.alcor.privateipmanager.utils.Ipv6AddrUtil;
import com.futurewei.alcor.web.entity.ip.IpAddrState;
import com.futurewei.alcor.web.entity.ip.IpVersion;

import java.math.BigInteger;
import java.util.*;

public class IpAddrRange {
    private String id;
    private String vpcId;
    private String subnetId;
    private int ipVersion;
    private String firstIp;
    private String lastIp;
    private long usedIps;
    private long totalIps;
    private IpAddrAllocator allocator;

    public IpAddrRange(String id, String vpcId, String subnetId, int ipVersion, String firstIp, String lastIp) {
        this.id = id;
        this.vpcId = vpcId;
        this.subnetId = subnetId;
        this.ipVersion = ipVersion;
        this.firstIp = firstIp;
        this.lastIp = lastIp;

        if (ipVersion == IpVersion.IPV4.getVersion()) {
            long firstIpLong = Ipv4AddrUtil.ipv4ToLong(firstIp);
            long lastIpLong = Ipv4AddrUtil.ipv4ToLong(lastIp);

            totalIps = lastIpLong - firstIpLong + 1;
            allocator = new Ipv4AddrAllocator(firstIpLong, lastIpLong);
        } else {
            BigInteger firstIpBigInt = Ipv6AddrUtil.ipv6ToBitInt(firstIp);
            BigInteger lastIpBigInt = Ipv6AddrUtil.ipv6ToBitInt(lastIp);

            totalIps = lastIpBigInt.subtract(firstIpBigInt).longValue() + 1;
            allocator = new Ipv6AddrAllocator(firstIpBigInt, lastIpBigInt);
        }

        //allocatedIps = new HashMap<>();
    }

    public IpAddrAlloc allocate(String ip) throws Exception {
        if (ip != null && allocator.isIpAllocated(ip)) {
            throw new IpAddrConflictException();
        }

        String ipAddr = allocator.allocate(ip);
        IpAddrAlloc ipAddrAlloc = new IpAddrAlloc(ipVersion, subnetId, id, ipAddr, IpAddrState.ACTIVATED.getState());

        return ipAddrAlloc;
    }

    @Deprecated
    public List<IpAddrAlloc> allocateBulk(int num) throws Exception {
        List<String> ipAddrList = allocator.allocateBulk(num);
        List<IpAddrAlloc> ipAddrAllocs = new ArrayList<>();

        for (String ipAddr: ipAddrList) {
            IpAddrAlloc ipAddrAlloc = new IpAddrAlloc(ipVersion, subnetId, id, ipAddr, IpAddrState.ACTIVATED.getState());

            ipAddrAllocs.add(ipAddrAlloc);
        }

        return ipAddrAllocs;
    }

    public List<IpAddrAlloc> allocateBulk(List<String> ipAddrList) throws Exception {
        List<IpAddrAlloc> ipAddrAllocList = new ArrayList<>();

        for (String ip: ipAddrList) {
            String ipAddr;
            try {
                ipAddr = allocator.allocate(ip);
            } catch (Exception e) {
                break;
            }

            IpAddrAlloc ipAddrAlloc = new IpAddrAlloc(ipVersion, subnetId, id,
                    ipAddr, IpAddrState.ACTIVATED.getState());

            ipAddrAllocList.add(ipAddrAlloc);
        }

        return ipAddrAllocList;
    }

    public void modifyIpAddrState(String ipAddr, String state) throws Exception {
        int index = allocator.getIpIndex(ipAddr);
        if (!allocator.isIpAllocated(ipAddr)) {
            throw new IpAddrAllocNotFoundException();
        }

        allocator.setIpStatus(index, IpAddrState.valueOf(state).ordinal());
    }

    public void release(String ipAddr) throws Exception {
        int index = allocator.getIpIndex(ipAddr);
        if (allocator.isIpAllocated(ipAddr)) {
            throw new IpAddrAllocNotFoundException();
        }

        allocator.release(ipAddr);
    }

    public void releaseBulk(List<String> ipAddrList) throws Exception {
        allocator.releaseBulk(ipAddrList);
        for (String ipAddr: ipAddrList) {
            if (allocator.isIpAllocated(ipAddr)) {
                throw new IpAddrAllocNotFoundException();
            }
        }
    }

    public IpAddrAlloc getIpAddr(String ipAddr) throws Exception {
        int index = allocator.getIpIndex(ipAddr);
        if (allocator.isIpAllocated(ipAddr)) {
            return new IpAddrAlloc(ipVersion, subnetId, id, ipAddr, ((IpAddrState.values())[allocator.getIpStatusOrdinal(index)]).toString());
        }

        if (allocator.validate(ipAddr)) {
            return new IpAddrAlloc(ipVersion, subnetId, id, ipAddr, IpAddrState.FREE.getState());
        }

        throw new IpAddrInvalidException();
    }

    public Collection<IpAddrAlloc> getIpAddrBulk() throws Exception {
        List<String> ipAddrs = allocator.getAllIps();
        List<IpAddrAlloc> ips = new ArrayList<>();
        for (String ip : ipAddrs) {
            ips.add(new IpAddrAlloc(ipVersion, subnetId, id, ip, ((IpAddrState.values())[allocator.getIpIndex(ip)]).toString()));
        }
        return ips;
    }

    public int getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(int ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public long getUsedIps() {
        try {
            return allocator.getAllIps().size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getFirstIp() {
        return firstIp;
    }

    public void setFirstIp(String firstIp) {
        this.firstIp = firstIp;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public IpAddrAllocator getAllocator() {
        return allocator;
    }

    public void setAllocator(IpAddrAllocator allocator) {
        this.allocator = allocator;
    }

    public void setUsedIps(int usedIps) {
        this.usedIps = usedIps;
    }

    public long getTotalIps() {
        return totalIps;
    }

    public void setTotalIps(int totalIps) {
        this.totalIps = totalIps;
    }

    @Override
    public String toString() {
        return "IpAddrRange{" +
                "id='" + id + '\'' +
                ", subnetId='" + subnetId + '\'' +
                ", ipVersion=" + ipVersion +
                ", firstIp='" + firstIp + '\'' +
                ", lastIp='" + lastIp + '\'' +
                ", usedIps=" + usedIps +
                ", totalIps=" + totalIps +
                ", allocator=" + allocator +
                '}';
    }
}