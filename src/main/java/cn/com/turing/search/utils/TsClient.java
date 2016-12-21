package cn.com.turing.search.utils;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class TsClient {

    private String clusterName;
    private String[] addressArray; //ES集群的ip，填一个或两个节点IP
    private int port;

    public TsClient(String clusterName, String[] addressArray, int port) {
        this.clusterName = clusterName;
        this.addressArray = addressArray.clone();
        this.port = port;
    }


    /**
     * NodeClient的构建方式
     * NodeBuilder采用建造者的方式构建Node，可以指定参数，
     * clustrName：集群名字
     * Client：Node的角色限定为client，不存储数据
     * local：本地模式，适用于测试开发
     */
    public Client nodeClient() {
        /** 两种方式设置clustername
         * 1，参数指定 nodeBuilder().clusterName("yourclustername")
         * 2，在classpath下面设置yml的配置文件**/
        Node node = nodeBuilder().clusterName(clusterName).client(true).node();
        Client client = node.client();
        return client;
    }

    /**
     * TransportClient的构建方式，不加入集群，只与集群中的一两个节点相连（round_robin）
     * 指定集群也可采用参数指定和配置文件两种方式
     * 其它可用参数：
     * client.transport.ignore_cluster_name
     * client.transport.ping_timeout
     * client.transport.nodes_sampler_interval
     */
    public Client transportClient() {
        //ES 2.3+
        Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName)
                .put("client.transport.sniff", true).build();
        Client client = TransportClient.builder().settings(settings).build();
        try {
            for (String address : addressArray) {
                ((TransportClient) client)
                        .addTransportAddress(
                                new InetSocketTransportAddress(InetAddress.getByName(address), port));
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return client;
    }

}
