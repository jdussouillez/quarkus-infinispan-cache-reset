package com.github.jdussouillez;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.infinispan.protostream.annotations.ProtoField;

public class MathInfo {

    @JsonProperty
    @ProtoField(number = 1, required = true)
    protected boolean even;

    @JsonProperty
    @ProtoField(number = 2, required = true)
    protected double squareRoot;

    @JsonProperty
    @ProtoField(number = 3, required = true)
    protected int nbDigits;

    public MathInfo(boolean even, double squareRoot, int nbDigits) {
        this.even = even;
        this.squareRoot = squareRoot;
        this.nbDigits = nbDigits;
    }

    public MathInfo() {
    }
}
