package com.sample.transcribestreaming;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.Alternative;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class TranscribeHelper {
    public static AudioInputStream getStreamFromMic() throws LineUnavailableException {

        // Signed PCM AudioFormat with 16kHz, 16 bit sample size, mono
        int sampleRate = 16000;
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Line not supported");
            System.exit(0);
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        return new AudioInputStream(line);
    }

    public static InputStream getStreamFromFile(File audioFile) {
        try {
            return new FileInputStream(audioFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream getStreamFromFile(String audioFile) {
        return getStreamFromFile(new File(audioFile));
    }

    public static TranscribeStreamingAsyncClient getTranscriptionClient(Region region) {
        return TranscribeStreamingAsyncClient.builder()
                .region(region)
                .build();

    }

    public static StartStreamTranscriptionResponseHandler getResponseHandler() {
        StringBuffer finalResult = new StringBuffer();
        return StartStreamTranscriptionResponseHandler.builder()
                .onResponse(r -> {
                    System.out.println("Received Initial response");
                })
                .onError(e -> {
                    System.out.println(e.getMessage());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    System.out.println("Error Occurred: " + sw);
                })
                .onComplete(() -> {
                    System.out.println("=== All records stream successfully ===");
                    System.out.println("Final transcript:\n " + finalResult);
                })
                .subscriber(event -> {
                    List<Result> results = ((TranscriptEvent) event).transcript().results();
                    Stream<Alternative> partialTranscript = results.stream().filter(Result::isPartial)
                            .map(Result::alternatives)
                            .flatMap(Collection::stream);

                    Stream<Alternative> nonPartialTranscript = results.stream().filter(r -> !r.isPartial())
                            .map(Result::alternatives)
                            .flatMap(Collection::stream);

                    partialTranscript.forEach(a -> {
                        if(!a.transcript().isEmpty()){
                            System.out.println("PARTIAL : " + a.transcript());
                        }
                    });
                    nonPartialTranscript.forEach(a -> {
                        if(!a.transcript().isEmpty()){
                            System.out.println("<<COMPLETE>> : " + a.transcript());
                            finalResult.append(" ").append(a.transcript());
                        }
                    });

                    /*if (results.size() > 0) {
                        if (!results.get(0).alternatives().get(0).transcript().isEmpty()) {
                            String transcript=results.get(0).alternatives().get(0).transcript();
                            System.out.println(transcript);
                            finalResult.append(transcript);
                        } else {
                            System.out.println("Empty result");
                        }
                    } else {
                        System.out.println("No results");
                    }*/
                })
                .build();
    }


}
