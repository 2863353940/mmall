package com.mmall.task;

import com.mmall.common.Const;
import com.mmall.common.RedissonManager;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2018/4/13.
 */
@Component
@Slf4j
public class CloseOrderTask {


    @Autowired
    private IOrderService iOrderService;

    @Autowired
    private RedissonManager redissonManager;

    //@Scheduled(cron = "0 */1 * * * ?")  // 每1分钟(每个1分钟的整数倍)
    public void closeOrderTaskV1(){
        log.info("关闭订单定时任务启动");
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour"));
        iOrderService.closeOrder(hour);
        log.info("关闭订单定时任务结束");
    }

    /**
     * redisson分布式锁
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void closeOrderTaskV4(){
        RLock rLock = redissonManager.getRedisson().getLock(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        boolean getLock = false;
        try {
            if(getLock = rLock.tryLock(0, 50, TimeUnit.SECONDS)){
                log.info("Redisson获取到分布式锁:{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
                iOrderService.closeOrder(Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour")));
            }else{
                log.info("Redisson没有获取到分布式锁:{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(!getLock){
                return;
            }
            rLock.unlock();
            log.info("Redisson分布式锁释放锁");
        }

    }









}
