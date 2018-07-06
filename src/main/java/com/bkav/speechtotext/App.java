package com.bkav.speechtotext;

import java.io.File;

import com.bkav.speechtotext.audio.AudioRecordDemo;

public class App 
{
    public static void main( String[] args ) throws Exception
    {
//    	AudioRecordDemo.demo2();
        System.out.println("IT's WORKED");
        
//        OutputStream outputStream = new ByteArrayOutputStream();
//        outputStream = new FileOutputStream("/home/namnk/Desktop/demo.wav");
	    AudioRecordDemo record = new AudioRecordDemo();
	    new Thread() {
	    	public void run() {
	    		try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
	    		record.finish();
	    	}
	    }.start();
	    record.startRecordFile(new File("/home/namnk/Desktop/demo.wav"));
//	    byte[] data = outputStream.toByteArray();
//	    System.out.println(data.length);
	    
//	    outputStream.close();
    }
}
