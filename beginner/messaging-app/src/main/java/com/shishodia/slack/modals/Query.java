package com.shishodia.slack.modals;

import lombok.Data;

@Data
public class Query {

    private String query;
    private String chartType; // TODO: Should be an enum.
    private String startDate; // TODO: Use SimpleDateFormat
    private String endDate; // TODO: Use SimpleDateFormat

}
