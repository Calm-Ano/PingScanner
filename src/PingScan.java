package src;

import java.io.IOException;
import java.net.InetAddress;

public class PingScan {
    private String hostname;
    private int alive_time;
    PingScan(String hostname, int alive_time){
        this.hostname = hostname;
        this.alive_time = alive_time;
    }

    public boolean ping_to() throws IOException {
        InetAddress ip_iddr = InetAddress.getByName(this.hostname);
        return ip_iddr.isReachable(this.alive_time);
    }

    public static void main(String args[]) throws IOException {
        PingScan test = new PingScan("192.168.10.100", 1000);
        System.out.println(test.ping_to());
    }
}
