// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.sample.transcribe;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.*;

import java.util.Arrays;

// snippet-start:[transcribe.java-list-jobs]
public class TranscriptionBatchJobs {
    static String bucketName="workshop-ACCOUNT_ID";
    static String mediaUri="s3://workshop-ACCOUNT_ID/media/demo-call.mp3";
    static String transcriptionPrefix="my-transcript/";

    static String roleArn="arn:aws:iam::ACCOUNT_ID:role/transcribe-role-name";

    static String languageCodes="en-US,hi-IN";
    static String jobNamePrefix="test-job-";

    static  Region region= Region.AP_SOUTH_1;
    public static void main(String[] args) {
        TranscribeClient transcribeClient = TranscribeClient.builder()
                .region(region)
                .build();
        StartTranscriptionJobResponse response = submitJob(transcribeClient);
        listTranscriptionJobs(transcribeClient, response.transcriptionJob().transcriptionJobName());
        trackCompletion(transcribeClient,response.transcriptionJob().transcriptionJobName());
    }

    private static StartTranscriptionJobResponse submitJob(TranscribeClient transcribeClient) {

        return transcribeClient.startTranscriptionJob(builder -> {
            builder
                    .transcriptionJobName(jobNamePrefix + System.currentTimeMillis())
                    .identifyLanguage(true)
                    .identifyMultipleLanguages(true)
                    .languageOptionsWithStrings(languageCodes.split(","))
                    .media(Media.builder().mediaFileUri(mediaUri).build())
//                    .mediaFormat(MediaFormat.MP3)
//                    .jobExecutionSettings(JobExecutionSettings.builder().dataAccessRoleArn(roleArn).build())
                    .outputBucketName(bucketName)
                    .outputKey(transcriptionPrefix)
                    .settings(getChannelAndSpeakerSetting())
                    //Multiple language identification isn’t available when you use content redaction with a job.
//                    .contentRedaction(getContentRedactionSettings())
                    .build();
        });

    }

    private static Settings getChannelAndSpeakerSetting(){
        return Settings.builder().channelIdentification(true).showSpeakerLabels(true).maxSpeakerLabels(3).build();
    }

    /**
     * Multiple language identification isn’t available when you use content redaction with a job.
     * Start a different type of job to use multiple language identification
     * @return
     */
    private static ContentRedaction getContentRedactionSettings(){
        return ContentRedaction.builder()
                .piiEntityTypes(
                        PiiEntityType.CREDIT_DEBIT_CVV,
                        PiiEntityType.SSN
                ).redactionOutput(RedactionOutput.REDACTED)
                .redactionType(RedactionType.PII)
                .build();
    }

    private static void trackCompletion(TranscribeClient transcribeClient, String jobName) {
        GetTranscriptionJobResponse jobDetails = transcribeClient.getTranscriptionJob(GetTranscriptionJobRequest.builder().transcriptionJobName(jobName).build());

        while (!Arrays.asList("COMPLETED", "FAILED").contains(jobDetails.transcriptionJob().transcriptionJobStatus().toString())) {
            System.out.println("Status:"+jobDetails.transcriptionJob().transcriptionJobStatus());
            sleep();
            jobDetails = transcribeClient.getTranscriptionJob(GetTranscriptionJobRequest.builder().transcriptionJobName(jobName).build());
        }

        System.out.println("Language Code: " + jobDetails.transcriptionJob().languageCode());
        System.out.println("Media Format: " + jobDetails.transcriptionJob().mediaFormat());
        System.out.println("Transcript Format: " + jobDetails.transcriptionJob().transcript().toString());

    }

    public static void listTranscriptionJobs(TranscribeClient transcribeClient, String jobName) {
        ListTranscriptionJobsRequest listJobsRequest = ListTranscriptionJobsRequest.builder()
                .jobNameContains(jobName).build();

        transcribeClient.listTranscriptionJobsPaginator(listJobsRequest).stream()
                .flatMap(response -> response.transcriptionJobSummaries().stream())
                .filter(jobSummary -> jobName.equalsIgnoreCase(jobSummary.transcriptionJobName()))
                .forEach(jobSummary -> {
                    System.out.println("Job Name: " + jobSummary.transcriptionJobName());
                    System.out.println("Job Status: " + jobSummary.transcriptionJobStatus());
                    System.out.println("Output Location: " + jobSummary.outputLocationType());
                    // Add more information as needed

                    // Retrieve additional details for the job if necessary
                    GetTranscriptionJobResponse jobDetails = transcribeClient.getTranscriptionJob(
                            GetTranscriptionJobRequest.builder()
                                    .transcriptionJobName(jobSummary.transcriptionJobName())
                                    .build());

                    // Display additional details
                    System.out.println("Language Code: " + jobDetails.transcriptionJob().languageCode());
                    System.out.println("Media Format: " + jobDetails.transcriptionJob().mediaFormat());
                    // Add more details as needed

                    System.out.println("--------------");
                });
    }

    private static void sleep() {
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
// snippet-end:[transcribe.java-list-jobs]
