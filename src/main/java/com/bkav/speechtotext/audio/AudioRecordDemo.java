package com.bkav.speechtotext.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class AudioRecordDemo {

	public static final long RECORD_TIME = 3000;

	public static void demo() {
		AudioFormat format = getAudioFormat();
		TargetDataLine microphone;
		SourceDataLine speakers;
		try {
			microphone = AudioSystem.getTargetDataLine(format);
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			microphone = (TargetDataLine)AudioSystem.getLine(info);
			microphone.open(format);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int numBytesRead;
			int CHUNK_SIZE = 1024;
			
			byte[] data = new byte[microphone.getBufferSize() / 5];
			microphone.start();
			
			int bytesRead = 0;
			DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
			speakers = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
			speakers.open(format);
			speakers.start();
			while(bytesRead < 100000) {
				numBytesRead = microphone.read(data, 0, CHUNK_SIZE);
				bytesRead += numBytesRead;
				out.write(data, 0, numBytesRead);
				speakers.write(data, 0, numBytesRead);
			}
			speakers.drain();
			speakers.close();
			microphone.close();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	public static void demo2() {
		AudioFormat format = getAudioFormat();
		TargetDataLine microphone;
		AudioInputStream audioInputStream;
		SourceDataLine speakers;
		try {
			microphone = AudioSystem.getTargetDataLine(format);
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			microphone = (TargetDataLine)AudioSystem.getLine(info);
			microphone.open(format);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int numBytesRead;
			int CHUNK_SIZE = 1024;
			byte[] data = new byte[microphone.getBufferSize() / 5];
			microphone.start();
			
			int bytesRead = 0;
			try {
				while(bytesRead < 100000) {
					numBytesRead = microphone.read(data, 0, CHUNK_SIZE);
					bytesRead += numBytesRead;
					System.out.println(bytesRead);
					out.write(data, 0, numBytesRead);
//					speakers.write(data, 0, numBytesRead);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			byte[] audioData = out.toByteArray();
			InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
			audioInputStream = new AudioInputStream(byteArrayInputStream, format, audioData.length / format.getFrameSize());
			DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
			speakers = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
			speakers.open(format);
			speakers.start();
			int cnt = 0;
			byte[] tempBuffer = new byte[10000];
			try {
				while ((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
					if (cnt > 0) {
						
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			// Block and wait for internal buffer of the data line to empty
			speakers.drain();
			speakers.close();
			microphone.close();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	public static AudioFormat getAudioFormat() {
		float sampleRate = 16000;
		int sampleSizeInBits = 16;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = true;
		AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		return format;
	}
	public void startRecordFile(File outputStream) {
		try {
			AudioFormat format = getAudioFormat();
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

			if (!AudioSystem.isLineSupported(info)) {
				System.out.println("Line not supported");
				System.exit(0);
			}
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start();// start capturing
			System.out.println("Start capturing...");
			AudioInputStream ais = new AudioInputStream(line);
			System.out.println("Start recording...");
			// start recording
			if (outputStream == null) {
				AudioSystem.write(ais, fileType, wavFile);				
			} else {
				AudioSystem.write(ais, fileType, outputStream);
			}
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void start(OutputStream outputStream) {
		try {
			AudioFormat format = getAudioFormat();
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

			if (!AudioSystem.isLineSupported(info)) {
				System.out.println("Line not supported");
				System.exit(0);
			}
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start();// start capturing
			System.out.println("Start capturing...");
			AudioInputStream ais = new AudioInputStream(line);
			System.out.println("Start recording...");
			// start recording
			if (outputStream == null) {
				AudioSystem.write(ais, fileType, wavFile);				
			} else {
				AudioSystem.write(ais, fileType, outputStream);
			}
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finish() {
		line.stop();
		line.close();
		System.out.println("Finished");
	}

	public static void main(String[] args) {
		final AudioRecordDemo recorder = new AudioRecordDemo();
		// creates new thread that waits for a specified of time before stopping
		Thread stopperr = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(RECORD_TIME);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
				recorder.finish();
			}
		});
		stopperr.start();
		// start recording
		recorder.start(null);
	}

	private File wavFile = new File("/home/namnk/Desktop/record.wav");
	private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
	private TargetDataLine line;
}
