package com.github.knightliao.star.starter.boot.health.helper.controller;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.knightliao.star.starter.boot.health.MyServerStatusService;

/**
 * 一般情况下，
 * 1. 首先，启动脚本 会在启动后 先请求 /online 来设置为在线状态
 * 2. 然后，启动脚本 间隔性地持续性地请求 /detect 直至状态OK。最后 才将容器设置为OK
 * 然后，
 *
 * @author knightliao
 * @email knightliao@gmail.com
 * @date 2021/8/22 22:54
 */
@RestController
@RequestMapping({"/health"})
public class MyGracefulRestController {

    @Resource
    private MyServerStatusService myServerStatusService;

    private AtomicBoolean isOnline = new AtomicBoolean(false);

    @RequestMapping({"/online"})
    public String online() {
        this.isOnline.set(true);
        return "ok";
    }

    @RequestMapping({"offline"})
    public String offline() {
        this.isOnline.set(false);
        return "fail";
    }

    @RequestMapping(value = {"/detect"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public String detect(HttpServletResponse response) {
        boolean success = this.isOnline.get();
        if (success) {
            return "online";
        }
        response.setStatus(503);
        return "offline";
    }

}
