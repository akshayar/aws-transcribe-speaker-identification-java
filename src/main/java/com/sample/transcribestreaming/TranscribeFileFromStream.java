package com.sample.transcribestreaming;

import com.amazonaws.transcribestreaming.AudioStreamPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED;


public class TranscribeFileFromStream {
    private static final Region REGION = Region.AP_SOUTH_1;

    public static void main(String[] args) throws Exception {
        System.out.println(System.getProperty("java.version"));
        // BasicConfigurator.configure();
        TranscribeStreamingAsyncClient client = TranscribeHelper.getTranscriptionClient(REGION);
        try {
            File inputFile = new File("/Users/test-data/samples_jfk.wav");
            CompletableFuture<Void> result = client.startStreamTranscription(
                    getRequest(inputFile),
                    new AudioStreamPublisher(TranscribeHelper.getStreamFromFile(inputFile)),
                    TranscribeHelper.getResponseHandler());
            result.get();
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }


    private static StartStreamTranscriptionRequest getRequest(File inputFile) throws IOException, UnsupportedAudioFileException {
        //TODO: I read the file twice in this example.  Can this be more performant?
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputFile);
        AudioFormat audioFormat = audioInputStream.getFormat();
        return StartStreamTranscriptionRequest.builder()
                .languageCode(LanguageCode.EN_US)
                .mediaEncoding(MediaEncoding.PCM)
                .mediaEncoding(getAwsMediaEncoding(audioFormat))
                .mediaSampleRateHertz(getAwsSampleRate(audioFormat))
                .build();
    }

    private static MediaEncoding getAwsMediaEncoding(AudioFormat audioFormat) {
        final String javaMediaEncoding = audioFormat.getEncoding().toString();

        if (PCM_SIGNED.toString().equals(javaMediaEncoding)) {
            return MediaEncoding.PCM;
        } else if (PCM_UNSIGNED.toString().equals(javaMediaEncoding)) {
            return MediaEncoding.PCM;
        } /*else if (ALAW.toString().equals(javaMediaEncoding)){
            //WARNING: I have no idea how ALAW maps to AWS media encodings.
            return MediaEncoding.OGG_OPUS;
        } else if (ULAW.toString().equals(javaMediaEncoding)){
            //WARNING: I have no idea how ULAW maps to AWS encodings.
            return MediaEncoding.FLAC;
        }*/

        throw new IllegalArgumentException("Not a recognized media encoding:" + javaMediaEncoding);
    }

    private static Integer getAwsSampleRate(AudioFormat audioFormat) {
        return Math.round(audioFormat.getSampleRate());
    }

}