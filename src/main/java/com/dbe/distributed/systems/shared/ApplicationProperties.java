package com.dbe.distributed.systems.shared;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ApplicationProperties {

    private static final String PROP_TCP_SERVER_PORT = "TCP_SERVER_PORT";
    private static final String PROP_MEMBER_ID = "MEMBER_ID";
    private static final String PROP_IP_ADDRESS = "IP_ADDRESS";

    public static Integer getTcpServerPort() {
        return Integer.parseInt(getProperty(PROP_TCP_SERVER_PORT));
    }

    public static String getMemberId() {
        return getProperty(PROP_MEMBER_ID);
    }

    public static InetAddress getIpAddress() {
        try {
            return InetAddress.getByName(getProperty(PROP_IP_ADDRESS));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getProperty(final String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(String.format("The property %s is required.", propertyName));
        }
        return value;
    }


    public static InetAddress getMulticastAddress() {
        try {
            return InetAddress.getByName("225.6.7.8");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


}
