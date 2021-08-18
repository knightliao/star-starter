package com.github.knightliao.star.starter.boot.alimetrics.endpoint;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.alibaba.metrics.FastCompass;
import com.alibaba.metrics.IMetricManager;
import com.alibaba.metrics.MetricFilter;
import com.alibaba.metrics.MetricManager;
import com.alibaba.metrics.MetricName;
import com.alibaba.metrics.MetricRegistry;
import com.alibaba.metrics.common.CollectLevel;
import com.alibaba.metrics.common.MetricObject;
import com.alibaba.metrics.common.MetricsCollector;
import com.alibaba.metrics.common.MetricsCollectorFactory;

/**
 * @author knightliao
 * @email knightliao@gmail.com
 * @date 2021/8/18 10:52
 */
@RestControllerEndpoint(id = "alimetrics")
public class AliMMetricsRestEndpoint {

    private final IMetricManager manager = MetricManager.getIMetricManager();

    private static final double rateFactor = TimeUnit.SECONDS.toSeconds(1);
    private static final double durationFactor = 1.0 / TimeUnit.MILLISECONDS.toNanos(1);

    @GetMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> index() {

        return getMetricResult(s -> true);
    }

    @GetMapping(path = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> list() {

        if (manager.isEnabled()) {

            Map<String, Set<MetricObject>> metrics = new LinkedHashMap<>();

            for (String groupName : manager.listMetricGroups()) {

                MetricRegistry registry = manager.getMetricRegistryByGroup(groupName);
                Set<MetricObject> metricPerRegistry = new LinkedHashSet<>();
                metricPerRegistry.addAll(buildMetricRegistry(registry, null));
                metrics.put(groupName, metricPerRegistry);
            }

            try {
                return buildResult(metrics, true, "");
            } catch (Exception ex) {
                return buildResult(null, false, toString());
            }
        } else {

            return buildResult(null, false, "disable");
        }
    }

    @GetMapping(path = "/{group}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> group(@PathVariable String group) {

        return getMetricResult(s -> s.equals(group));
    }

    private List<MetricObject> buildMetricRegistry(MetricRegistry registry, MetricFilter filter) {

        long ts = System.currentTimeMillis();

        MetricsCollector collector = MetricsCollectorFactory.createNew(CollectLevel.NORMAL, rateFactor,
                durationFactor, filter);

        //
        SortedMap<MetricName, FastCompass> fastCompass = filter == null ? registry.getFastCompasses() :
                registry.getFastCompasses(filter);
        for (Map.Entry<MetricName, FastCompass> entry : fastCompass.entrySet()) {
            collector.collect(entry.getKey(), entry.getValue(), ts);
        }

        return collector.build();
    }

    private Map<String, Object> getMetricResult(Predicate<String> stringPredicate) {

        try {

            Map<String, List<MetricObject>> metrics = getMetrics(stringPredicate);
            if (metrics.isEmpty()) {
                return buildResult("", false, "group not found");
            }

            return buildResult(metrics, true, "");

        } catch (Throwable e) {
            return buildResult("", false, e.toString());
        }
    }

    private Map<String, List<MetricObject>> getMetrics(Predicate<String> groupFilter) {

        final Map<String, List<MetricObject>> result = new TreeMap<>();

        manager.listMetricGroups().stream().filter(groupFilter).forEach(group -> {

            MetricRegistry registry = manager.getMetricRegistryByGroup(group);
            result.put(group, buildMetricRegistry(registry, null));
        });

        return result;
    }

    private Map<String, Object> buildResult(Object data, boolean success, String message) {

        Map<String, Object> result = new HashMap<>();
        if (data != null) {
            result.put("data", data);
        }

        result.put("message", message);
        result.put("success", success);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
