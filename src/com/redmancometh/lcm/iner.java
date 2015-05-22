package com.redmancometh.lcm;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

public class iner implements Observer {
	
	private static final String DEFAULT_URL = "http://127.0.0.1:9332/";
	private static final String DEFAULT_AUTH = "rpcuser:rpcpass";
	private static final long DEFAULT_SCAN_TIME = 5000;
	private static final long DEFAULT_RETRY_PAUSE = 30000;
	
	private orker worker;
	private long lastWorkTime;
	private long lastWorkHashes;
	
	public iner(String url, String auth, long scanTime, long retryPause, int nThread, double throttle) {
		if (nThread < 1)
			throw new IllegalArgumentException("Invalid number of threads: " + nThread);
		if (throttle <= 0.0 || throttle > 1.0)
			throw new IllegalArgumentException("Invalid throttle: " + throttle);
		if (scanTime < 1L)
			throw new IllegalArgumentException("Invalid scan time: " + scanTime);
		if (retryPause < 0L)
			throw new IllegalArgumentException("Invalid retry pause: " + retryPause);
		try 
		{
			worker = new orker(new URL(url), auth, scanTime, retryPause, nThread, throttle);
		}
		catch (MalformedURLException e) 
		{
			throw new IllegalArgumentException("Invalid URL: " + url);
		}
		worker.addObserver(this);
		Thread t = new Thread(worker);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		log(nThread + " miner threads started");
	}
	
	private static final DateFormat logDateFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");
	
	public void log(String str) {
		System.out.println(logDateFormat.format(new Date()) + str);
	}
	
	public void update(Observable o, Object arg) {
		orker.Notification n = (orker.Notification) arg;
		if (n == orker.Notification.SYSTEM_ERROR) {
			log("System error");
			System.exit(1);
		} else if (n == orker.Notification.PERMISSION_ERROR) {
			log("Permission error");
			System.exit(1);
		} else if (n == orker.Notification.AUTHENTICATION_ERROR) {
			log("Invalid worker username or password");
			System.exit(1);
		} else if (n == orker.Notification.CONNECTION_ERROR) {
			log("Connection error, retrying in " + worker.getRetryPause()/1000L + " seconds");
		} else if (n == orker.Notification.COMMUNICATION_ERROR) {
			log("Communication error");
		} else if (n == orker.Notification.LONG_POLLING_FAILED) {
			log("Long polling failed");
		} else if (n == orker.Notification.LONG_POLLING_ENABLED) {
			log("Long polling activated");
		} else if (n == orker.Notification.NEW_BLOCK_DETECTED) {
			log("LONGPOLL detected new block");
		} else if (n == orker.Notification.POW_TRUE) {
			log("PROOF OF WORK RESULT: true (yay!!!)");
		} else if (n == orker.Notification.POW_FALSE) {
			log("PROOF OF WORK RESULT: false (booooo)");
		} else if (n == orker.Notification.NEW_WORK) {
			if (lastWorkTime > 0L) {
				long hashes = worker.getHashes() - lastWorkHashes;
				float speed = (float) hashes / Math.max(1, System.currentTimeMillis() - lastWorkTime);
				log(String.format("%d hashes, %.2f khash/s", hashes, speed));
			}
			lastWorkTime = System.currentTimeMillis();
			lastWorkHashes = worker.getHashes();
		}
	}
	
	public static void main(String[] args) {
		String url = DEFAULT_URL;
		String auth = DEFAULT_AUTH;
		int nThread = Runtime.getRuntime().availableProcessors();
		double throttle = 1.0;
		long scanTime = DEFAULT_SCAN_TIME;
		long retryPause = DEFAULT_RETRY_PAUSE;
		
		if (args.length > 0 && args[0].equals("--help")) {
			System.out.println("Usage:  java Miner [URL] [USERNAME:PASSWORD] [THREADS] [THROTTLE] [SCANTIME] [RETRYPAUSE]");
			return;
		}
		
		if (args.length > 0) url = args[0];
		if (args.length > 1) auth = args[1];
		if (args.length > 2) nThread = Integer.parseInt(args[2]);
		if (args.length > 3) throttle = Double.parseDouble(args[3]);
		if (args.length > 4) scanTime = Integer.parseInt(args[4]) * 1000L;
		if (args.length > 5) retryPause = Integer.parseInt(args[5]) * 1000L;
		
		try {
			new iner(url, auth, scanTime, retryPause, nThread, throttle);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

}
