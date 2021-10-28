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
package com.futurewei.alcor.privateipmanager.allocator;

import com.futurewei.alcor.privateipmanager.exception.IpAddrInvalidException;
import com.futurewei.alcor.privateipmanager.exception.IpAddrNotEnoughException;
import com.futurewei.alcor.privateipmanager.utils.Ipv4AddrUtil;
import com.futurewei.alcor.web.entity.ip.IpAddrState;

import java.util.*;
import java.util.stream.Collectors;


public class Ipv4AddrAllocator implements IpAddrAllocator {
    private BitSet bitSet;
    private long firstIp;
    private long lastIp;
    private long ipAddrNum;
    private BitSet ipStatusIndex0;
    private BitSet ipStatusIndex1;


    public Ipv4AddrAllocator(long firstIp, long lastIp) {
        this.firstIp = firstIp;
        this.lastIp = lastIp;
        ipAddrNum = lastIp - firstIp + 1;
        bitSet = new BitSet();
        ipStatusIndex0 = new BitSet();
        ipStatusIndex1 = new BitSet();
    }

    @Override
    public String allocate(String ipAddr) throws Exception {
        int freeBit;

        if (ipAddr != null) {
            long ipLong = Ipv4AddrUtil.ipv4ToLong(ipAddr);
            if (ipLong < firstIp || ipLong > lastIp) {
                throw new IpAddrInvalidException();
            }

            freeBit = (int)(ipLong - firstIp);
        } else {
            freeBit = bitSet.nextClearBit(0);

            if (freeBit < 0 || freeBit >= ipAddrNum) {
                throw new IpAddrNotEnoughException();
            }
        }

        bitSet.set(freeBit);
        setIpStatus(IpAddrState.ACTIVATED.ordinal(), freeBit);

        return Ipv4AddrUtil.longToIpv4(firstIp + freeBit);
    }

    @Override
    public int getIpIndex(String ip) throws Exception {
        long ipLong = Ipv4AddrUtil.ipv4ToLong(ip);
        if (ipLong < firstIp || ipLong > lastIp) {
            throw new IpAddrInvalidException();
        }

        return (int)(ipLong - firstIp);
    }


    @Override
    public List<String> getAllIps() throws Exception {
        List<String> ipAddrs = new ArrayList<>();
        long[] ips = bitSet.toLongArray();
        for (long ip : ips) {
            ipAddrs.add(Ipv4AddrUtil.longToIpv4(ip));
        }
        return ipAddrs;
    }

    @Override
    public String getIp(int index) throws Exception {
        return Ipv4AddrUtil.longToIpv4(firstIp + index);
    }

    @Override
    public List<String> allocateBulk(int num) throws Exception {
        List<Integer> freeBits = new ArrayList<>();
        List<String> ipv4AddrList = new ArrayList<>();

        int freeBit = 0;
        while (freeBits.size() < num) {
            freeBit = bitSet.nextClearBit(freeBit + 1);
            if (freeBit < 0 || freeBit >= ipAddrNum) {
                throw new IpAddrNotEnoughException();
            }

            freeBits.add(freeBit);
        }

        for (int bit: freeBits) {
            bitSet.set(bit);
            String ipv4Addr = Ipv4AddrUtil.longToIpv4(firstIp + bit);
            ipv4AddrList.add(ipv4Addr);
            setIpStatus(IpAddrState.ACTIVATED.ordinal(), bit);
        }

        return ipv4AddrList;
    }

    public void release(String ipAddr) throws Exception {
        long ipLong = Ipv4AddrUtil.ipv4ToLong(ipAddr);
        if (ipLong < firstIp || ipLong > lastIp) {
            throw new IpAddrInvalidException();
        }

        bitSet.clear((int)(ipLong - firstIp));
        ipStatusIndex0.clear((int)(ipLong - firstIp));
        ipStatusIndex1.clear((int)(ipLong - firstIp));
    }

    @Override
    public void releaseBulk(List<String> ipAddrList) throws Exception {
        for (String ipAddr: ipAddrList) {
            release(ipAddr);
        }
    }

    @Override
    public void setIpStatus(int ordinal, int ip) {
        switch (ordinal) {
            case 0:
                if (ipStatusIndex0.get(ip)) {
                    ipStatusIndex0.clear(ip);
                }
                if (ipStatusIndex1.get(ip)) {
                    ipStatusIndex1.clear(ip);
                }
                break;
            case 1:
                ipStatusIndex0.set(ip);
                if (ipStatusIndex1.get(ip)) {
                    ipStatusIndex1.clear(ip);
                }
                break;
            case 2:
                if (ipStatusIndex0.get(ip)) {
                    ipStatusIndex0.clear(ip);
                }
                ipStatusIndex1.set(ip);
                break;
            default:
        }
    }

    @Override
    public int getIpStatusOrdinal(int ip) {
        boolean index0 = ipStatusIndex0.get(ip);
        boolean index1 = ipStatusIndex1.get(ip);
        int ordinal = 0;
        if (index0) {
            ordinal++;
        }
        if (index1) {
            ordinal += 2;
        }
        return ordinal;
    }


    @Override
    public boolean validate(String ipAddr) {
        long ipLong = Ipv4AddrUtil.ipv4ToLong(ipAddr);

        return ipLong >= firstIp && ipLong <= lastIp;
    }

    @Override
    public boolean isIpAllocated(String ipAddr) {
        long ipLong = Ipv4AddrUtil.ipv4ToLong(ipAddr);

        return bitSet.get((int)(ipLong - firstIp));
    }

    public BitSet getBitSet() {
        return bitSet;
    }

    public void setBitSet(BitSet bitSet) {
        this.bitSet = bitSet;
    }

    public long getFirstIp() {
        return firstIp;
    }

    public void setFirstIp(long firstIp) {
        this.firstIp = firstIp;
    }

    public long getLastIp() {
        return lastIp;
    }

    public void setLastIp(long lastIp) {
        this.lastIp = lastIp;
    }

    public long getIpAddrNum() {
        return ipAddrNum;
    }

    public void setIpAddrNum(long ipAddrNum) {
        this.ipAddrNum = ipAddrNum;
    }

}
