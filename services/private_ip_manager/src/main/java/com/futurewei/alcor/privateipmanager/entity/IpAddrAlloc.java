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
import com.futurewei.alcor.privateipmanager.utils.Ipv4AddrUtil;

public class IpAddrAlloc {
    IpAddrRange ipAddrRange;
    private long ipAddr;
    private String state;

    public IpAddrAlloc() {
    }

    public IpAddrAlloc(IpAddrRange ipAddrRange, long ipAddr, String state) {
        this.ipAddrRange = ipAddrRange;
        this.ipAddr = ipAddr;
        this.state = state;
    }

    public IpAddrRange getIpAddrRange() {
        return ipAddrRange;
    }

    public void setIpAddrRange (IpAddrRange ipAddrRange) {
        this.ipAddrRange = ipAddrRange;
    }

    public String getIpAddr() {
        return Ipv4AddrUtil.longToIpv4(ipAddr);
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = Ipv4AddrUtil.ipv4ToLong(ipAddr);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "IpAddrAlloc{" +
                ", ipAddr='" + ipAddr + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
