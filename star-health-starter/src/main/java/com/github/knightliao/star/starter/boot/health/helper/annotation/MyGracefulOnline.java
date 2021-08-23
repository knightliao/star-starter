package com.github.knightliao.star.starter.boot.health.helper.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.knightliao.star.starter.boot.health.helper.constants.MyHealthConstants;

/**
 * @author knightliao
 * @email knightliao@gmail.com
 * @date 2021/8/22 22:50
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyGracefulOnline {

    int shutdownWaitSecond() default MyHealthConstants.SHUTDOWN_WAIT_TIME_SEC;

    // 端口号
    int port() default MyHealthConstants.DEFAULT_PORT;
}
