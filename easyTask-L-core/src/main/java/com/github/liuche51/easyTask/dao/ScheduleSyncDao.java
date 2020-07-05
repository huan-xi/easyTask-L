package com.github.liuche51.easyTask.dao;

import com.github.liuche51.easyTask.core.AnnularQueue;
import com.github.liuche51.easyTask.dto.Schedule;
import com.github.liuche51.easyTask.dto.ScheduleSync;
import com.github.liuche51.easyTask.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ScheduleSyncDao {
    public static boolean existTable() throws SQLException, ClassNotFoundException {
        SqliteHelper helper = new SqliteHelper();
        try {
            ResultSet resultSet = helper.executeQuery("SELECT COUNT(*) FROM sqlite_master where type='table' and name='schedule_sync';");
            while (resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0)
                    return true;
            }
        } finally {
            helper.destroyed();
        }
        return false;
    }

    public static void save(ScheduleSync scheduleSync) throws Exception {
        scheduleSync.setCreateTime(DateUtils.getCurrentDateTime());
        scheduleSync.setModifyTime(DateUtils.getCurrentDateTime());
        String sql = "insert into schedule_sync(transaction_id,schedule_id,follow,status,create_time,modify_time) values('"
                + scheduleSync.getTransactionId() + "','" + scheduleSync.getScheduleId() + "'," + "','" + scheduleSync.getFollow() + "'," + scheduleSync.getStatus()
                + ",'" + scheduleSync.getCreateTime() + "','" + scheduleSync.getCreateTime() + "');";
        SqliteHelper.executeUpdateForSync(sql);
    }

    public static List<ScheduleSync> selectByFollowAndStatusWithCount(String follow, short status, int count) throws SQLException, ClassNotFoundException {
        List<ScheduleSync> list = new ArrayList<>(count);
        SqliteHelper helper = new SqliteHelper();
        try {
            ResultSet resultSet = helper.executeQuery("SELECT * FROM schedule_sync where follow='" + follow +
                    "' and status=" + status + " limit " + count + ";");
            while (resultSet.next()) {
                String transactionId = resultSet.getString("transaction_id");
                String scheduleId = resultSet.getString("schedule_id");
                String follow1 = resultSet.getString("follow");
                short status1 = resultSet.getShort("status");
                String createTime = resultSet.getString("create_time");
                String modifyTime = resultSet.getString("modify_time");
                ScheduleSync scheduleSync = new ScheduleSync();
                scheduleSync.setScheduleId(transactionId);
                scheduleSync.setScheduleId(scheduleId);
                scheduleSync.setFollow(follow1);
                scheduleSync.setStatus(status1);
                scheduleSync.setCreateTime(createTime);
                scheduleSync.setModifyTime(modifyTime);
                list.add(scheduleSync);
            }
        } finally {
            helper.destroyed();
        }
        return list;
    }

    public static void updateFollowAndStatusByFollow(String oldFollow, String newFollow, short status) throws SQLException, ClassNotFoundException {
        String sql = "update schedule_sync set follow='" + newFollow + "', status=" + status + ",modify_time='" + DateUtils.getCurrentDateTime() + "' where follow='" + oldFollow + "';";
        SqliteHelper.executeUpdateForSync(sql);
    }

    public static void updateStatusByFollowAndStatus(String follow, short status, short updateStatus) throws SQLException, ClassNotFoundException {
        String sql = "update schedule_sync set status=" + updateStatus + ",modify_time='" + DateUtils.getCurrentDateTime() + "' where follow='" + follow + "' and status=" + status + ";";
        SqliteHelper.executeUpdateForSync(sql);
    }

    public static void updateStatusByFollowAndScheduleIds(String follow, String[] scheduleIds, short updateStatus) throws SQLException, ClassNotFoundException {
        String str = SqliteHelper.getInConditionStr(scheduleIds);
        String sql = "update schedule_sync set status=" + updateStatus + ",modify_time='" + DateUtils.getCurrentDateTime()
                + "' where follow='" + follow + "' and schedule_id in" + str + ";";
        SqliteHelper.executeUpdateForSync(sql);
    }

    public static void updateStatusByScheduleIdAndFollow(String scheduleId, String follow, short status) throws SQLException, ClassNotFoundException {
        String sql = "update schedule_sync set status=" + status + ",modify_time='" + DateUtils.getCurrentDateTime() + "' where schedule_id='" + scheduleId + "' and follow='" + follow + "';";
        SqliteHelper.executeUpdateForSync(sql);
    }
    public static void updateStatusAndTransactionIdByScheduleId(String scheduleId, short status,String transactionId) throws SQLException, ClassNotFoundException {
        String sql = "update schedule_sync set status=" + status + ",transaction_id='" + transactionId + "',modify_time='" + DateUtils.getCurrentDateTime() + "' where schedule_id='" + scheduleId + "';";
        SqliteHelper.executeUpdateForSync(sql);
    }
    public static void updateStatusByTransactionIds(String[] transactionIds,short status) throws SQLException, ClassNotFoundException {
        String instr=SqliteHelper.getInConditionStr(transactionIds);
        String sql = "update schedule_sync set status=" + status + ",modify_time='" + DateUtils.getCurrentDateTime() + "' where transaction_id in " + instr + ";";
        SqliteHelper.executeUpdateForSync(sql);
    }
    public static void deleteByTransactionIdAndFollow(String transactionId, String follow) throws SQLException, ClassNotFoundException {
        String sql = "delete FROM schedule_sync where transaction_id='" + transactionId + "' and follow='" + follow + "';";
        SqliteHelper.executeUpdateForSync(sql);
    }
    public static void deleteByStatus(short status) throws SQLException, ClassNotFoundException {
        String sql = "delete FROM schedule_sync where status = " + status+";";
        SqliteHelper.executeUpdateForSync(sql);
    }
    public static void deleteAll() throws SQLException, ClassNotFoundException {
        String sql = "delete FROM schedule_sync;";
        SqliteHelper.executeUpdateForSync(sql);
    }
}
