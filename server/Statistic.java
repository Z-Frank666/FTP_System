package com.Frank.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Statistic {
    private int id;
    // total_connections
    private int total_connections;
    // files_downloaded
    private int files_downloaded;
    // files_uploaded
    private int files_uploaded;
    // failed_downloads
    private int failed_downloads;
    // failed_uploads
    private int failed_uploads;
    // total_kilobytes_reveived (注意拼写与数据库一致)
    private int total_kilobytes_reveived;
    // total_kilobytes_sent
    private int total_kilobytes_sent;

    public static void main(String[] args) throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();
        System.out.println("Local HostAddress: " + addr.getHostAddress());
        String hostname = addr.getHostName();
        System.out.println("Local host name: " + hostname);
    }
}
