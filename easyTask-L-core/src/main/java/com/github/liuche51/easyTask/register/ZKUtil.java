package com.github.liuche51.easyTask.register;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZKUtil {
    private static Logger log = LoggerFactory.getLogger(ZKUtil.class);
    public static  String ZK_SERVER_NAME="";
    //会话超时时间
    private static  int SESSION_TIMEOUT = 30 * 1000;

    //连接超时时间
    private static int CONNECTION_TIMEOUT = 3 * 1000;

    //ZooKeeper服务地址
    private static final String CONNECT_ADDR = "127.0.0.1:2181";

    //创建连接实例
    private static CuratorFramework client = null;
    public static CuratorFramework getClient(){
        if(client!=null)
            return client;
        //1 重试策略：初试时间为1s 重试10次
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        //2 通过工厂创建连接
         client = CuratorFrameworkFactory.builder()
                .connectString(CONNECT_ADDR).connectionTimeoutMs(CONNECTION_TIMEOUT)
                .sessionTimeoutMs(SESSION_TIMEOUT)
                .retryPolicy(retryPolicy)
                .namespace("easyTask-L")//命名空间
                .build();
        //3 开启连接
        client.start();
        System.out.println(ZooKeeper.States.CONNECTED);
        System.out.println(client.getState());
        return client;
    }
    public static void initZK(){
        //测试检查某个节点是否存在
        try {
            CuratorFramework client=getClient();
            Stat stat1 = client.checkExists().forPath("/"+ZK_SERVER_NAME);
            if(stat1!=null) return;
            else {
                //创建当前微服务的名称的永久节点
                client.create().forPath("/"+ZK_SERVER_NAME,null);
            }
        } catch (Exception e) {
           log.error("initZK error.",e);
        }
    }
}
