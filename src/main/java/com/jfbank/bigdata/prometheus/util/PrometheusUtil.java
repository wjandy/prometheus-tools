package com.jfbank.bigdata.prometheus.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jfbank.bigdata.prometheus.aspect.MonitorType;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector.Builder;

public class PrometheusUtil {

	// 调用时间的collector，key为prometheus的namespace_subsystem_name组成
	private static Map<String, Collector> latencyMap = new ConcurrentHashMap<String, Collector>();

	// 存放并发数的collector，key为prometheus的namespace_subsystem_name组成
	private static Map<String, Collector> concurrentCountMap = new ConcurrentHashMap<String, Collector>();

	// 存放失败数的collector，key为prometheus的namespace_subsystem_name组成
	private static Map<String, Collector> counterMap = new ConcurrentHashMap<String, Collector>();

	private static String defaultNameSpace="space";
	private static String defaultSubSystem="sub";
	
	//private static final double[] buckets = { 0.05,0.1,0.3, 0.5,0.65, 0.8, 1.1, 1.5 };

	public static void init(String defaultNameSpace, String defaultSubSystem) {
		PrometheusUtil.defaultNameSpace = defaultNameSpace;
		PrometheusUtil.defaultSubSystem = defaultSubSystem;
	}

	public static void guagaStatisByLabel(String name, String label, double param) {
		Gauge concurrentCounter = getConcurrentGauge(name, label);
		concurrentCounter.labels(label).set(param);
	}
	
	public static void guagaStatisByLabel(String name, String label,String labelVal, double param) {
		Gauge concurrentCounter = getConcurrentGauge(name, label);
		concurrentCounter.labels(labelVal).set(param);
	}
	
	public static void guagaStatisByLabelMuti(String name, String[] label, double param,String... labelVal) {
		Gauge concurrentCounter = getConcurrentGauge(name, label);
		concurrentCounter.labels(labelVal).set(param);
	}

	public static void counterStatisByLabel(String name, String label,String labelVal) {
		Counter counter = getCounter(name, label);
		counter.labels(labelVal).inc();
	}

	public static void counterStatisByLabel(String name, String label,String labelVal, double param) {
		Counter counter = getCounter(name, label);
		counter.labels(labelVal).inc(param);
	}

	private synchronized static Collector checkAndInit(String name,  Map<String, Collector> collectorMap,
			MonitorType type,String... label) {
		String key = getKey(name);
		if (!collectorMap.containsKey(key)) {
			Builder builder = null;
			// 创建对应的builder
			if (type == MonitorType.LATENCY_HIS) {
				builder = Histogram.build();

			} else if (type == MonitorType.CONCURRENT_GAUGE) {
				builder = Gauge.build().name(name + "_concurrency_number");

			} else if (type == MonitorType.FAIL_COUNTER) {
				builder = Counter.build();
			}

			if (builder != null) {
				Collector collector = null;
				// 根据builder设置namespace、subsystem等并register
				if (label == null) {
					collector = builder.namespace(defaultNameSpace).subsystem(defaultSubSystem)
							.name(name + type.getNameSuffix()).help(type.getHelpPrefix() + name).register();
				} else {
					collector = builder.namespace(defaultNameSpace).subsystem(defaultSubSystem)
							.name(name + type.getNameSuffix()).labelNames(label).help(type.getHelpPrefix() + name)
							.register();
				}
				collectorMap.put(key, collector);
				return collector;
			}
		}
		return collectorMap.get(key);
	}

	private static Gauge getConcurrentGauge(String name, String... label) {
		String key = getKey(name);
		Gauge guage = (Gauge) concurrentCountMap.get(key);
		if (guage == null) {
			guage = (Gauge) checkAndInit(name, concurrentCountMap, MonitorType.CONCURRENT_GAUGE, label);
		}
		return guage;
	}

	public static Histogram getHistogram(String name, String... label) {
		String key = getKey(name);
		Histogram guage = (Histogram) latencyMap.get(key);
		if (guage == null) {
			guage = (Histogram) checkAndInit(name,  latencyMap, MonitorType.LATENCY_HIS, label);
		}
		return guage;
	}

	private static Counter getCounter(String name, String... label) {
		String key = getKey(name);
		Counter guage = (Counter) counterMap.get(key);
		if (guage == null) {
			guage = (Counter) checkAndInit(name,  counterMap, MonitorType.FAIL_COUNTER, label);
		}
		return guage;
	}

	private static String getKey(String name) {
		return defaultNameSpace + "_" + defaultSubSystem + "_" + name;
	}
	
	public static void main(String[] args) {
		init("gg", "fff");
		PrometheusUtil.getHistogram("dddd", "aa").labels("aa").observe(0.6);
		//counterStatisByLabel("lxb", "ggg");
	}
}
