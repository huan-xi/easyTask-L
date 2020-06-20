package com.github.liuche51.easyTask.cluster.leader;

import com.github.liuche51.easyTask.cluster.ClusterService;
import com.github.liuche51.easyTask.cluster.Node;
import com.github.liuche51.easyTask.core.EasyTaskConfig;
import com.github.liuche51.easyTask.dto.zk.ZKNode;
import com.github.liuche51.easyTask.register.ZKService;
import com.github.liuche51.easyTask.util.DateUtils;
import com.github.liuche51.easyTask.util.StringConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * leader选举follow。
 * 使用多线程互斥机制
 */
public class VoteFollows {
    private static final Logger log = LoggerFactory.getLogger(VoteFollows.class);
    private static volatile boolean selecting = false;//选举状态。多线程控制
    private static ReentrantLock lock = new ReentrantLock();//选举互斥锁
    /**
     * 节点启动初始化选举follows。
     *不存在多线程情况，不需要考虑
     * @return
     */
    public static void initSelectFollows() throws Exception {
        int count = EasyTaskConfig.getInstance().getBackupCount();
        List<String> availableFollows = VoteFollows.getAvailableFollows(null);
        List<Node> follows = VoteFollows.selectFollows(count, availableFollows);
        if (follows.size() < count) {
            log.info("follows.size() < count,so start to initSelectFollows");
            initSelectFollows();//数量不够递归重新选
        }else {
            ClusterService.CURRENTNODE.setFollows(follows);
            //通知follows当前Leader位置
            LeaderUtil.notifyFollowsLeaderPosition(follows, EasyTaskConfig.getInstance().getTryCount());
        }
    }
    /**
     * 选择新follow
     * leader同步数据失败或心跳检测失败，则进入选新follow程序
     *
     * @return
     */
    public static Node selectNewFollow(Node oldFollow) throws Exception {
        if (selecting) throw new Exception("cluster is voting,please retry later.");
        selecting = true;
        List<Node> follows = null;
        try {
            lock.lock();
            //多线程下，如果follows已经选好，则让客户端重新提交任务。以后可以优化为获取选举后的follow
            if(ClusterService.CURRENTNODE.getFollows()!=null&&ClusterService.CURRENTNODE.getFollows().size()>=EasyTaskConfig.getInstance().getBackupCount())
                throw new Exception("cluster is voted,please retry again.");
            if (ClusterService.CURRENTNODE.getFollows().contains(oldFollow))
                ClusterService.CURRENTNODE.getFollows().remove(oldFollow);//移除失效的follow
            List<String> availableFollows = getAvailableFollows(Arrays.asList(oldFollow.getAddress()));
            follows = selectFollows(1, availableFollows);
            if (follows.size() < 1)
                selectNewFollow(oldFollow);//数量不够递归重新选
            else
                ClusterService.CURRENTNODE.getFollows().add(follows.get(0));
        } finally {
            selecting=false;//复原选举装填
            lock.unlock();
        }
        if(follows==null||follows.size()==0) throw new Exception("cluster is vote failed,please retry later.");
        //通知follows当前Leader位置
        LeaderUtil.notifyFollowsLeaderPosition(follows, EasyTaskConfig.getInstance().getTryCount());
        return follows.get(0);
    }

    /**
     * 从zk获取可用的follow，并排除自己
     *
     * @return
     */
    private static List<String> getAvailableFollows(List<String> exclude) throws InterruptedException {
        int count = EasyTaskConfig.getInstance().getBackupCount();
        List<String> availableFollows = ZKService.getChildrenByNameSpase();
        //排除自己
        Optional<String> temp = availableFollows.stream().filter(x -> x.equals(EasyTaskConfig.getInstance().getzKServerName())).findFirst();
        if (temp.isPresent())
            availableFollows.remove(temp.get());
        ClusterService.CURRENTNODE.getFollows().forEach(x -> {//排除现有的
            Optional<String> temp1 = availableFollows.stream().filter(y -> y.equals(x.getHost() + ":" + x.getPort())).findFirst();
            if (temp1.isPresent())
                availableFollows.remove(temp1.get());
        });
        if(exclude!=null){
            exclude.forEach(x -> {//排除现有的
                Optional<String> temp1 = availableFollows.stream().filter(y -> y.equals(x)).findFirst();
                if (temp1.isPresent())
                    availableFollows.remove(temp1.get());
            });
        }
        if (availableFollows.size() < count)//如果可选备库节点数量不足，则等待1s，然后重新选。注意：等待会阻塞整个服务可用性
        {
            log.info("availableFollows is not enough! only has " + availableFollows.size());
            Thread.sleep(1000);
            return getAvailableFollows(exclude);
        } else
            return availableFollows;
    }

    /**
     * 从可用follows中选择若干个follow
     *
     * @param count            需要的数量
     * @param availableFollows 可用follows
     */
    private static List<Node> selectFollows(int count, List<String> availableFollows) {
        List<Node> follows = new LinkedList<>();//备选follows
        int size = availableFollows.size();
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            int index = random.nextInt(availableFollows.size());//随机生成的随机数范围就变成[0,size)。
            ZKNode node2 = ZKService.getDataByPath(StringConstant.CHAR_SPRIT + availableFollows.get(index));
            //如果最后心跳时间超过60s，则直接删除该节点信息。
            if (ZonedDateTime.now().minusSeconds(EasyTaskConfig.getInstance().getDeleteZKTimeOunt())
                    .compareTo(DateUtils.parse(node2.getLastHeartbeat())) > 0) {
                ZKService.deleteNodeByPathIgnoreResult(StringConstant.CHAR_SPRIT + availableFollows.get(index));
            } else if (ZonedDateTime.now().minusSeconds(EasyTaskConfig.getInstance().getSelectLeaderZKNodeTimeOunt())
                    .compareTo(DateUtils.parse(node2.getLastHeartbeat())) > 0) {
                //如果最后心跳时间超过30s，也不能将该节点作为follow
            } else if (follows.size() < count) {
                follows.add(new Node(node2.getHost(), node2.getPort()));
                if (follows.size() == count)//已选数量够了就跳出
                    break;
            }
            availableFollows.remove(index);
        }
        return follows;
    }
}
