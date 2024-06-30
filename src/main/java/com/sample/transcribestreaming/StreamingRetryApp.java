// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.sample.transcribestreaming;

import com.amazonaws.transcribestreaming.AudioStreamPublisher;
import com.amazonaws.transcribestreaming.TranscribeStreamingRetryClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;

import javax.sound.sampled.LineUnavailableException;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

// snippet-start:[transcribe.java-streaming-retry-app]
public class StreamingRetryApp {
    private static final Region region = Region.AP_SOUTH_1;
    private static final int sample_rate = 28800;
    private static final String FILE = "/Users/test-data/samples_jfk.wav";

    public static void main(String args[]) throws URISyntaxException, ExecutionException, InterruptedException,
            LineUnavailableException, FileNotFoundException {

        TranscribeStreamingRetryClient client =  new TranscribeStreamingRetryClient(
                TranscribeHelper.getTranscriptionClient(region));

        StartStreamTranscriptionRequest request = StartStreamTranscriptionRequest.builder()
                .languageCode(LanguageCode.EN_US.toString())
                .mediaEncoding(MediaEncoding.PCM)
                .mediaSampleRateHertz(sample_rate)
                .build();
        /**
         * Start real-time speech recognition. The Amazon Transcribe streaming java
         * client uses the Reactive-streams
         * interface. For reference on Reactive-streams:
         * https://github.com/reactive-streams/reactive-streams-jvm
         */
        CompletableFuture<Void> result = client.startStreamTranscription(
                /**
                 * Request parameters. Refer to API documentation for details.
                 */
                request,
                /**
                 * Provide an input audio stream.
                 * For input from a microphone, use getStreamFromMic().
                 * For input from a file, use getStreamFromFile().
                 */
                new AudioStreamPublisher(TranscribeHelper.getStreamFromFile(FILE)),
                /**
                 * Object that defines the behavior on how to handle the stream
                 */
                new StreamTranscriptionBehaviorImpl());

        /**
         * Synchronous wait for stream to close, and close client connection
         */
        result.get();
        client.close();
    }


}
// snippet-end:[transcribe.java-streaming-retry-app]