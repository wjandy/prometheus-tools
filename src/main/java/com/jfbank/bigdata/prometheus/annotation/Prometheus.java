package com.jfbank.bigdata.prometheus.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.jfbank.bigdata.prometheus.aspect.MonitorType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Order(Ordered.HIGHEST_PRECEDENCE)
public @interface Prometheus {

  /**
   * 命名空间(optional)，建议是按层来划分，如dubbo/mgw/wgw，未重构之前的可以是webxm/webcd/module
   * 
   * 如果该值未配置，会取promethues.properties中的prometheus.namespace，取不到为空，不强制要求
   */
  String namespace() default "";

  /**
   * 命名空间下的子系统名称(optional)，建议是业务的名称，比如approve/memo/customer/mail等
   * 
   * 如果该值未配置，会取promethues.properties中的prometheus.subsystem，取不到为空，不强制要求
   */
  String subSystem() default "";

  /**
   * 监控项的名称(optional).如果不填写，则以调用的方法名称作为name
   */
  String name() default "";

  /**
   * 监控项的配置，不配置默认全部。详见com.jfbank.bigdata.prometheus.aspect.MonitorType
   */
  MonitorType[] types() default {};

  /**
   * bucket的设置，供Histogram使用
   */
  //double[] buckets() default {0.1, 0.4, 0.8, 1.1, 1.5};
}
