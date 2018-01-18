package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oath.gemini.merchant.ews.EWSConstant.ReportingJobStatusEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @author tong on 1/1/2018
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EWSReportingJobData {
    private String jobId;

    // submitted|running|failed|completed|killed
    private ReportingJobStatusEnum status;

    // When the job is completed, you will receive a URL you will use to download the report data
    private String jobResponse;
}
