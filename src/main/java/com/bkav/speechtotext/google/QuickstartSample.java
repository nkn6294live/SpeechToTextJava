package com.bkav.speechtotext.google;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.bkav.speechtotext.audio.AudioRecordDemo;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeResponse;
//Imports the Google Cloud client library
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionResult;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

public class QuickstartSample {

	static class ResponseApiStreamingObserver<T> implements ApiStreamObserver<T> {
		private final SettableFuture<List<T>> future = SettableFuture.create();
		private final List<T> messages = new java.util.ArrayList<T>();

		@Override
		public void onNext(T message) {
			messages.add(message);
		}

		@Override
		public void onError(Throwable t) {
			future.setException(t);
		}

		@Override
		public void onCompleted() {
			future.set(messages);
		}

		// Returns the SettableFuture object to get received messages / exceptions.
		public SettableFuture<List<T>> future() {
			return future;
		}
	}
	
	public static void main(String... strings) throws Exception {
		System.getenv().entrySet().stream()
//		.filter(entry -> env_name.equals(entry.getKey()))
		.forEach(entry -> {
			System.out.println(String.format("[%s]:[%s]", entry.getKey(), entry.getValue()));
		});
		syncRecognizeFile2();
//		streamingRecognizeFile("/home/namnk/workspace/nodejs/2/resources/test.wav");
//		streamingRecognizeFile("/home/namnk/Desktop/record.wav");
//		streamRecognizeMic();
	}

	public static void streamRecognizeMic() throws Exception {
		String fileName = "/home/namnk/workspace/nodejs/2/resources/test.wav";
		fileName = "/home/namnk/Desktop/record.wav";
		final String tempFile = "/home/namnk/Desktop/temp.wav";
		Path path = Paths.get(fileName);
		byte[] data = Files.readAllBytes(path);

		// Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
		try (SpeechClient speech = SpeechClient.create()) {
			// Configure request with local raw PCM audio
		    RecognitionConfig recConfig = RecognitionConfig.newBuilder()
		        .setEncoding(AudioEncoding.LINEAR16)
//		        .setLanguageCode("en-US")
		        .setLanguageCode("vi-VN")
		        .setSampleRateHertz(16000)
		        .setModel("default")
		        .build();
		    StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder()
		        .setConfig(recConfig)
		        .build();
		    ResponseApiStreamingObserver<StreamingRecognizeResponse> responseObserver =
		        new ResponseApiStreamingObserver<>();

		    //------------------------------------------
		    AudioRecordDemo record = new AudioRecordDemo();
		    Thread t = new Thread() {
		    	public void run() {
		    		try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
					}
		    		record.finish();
		    	}
		    };
		    t.start();
		    record.startRecordFile(new File(tempFile));
		    while (t.isAlive()) {
		    	Thread.sleep(1000);
		    }
		    System.out.println("DONE");
		    //--------------------------------------------------
		    path = Paths.get(tempFile);
			data = Files.readAllBytes(path);
			
		    BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable =
			        speech.streamingRecognizeCallable();

		    ApiStreamObserver<StreamingRecognizeRequest> requestObserver =
			        callable.bidiStreamingCall(responseObserver);

		    // The first request must **only** contain the audio configuration:
		    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
		        .setStreamingConfig(config)
		        .build());
		    // Subsequent requests must **only** contain the audio data.
		    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
		        .setAudioContent(ByteString.copyFrom(data))
		        .build());

		    // Mark transmission as completed after sending the data.
		    requestObserver.onCompleted();

		    List<StreamingRecognizeResponse> responses = responseObserver.future().get();

		    for (StreamingRecognizeResponse response : responses) {
		      // For streaming recognize, the results list has one is_final result (if available) followed
		      // by a number of in-progress results (if iterim_results is true) for subsequent utterances.
		      // Just print the first result here.
		      StreamingRecognitionResult result = response.getResultsList().get(0);
		      // There can be several alternative transcripts for a given chunk of speech. Just use the
		      // first (most likely) one here.
		      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
		      System.out.printf("Transcript : %s\n", alternative.getTranscript());
		    }
		  }
	}
	
	/**
	 * Demonstrates using the Speech API to transcribe an audio file.
	 */
	public static void syncRecognizeFile2() throws Exception {
		// Instantiates a client
		try (SpeechClient speechClient = SpeechClient.create()) {

			// The path to the audio file to transcribe
			String resourceFolder = System.getenv("RESOURCE_FOLDER");
			System.out.println(resourceFolder);
			String fileName = resourceFolder + "/audio.raw";

			// Reads the audio file into memory
			Path path = Paths.get(fileName);
			byte[] data = Files.readAllBytes(path);
			ByteString audioBytes = ByteString.copyFrom(data);

			// Builds the sync recognize request
			RecognitionConfig config = RecognitionConfig.newBuilder().setEncoding(AudioEncoding.LINEAR16)
					.setSampleRateHertz(16000).setLanguageCode("en-US").build();
			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

			// Performs speech recognition on the audio file
			RecognizeResponse response = speechClient.recognize(config, audio);
			List<SpeechRecognitionResult> results = response.getResultsList();

			for (SpeechRecognitionResult result : results) {
				// There can be several alternative transcripts for a given chunk of speech.
				// Just use the
				// first (most likely) one here.
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				System.out.printf("Transcription: %s%n", alternative.getTranscript());
			}
		}
	}

	public static void syncRecognizeFile(String fileName) throws Exception {
		try (SpeechClient speech = SpeechClient.create()) {
			Path path = Paths.get(fileName);
			byte[] data = Files.readAllBytes(path);
			ByteString audioBytes = ByteString.copyFrom(data);

			// Configure request with local raw PCM audio
			RecognitionConfig config = RecognitionConfig.newBuilder().setEncoding(AudioEncoding.LINEAR16)
					.setLanguageCode("en-US").setSampleRateHertz(16000).build();
			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

			// Use blocking call to get audio transcript
			RecognizeResponse response = speech.recognize(config, audio);
			List<SpeechRecognitionResult> results = response.getResultsList();

			for (SpeechRecognitionResult result : results) {
				// There can be several alternative transcripts for a given chunk of speech.
				// Just use the
				// first (most likely) one here.
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				System.out.printf("Transcription: %s%n", alternative.getTranscript());
			}
		}
	}

	public static void syncRecognizeGcs(String gcsUri) throws Exception {
		// Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
		try (SpeechClient speech = SpeechClient.create()) {
			// Builds the request for remote FLAC file
			RecognitionConfig config = RecognitionConfig.newBuilder()
					.setEncoding(AudioEncoding.FLAC)
					.setLanguageCode("en-US")
					.setSampleRateHertz(16000)
					.build();
			RecognitionAudio audio = RecognitionAudio.newBuilder()
					.setUri(gcsUri)
					.build();

			// Use blocking call for getting audio transcript
			RecognizeResponse response = speech.recognize(config, audio);
			List<SpeechRecognitionResult> results = response.getResultsList();

			for (SpeechRecognitionResult result : results) {
				// There can be several alternative transcripts for a given chunk of speech.
				// Just use the
				// first (most likely) one here.
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				System.out.printf("Transcription: %s%n", alternative.getTranscript());
			}
		}
	}
	
	public static void asyncRecognizeGcs(String gcsUri) throws Exception {
		  // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
		  try (SpeechClient speech = SpeechClient.create()) {

		    // Configure remote file request for Linear16
		    RecognitionConfig config = RecognitionConfig.newBuilder()
		        .setEncoding(AudioEncoding.FLAC)
		        .setLanguageCode("en-US")
		        .setSampleRateHertz(16000)
		        .build();
		    RecognitionAudio audio = RecognitionAudio.newBuilder()
		        .setUri(gcsUri)
		        .build();

			// Use non-blocking call for getting file transcription
			OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speech
					.longRunningRecognizeAsync(config, audio);
			while (!response.isDone()) {
				System.out.println("Waiting for response...");
				Thread.sleep(10000);
			}

			List<SpeechRecognitionResult> results = response.get().getResultsList();

			for (SpeechRecognitionResult result : results) {
				// There can be several alternative transcripts for a given chunk of speech.
				// Just use the
				// first (most likely) one here.
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				System.out.printf("Transcription: %s\n", alternative.getTranscript());
			}
		  }
		}

	public static void streamingRecognizeFile(String fileName) throws Exception, IOException {
		  Path path = Paths.get(fileName);
		  byte[] data = Files.readAllBytes(path);

		  // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
		  try (SpeechClient speech = SpeechClient.create()) {

		    // Configure request with local raw PCM audio
		    RecognitionConfig recConfig = RecognitionConfig.newBuilder()
		        .setEncoding(AudioEncoding.LINEAR16)
//		        .setLanguageCode("en-US")
		        .setLanguageCode("vi-VN")
		        .setSampleRateHertz(16000)
		        .setModel("default")
		        .build();
		    StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder()
		        .setConfig(recConfig)
		        .build();
		    ResponseApiStreamingObserver<StreamingRecognizeResponse> responseObserver =
		        new ResponseApiStreamingObserver<>();

		    BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable =
		        speech.streamingRecognizeCallable();

		    ApiStreamObserver<StreamingRecognizeRequest> requestObserver =
		        callable.bidiStreamingCall(responseObserver);

		    // The first request must **only** contain the audio configuration:
		    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
		        .setStreamingConfig(config)
		        .build());

		    // Subsequent requests must **only** contain the audio data.
		    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
		        .setAudioContent(ByteString.copyFrom(data))
		        .build());

		    // Mark transmission as completed after sending the data.
		    requestObserver.onCompleted();

		    List<StreamingRecognizeResponse> responses = responseObserver.future().get();

		    for (StreamingRecognizeResponse response : responses) {
		      // For streaming recognize, the results list has one is_final result (if available) followed
		      // by a number of in-progress results (if iterim_results is true) for subsequent utterances.
		      // Just print the first result here.
		      StreamingRecognitionResult result = response.getResultsList().get(0);
		      // There can be several alternative transcripts for a given chunk of speech. Just use the
		      // first (most likely) one here.
		      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
		      System.out.printf("Transcript : %s\n", alternative.getTranscript());
		    }
		  }
		}
	
	
}