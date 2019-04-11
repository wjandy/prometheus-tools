package com.jfbank.bigdata.prometheus.export;

import io.prometheus.client.hotspot.DefaultExports;

public class JVMExports {

  public JVMExports() {
    DefaultExports.initialize();
  }
}
