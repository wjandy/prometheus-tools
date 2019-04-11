package com.jfbank.bigdata.prometheus.aspect;

public enum MonitorType {

  CONCURRENT_GAUGE("_concurrency_number", "The concurrency number of "), FAIL_COUNTER("_count",
      "The  count of "), LATENCY_HIS("_latency", "The latency of request for ");

  private String nameSuffix;
  private String helpPrefix;

  private MonitorType(String nameSuffix, String helpPrefix) {
    this.nameSuffix = nameSuffix;
    this.helpPrefix = helpPrefix;
  }

  public String getNameSuffix() {
    return nameSuffix;
  }

  public String getHelpPrefix() {
    return helpPrefix;
  }
}
