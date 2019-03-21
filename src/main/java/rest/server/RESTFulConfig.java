package rest.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "server")
public class RESTFulConfig {
    public static String _port="8001";
    public static String _address="127.0.0.1";
    public String port;
    public String address;

    public void setPort(String port) {
        this.port = port;
        _port = port;
    }

    public void setAddress(String address) {
        this.address = address;
        _address = address;
    }
}
