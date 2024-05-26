package com.dowglasmaia.exactbank.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;


public class DateConverter {

    public static OffsetDateTime convertStringToOffsetDateTime(String dateString){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate localDate = LocalDate.parse(dateString, formatter);
        LocalDateTime endOfDay = localDate.atTime(LocalTime.MAX);
        return endOfDay.atOffset(ZoneOffset.UTC);
    }

}
