package com.jfbank.bigdata.prometheus.aspect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.jfbank.bigdata.prometheus.annotation.Prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Histogram.Timer;
import io.prometheus.client.SimpleCollector.Builder;

@Aspect
@Component
public class PrometheusAspect {

  @Value("${prometheus.namespace}")
  private String defaultNameSpace;
  @Value("${prometheus.subsystem}")
  private String defaultSubSystem;

  // 调用时间的collector，key为prometheus的namespace_subsystem_name组成
  private Map<String, Collector> latencyMap = new ConcurrentHashMap<String, Collector>();

  // 存放并发数的collector，key为prometheus的namespace_subsystem_name组成
  private Map<String, Collector> concurrentCountMap = new ConcurrentHashMap<String, Collector>();

  // 存放失败数的collector，key为prometheus的namespace_subsystem_name组成
  private Map<String, Collector> failCounterMap = new ConcurrentHashMap<String, Collector>();

  @Around("@annotation(prometheus)")
  public Object process(final ProceedingJoinPoint joinPoint, Prometheus prometheus)
      throws Throwable {
    String key = getKey(prometheus, joinPoint);
    Histogram latencyHis = isMonitor(prometheus, MonitorType.LATENCY_HIS)
        ? getHistogram(key, prometheus, joinPoint) : null;
    Gauge concurrentCounter = isMonitor(prometheus, MonitorType.CONCURRENT_GAUGE)
        ? initConcurrentGauge(key, prometheus, joinPoint) : null;
    Counter failCounter = isMonitor(prometheus, MonitorType.FAIL_COUNTER)
        ? getFailCounter(key, prometheus, joinPoint) : null;

    Timer latencyTimer = null;
    if (latencyHis != null) {
      latencyTimer = latencyHis.startTimer();
    }
    if (concurrentCounter != null) {
      concurrentCounter.inc();
    }
    try {
      return joinPoint.proceed();
    } catch (Throwable e) {
      if (failCounter != null) {
        failCounter.inc();
      }
      throw e;
    } finally {
      if (latencyTimer != null) {
        latencyTimer.observeDuration();
      }
      if (concurrentCounter != null) {
        concurrentCounter.dec();
      }
    }
  }

  private boolean isMonitor(Prometheus prometheus, MonitorType type) {
    if (prometheus.types() == null || prometheus.types().length == 0) {
      return true;
    }

    for (MonitorType mt : prometheus.types()) {
      if (mt == type) {
        return true;
      }
    }

    return false;
  }

  private Histogram getHistogram(String key, Prometheus prometheus, ProceedingJoinPoint joinPoint) {
    if (!latencyMap.containsKey(key)) {
      checkAndInit(key, prometheus, joinPoint, latencyMap, MonitorType.LATENCY_HIS);
    }

    if (latencyMap.containsKey(key)) {
      return (Histogram) latencyMap.get(key);
    } else {
      return null;
    }
  }

  private Gauge initConcurrentGauge(String key, Prometheus prometheus,
      ProceedingJoinPoint joinPoint) {
    if (!concurrentCountMap.containsKey(key)) {
      checkAndInit(key, prometheus, joinPoint, concurrentCountMap, MonitorType.CONCURRENT_GAUGE);
    }

    if (concurrentCountMap.containsKey(key)) {
      return (Gauge) concurrentCountMap.get(key);
    } else {
      return null;
    }
  }

  private Counter getFailCounter(String key, Prometheus prometheus, ProceedingJoinPoint joinPoint) {
    if (!failCounterMap.containsKey(key)) {
      checkAndInit(key, prometheus, joinPoint, failCounterMap, MonitorType.FAIL_COUNTER);
    }

    if (failCounterMap.containsKey(key)) {
      return (Counter) failCounterMap.get(key);
    } else {
      return null;
    }
  }

  @SuppressWarnings("rawtypes")
  private synchronized void checkAndInit(String key, Prometheus prometheus,
      ProceedingJoinPoint joinPoint, Map<String, Collector> collectorMap, MonitorType type) {
    if (!collectorMap.containsKey(key)) {
      Builder builder = null;
      // 创建对应的builder
      if (type == MonitorType.LATENCY_HIS) {
        builder = Histogram.build();

      } else if (type == MonitorType.CONCURRENT_GAUGE) {
        builder = Gauge.build().name(getName(prometheus, joinPoint) + "_concurrency_number");

      } else if (type == MonitorType.FAIL_COUNTER) {
        builder = Counter.build();
      }

      if (builder != null) {
        // 根据builder设置namespace、subsystem等并register
        String name = getName(prometheus, joinPoint);
        Collector collector =
            builder.namespace(getNameSpace(prometheus)).subsystem(getSubSystem(prometheus))
                .name(name + type.getNameSuffix()).help(type.getHelpPrefix() + name).register();
        collectorMap.put(key, collector);
      }
    }
  }

  private String getKey(Prometheus prometheus, ProceedingJoinPoint joinPoint) {
    String namespace = getNameSpace(prometheus);
    String subSystem = getSubSystem(prometheus);
    String name = getName(prometheus, joinPoint);

    String key = namespace + "_" + subSystem + "_" + name;

    return key;
  }

  private String getNameSpace(Prometheus prometheus) {
    String namespace = prometheus.namespace();
    if (isBlank(namespace)) {
      namespace = defaultNameSpace;
    }

    return namespace;
  }

  private String getSubSystem(Prometheus prometheus) {
    String subSystem = prometheus.subSystem();
    if (isBlank(subSystem)) {
      subSystem = defaultSubSystem;
    }

    return subSystem;
  }

  private String getName(Prometheus prometheus, ProceedingJoinPoint joinPoint) {
    String name = prometheus.name();
    if (isBlank(name)) {
      name = getMethodName(joinPoint);
    }

    return name;
  }

  private String getMethodName(ProceedingJoinPoint joinPoint) {
    Signature sign = joinPoint.getSignature();
    MethodSignature methodSign = (MethodSignature) sign;
    Method m = methodSign.getMethod();

    return m.getName();
  }

  private boolean isBlank(String s) {
    return s == null || "".equals(s);
  }
}
