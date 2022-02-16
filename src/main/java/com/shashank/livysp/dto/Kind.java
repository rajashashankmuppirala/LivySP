package com.shashank.livysp.dto;

public enum Kind {

    SPARK  ("spark"),
    PYSPARK("pyspark"),
    SPARKR("sparkr"),
    SQL("sql");

    private final String type;

    Kind(String type) {
      this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
