package cn.com.turing.search.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * TuringSearch 配置类
 */
public class TsConfig {

    private static final String CLUSTERNAME = "clusterName";
    private static final String HOSTS = "hosts";
    private static final String PORT = "port";
    private static TsConfig config = null;
    /**
     * TuringSearch 集群名称
     */
    private String clusterName;

    /**
     * TuringSearch TCP服务端口号
     */
    private String port;

    /**
     * TuringSearch 集群主机IP列表，一个或多个
     */
    private String[] hosts;

    private TsConfig() {
        init();
    }

    /**
     * 返回TSConfig实例
     */
    public static TsConfig getIntance() {
        return ConfigInstance.INSTANCE;
    }

    public static void main(String[] args) {
        TsConfig esConfig = TsConfig.getIntance();
        System.out.println(esConfig.toString());
    }

    /**
     * 读配置文件es-config.properties
     */
    private Properties readConfig() {
        Properties props = new Properties();
        try {
            InputStream stream = this.getClass().getClassLoader().getResourceAsStream("es-config.properties");
            props.load(stream);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return props;
    }

    /**
     * 根据prop配置，初始化config对象
     */
    private void init() {
        Properties pros = this.readConfig();
        String clustername = pros.getProperty(TsConfig.CLUSTERNAME);
        if (clustername != null) {
            config.clusterName = clustername;
        }
        String hosts = pros.getProperty(TsConfig.HOSTS);
        if (hosts != null) {
            String addresss = hosts.toString();
            if (addresss.indexOf(",") != -1) {
                config.hosts = addresss.split(",");
            } else {
                config.hosts = new String[]{addresss};
            }
        }
        String port = pros.getProperty(TsConfig.PORT);
        if (port != null) {
            config.port = port;
        }
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String[] getHosts() {
        return hosts.clone();
    }

    public void setHosts(String[] hosts) {
        this.hosts = hosts.clone();
    }

    /**
     * 静态内部类
     */
    private static class ConfigInstance {
        private static final TsConfig INSTANCE = new TsConfig();
    }

}
