// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.sample.transcribestreaming;

import com.amazonaws.transcribestreaming.AudioStreamPublisher;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.*;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// snippet-start:[transcribe.java-streaming-demo]
public class TranscribeStreamingWithSpeakerLabel {
    private static final Region REGION = Region.AP_SOUTH_1;

    private static  String outFile="multi-lang.txt";

    public static void main(String args[])
            throws Exception {

        TranscribeStreamingAsyncClient client = TranscribeStreamingAsyncClient.builder()
                .credentialsProvider(getCredentials())
                .region(REGION)
                .build();
        boolean fromMic=false;
        File file = new File("/Users/Music/Music/Media.localized/Unknown Artist/Unknown Album/OSR_us_000_0010_8k.wav") ;

        int sampleRate=16_000;
        StartStreamTranscriptionRequest request=null;
        AudioInputStream audioInputStream = null;
        if (fromMic) {
            request=getRequest(sampleRate);
            audioInputStream=TranscribeHelper.getStreamFromMic();
        } else {
            System.out.println(file.getName());
            audioInputStream=AudioSystem.getAudioInputStream(file);
            sampleRate= Math.round(audioInputStream.getFormat().getSampleRate());
            request =getRequest(sampleRate);
        }
        CompletableFuture<Void> result = client.startStreamTranscription(request,
                new AudioStreamPublisher(audioInputStream),
                getResponseHandler());

        result.get();
        client.close();
    }

    private static StartStreamTranscriptionRequest getRequest(Integer mediaSampleRateHertz) {
        StartStreamTranscriptionRequest req= StartStreamTranscriptionRequest.builder()
//                .languageOptions("hi-IN")
//                .identifyMultipleLanguages(Boolean.TRUE)
                .showSpeakerLabel(Boolean.TRUE)
//                .enableChannelIdentification(Boolean.TRUE)
//                .numberOfChannels(2)
                .languageCode(LanguageCode.EN_US.toString())
                .mediaEncoding(MediaEncoding.PCM)
                .mediaSampleRateHertz(mediaSampleRateHertz)
                .build();
        System.out.println(req);
        return req;
    }


    private static AwsCredentialsProvider getCredentials() {
        return DefaultCredentialsProvider.create();
    }


    private static StartStreamTranscriptionResponseHandler getResponseHandler() {
        return StartStreamTranscriptionResponseHandler.builder()
                .onResponse(r -> {
                    System.out.println("Received Initial response");
                })
                .onError(e -> {
                    System.out.println(e.getMessage());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    System.out.println("Error Occurred: " + sw.toString());
                })
                .onComplete(() -> {
                    System.out.println("=== All records stream successfully ===");
                })
                .subscriber(event -> {
                    TranscriptEvent transcriptEvent=(TranscriptEvent) event;
                    List<Result> results = transcriptEvent.transcript().results();
                    printMultiSpeakerResult(results);
                })
                .build();
    }

    private static void printMultiSpeakerResult(List<Result> results) {
        if(results.size()>0) {
            results.forEach(result -> {
                try {
                    printSingleResult( result);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void printSingleResult( Result firstResult) throws FileNotFoundException{
        PrintStream printStream = new PrintStream(new File(outFile));
        final StringBuffer finalTranscript=new StringBuffer();
        if (firstResult.alternatives().size() > 0 && !firstResult.alternatives().get(0).transcript().isEmpty()) {
            /*String transcript = firstResult.alternatives().get(0).transcript();*/
            /* PR: getting the speaker info from the transcribe resuls */
            String transcript = firstResult.alternatives().get(0).transcript();
            //String transcript = "Speaker " + firstResult.alternatives().get(0).items().get(0).speaker() + ": " + firstResult.alternatives().get(0).items().get(0).content();
            if(!transcript.isEmpty()) {

                if (!firstResult.isPartial()) {
                    String speaker = "";
                    String pretranscript = "";
                    String posttranscript = "";
                    int i = 0;
                    //System.out.println("ITEMS ARE:   " + firstResult.alternatives().get(0).items());
                    while (i < firstResult.alternatives().get(0).items().size()) {
                        if (i > 0) {
                            speaker = " Speaker " + firstResult.alternatives().get(0).items().get(i-1).speaker() + ": ";
                        }
                        i++;
                    }
                    pretranscript = firstResult.alternatives().get(0).transcript();
                    posttranscript = speaker + pretranscript + "\n";
                    finalTranscript.append(posttranscript);
                    posttranscript = "";
                }
                if (!finalTranscript.toString().trim().isEmpty()) {
                    System.out.println(finalTranscript);
                    printStream.println(finalTranscript);
                }
            }
        }
    }

}
// snippet-end:[transcribe.java-streaming-demo]
