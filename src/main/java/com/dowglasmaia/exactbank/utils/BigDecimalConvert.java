package com.dowglasmaia.exactbank.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalConvert {

    public static BigDecimal toBigDecimal(Double amout){
        return BigDecimal.valueOf(amout)
              .setScale(2, RoundingMode.HALF_UP);
    }
}
