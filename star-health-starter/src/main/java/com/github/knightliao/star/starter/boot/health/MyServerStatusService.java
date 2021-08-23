package com.github.knightliao.star.starter.boot.health;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.github.knightliao.middle.http.sync.utils.MyHttpUtils;
import com.github.knightliao.star.starter.boot.health.helper.annotation.MyGracefulOnline;
import com.github.knightliao.star.starter.boot.health.helper.constants.MyHealthConstants;

/**
 * @author knightliao
 * @email knightliao@gmail.com
 * @date 2021/8/22 22:57
 */
@Service
public class MyServerStatusService implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MyServerStatusService.class);
    private AtomicBoolean serverStatus = new AtomicBoolean(false);
    private ApplicationContext applicationContext = null;
    private Thread hookThread = new Thread(this::shutdown);
    private boolean isStartup = false;

    @Override
    public void destroy() throws Exception {

        log.info("destroy spring...");
        this.shutdown();
    }

    private void shutdown() {

        this.serverStatus.set(false);

        if (this.applicationContext != null) {
            int waitTime = this.getShutdownWaitTime(this.applicationContext);
            log.info("spring shutdown done! waitTime={}s", waitTime);

            try {
                Thread.sleep(waitTime * 1000L);
            } catch (InterruptedException ex) {

            }

            boolean isActive = ((AbstractApplicationContext) this.applicationContext).isActive();
            if (!isActive) {
                log.info("spring already shutdown");
            } else {
                ((AbstractApplicationContext) this.applicationContext).close();
            }
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        ApplicationContext applicationContext = event.getApplicationContext();
        log.info("onApplicationEvent start");

        if (!this.isStartup) {
            this.isStartup = true;
            this.applicationContext = applicationContext;
            Runtime.getRuntime().addShutdownHook(this.hookThread);
            log.info("spring container start successful");

            //
            this.prehot();

            //
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        MyServerStatusService.this.startup();
                    } catch (Exception ex) {
                        MyServerStatusService.log.error("set startup fail!" + ex.toString(), ex);
                    }
                }
            })).start();
        }
    }

    private void startup() {
        this.serverStatus.set(true);
        log.info("server status set {}", this.serverStatus.get());
    }

    // 只是预热，失败也无法所谓
    private void prehot() {

        try {

            String content = MyHttpUtils.get("http://localhost:" + getPort(applicationContext) + "/health/detect",
                    1500, 1500);
            log.info("{}", content);

        } catch (Exception ex) {
            log.warn(ex.toString());
        }
    }

    private int getShutdownWaitTime(ApplicationContext context) {

        MyGracefulOnline myGracefulOnline = getMyGracefulOnline(context);
        if (myGracefulOnline == null) {
            return MyHealthConstants.SHUTDOWN_WAIT_TIME_SEC;
        }

        return myGracefulOnline.shutdownWaitSecond();
    }

    private int getPort(ApplicationContext context) {

        MyGracefulOnline myGracefulOnline = getMyGracefulOnline(context);
        if (myGracefulOnline == null) {
            return MyHealthConstants.DEFAULT_PORT;
        }

        return myGracefulOnline.port();
    }

    /**
     * https://stackoverflow.com/questions/53968592/spring-applicationcontext-getbeanswithannotation-method-returns
     * -an-empty-list
     * 由于可能出现父子context的问题，因此要寻找parent context
     *
     * @param context
     * @return
     */
    private MyGracefulOnline getMyGracefulOnline(ApplicationContext context) {

        try {

            Map<String, Object> map = context.getBeansWithAnnotation(MyGracefulOnline.class);
            if (CollectionUtils.isEmpty(map) && context.getParent() != null) {
                map = context.getParent().getBeansWithAnnotation(MyGracefulOnline.class);
            }

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object obj = applicationContext.getBean(entry.getKey());

                return AnnotationUtils.findAnnotation(obj.getClass(), MyGracefulOnline.class);
            }

        } catch (Exception ex) {
            log.error(ex.toString(), ex);
        }

        return null;
    }
}
