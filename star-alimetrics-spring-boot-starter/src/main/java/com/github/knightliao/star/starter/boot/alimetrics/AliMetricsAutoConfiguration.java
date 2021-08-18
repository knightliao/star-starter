package com.github.knightliao.star.starter.boot.alimetrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.knightliao.star.starter.boot.alimetrics.endpoint.AliMMetricsRestEndpoint;

/**
 * @author knightliao
 * @email knightliao@gmail.com
 * @date 2021/8/18 10:48
 */
@Configuration
public class AliMetricsAutoConfiguration {

    @Bean
    public AliMMetricsRestEndpoint aliMMetricsRestEndpoint() {

        return new AliMMetricsRestEndpoint();
    }

}
