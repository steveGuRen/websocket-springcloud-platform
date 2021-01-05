package com.springcloud.ufire.conf;

import com.springcloud.ufire.entiy.HashRingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;

import java.util.*;

/**
 * @program: ufire-springcloud-platform
 * @description: X
 * @author: fengandong
 * @create: 2021-01-05 11:11
 **/
public class HashRingConfig {
    private static final Logger log = LoggerFactory.getLogger(HashRingConfig.class);
    //真实结点需要对应的虚拟节点个数
    private static final int VIRTUAL_NODES = 100;

    private HashRingEntity hashRing;

    private List<ServiceInstance> instances;

    public void updateHashRing(Map<String, String> serverMap, Map<String, String> userMap) {
        HashRingEntity hashRingEntity = new HashRingEntity();
        SortedMap<Integer, String> serverSortedMap = new TreeMap<>();
        SortedMap<Integer, String> userSortedMap = new TreeMap<>();
        for (String key : serverMap.keySet()) {
            serverSortedMap.put(Integer.parseInt(key), serverMap.get(key));
        }
        for (String key : userMap.keySet()) {
            userSortedMap.put(Integer.parseInt(key), userMap.get(key));
        }
        hashRingEntity.setLastTimeServerMap(Objects.nonNull(hashRing) ? hashRing.getServerMap() : null);
        hashRingEntity.setServerMap(serverSortedMap);
        hashRingEntity.setUserMap(userSortedMap);
        this.hashRing = hashRingEntity;
    }

    public HashRingEntity getHashRing() {
        return this.hashRing;
    }

    public void setInstances(List<ServiceInstance> instances) {
        this.instances = instances;
    }


    //得到应当路由到的结点
    public ServiceInstance getServer(String key) {
        //得到该key的hash值
        int hash = getHash(key);
        SortedMap<Integer, String> serverMap = hashRing.getServerMap();
        for (Integer serverkey : serverMap.keySet()) {
            if (serverkey > hash) {
                String server = serverMap.get(serverkey);
                if (server.indexOf(("&&")) == -1) {
                    server = server.substring(0, server.indexOf("&&"));
                }
                String host = server.substring(0, server.indexOf(":"));
                String port = server.substring(server.indexOf(":") + 1, server.length());
                for (ServiceInstance instance : instances) {
                    if (instance.getHost().equals(host) && instance.getPort() == Integer.parseInt(port)) {
                        return instance;
                    }
                }
            }
        }
        return null;
    }


    public int getHash(String str) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash ^ str.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        // 如果算出来的值为负数则取其绝对值
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        return hash;
    }

    public void addVirtualNode(HashRingEntity hashRing) {
        SortedMap<Integer, String> serverMap = hashRing.getServerMap();
        SortedMap<Integer, String> virtualServerMap = new TreeMap<>();
        for (String realNode : serverMap.values()) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                String virtualNode = realNode + "&&VN" + String.valueOf(i);
                int virtualNodeHash = getHash(virtualNode);
                virtualServerMap.put(virtualNodeHash, virtualNode);
            }
            log.info("--------------------------------");
        }
        serverMap.putAll(virtualServerMap);
        hashRing.setServerMap(serverMap);
        this.hashRing = hashRing;
    }


//    for (int i = 0; i < VIRTUAL_NODES; i++) {
//        String virtualNode = realNode + "&&VN" + String.valueOf(i);
//        int virtualNodeHash = HashRingUtil.getHash(virtualNode);
//        jedis.hset(SERVER_WEBSOCKET, String.valueOf(virtualNodeHash), virtualNode);
//    }

}
