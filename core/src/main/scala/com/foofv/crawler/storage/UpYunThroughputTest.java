package com.foofv.crawler.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import main.java.com.UpYun;

class TestTask implements Runnable {

	static int flowAmount;
	static long totalTime;
	static int tasks;
	boolean read;
	int requests;

	public TestTask(boolean read, int requests) {
		this.read = read;
		this.requests = requests <= 1 ? 1 : requests;
	}

	public static void clear() {
		flowAmount = tasks = 0;
		totalTime = 0;
	}

	public void run() {
		int size = 0;
		long time = 0;
		long t0 = System.currentTimeMillis();
		if (read) {
			for (int i = 0; i < requests; i++) {
				size += UpYunThroughputTest.readFile();
			}
		} else {
			for (int i = 0; i < requests; i++) {
				size += UpYunThroughputTest.writeFile();
			}
		}
		time = System.currentTimeMillis() - t0;
		synchronized (UpYunThroughputTest.lock) {
			flowAmount += size;
			totalTime += time;
			if (++tasks == UpYunThroughputTest.threads) {
				UpYunThroughputTest.lock.notify();
			}
		}
	}
}

public class UpYunThroughputTest {

	private static final String BUCKET_NAME = "xms57";
	private static final String OPERATOR_NAME = "wengbenjue";
	private static final String OPERATOR_PWD = "xiaomishu57";
	// private static final String FILE_512B = "/test512B";
	// private static final String FILE_1K = "/test1K";
	// private static final String FILE_2K = "/test2K";
	// private static final String FILE_3K = "/test3K";
	private static final String FILE_DIR = System.getProperty("user.dir");
	private static String content = "";
	private static boolean read = true;
	private static UpYun upyun = null;
	public static Integer lock = 0;
	public static int threads = 500;

	public static int readFile() {

		String upyunFile = "/tpTest.txt";
		String data = upyun.readFile(upyunFile);
		if (data == null) {
			System.err.println("READ ERROR");
			System.exit(1);
		}

		return data.length();
	}

	public static int writeFile() {

		String upyunFile = "/tpTest.txt";
		boolean success = upyun.writeFile(upyunFile, content);
		if (success) {
			return content.length();
		} else {
			System.err.println("WRITE ERROR");
			System.exit(1);
			return 0;
		}
	}

	private static void execute(int threads, int requests, boolean read) {

		for (int i = 0; i < threads; i++) {
			Thread thread = new Thread(new TestTask(read, requests));
			thread.start();
		}
	}

	private static void init(boolean read, String... fileName) {

		upyun = new UpYun(BUCKET_NAME, OPERATOR_NAME, OPERATOR_PWD);
		if (!(UpYunThroughputTest.read = read)) {
			File file = new File(FILE_DIR + fileName[0]);
			if (!file.isFile()) {
				System.out.println("本地待上传的测试文件不存在！");
				System.exit(0);
			} else {
				try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
					String line;
					while ((line = raf.readLine()) != null) {
						content += line + "\n";
					}
					content = content.substring(0, content.length() - 1);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void test(int requests) throws InterruptedException {

		execute(threads, requests, read);
		synchronized (lock) {
			while (TestTask.tasks < threads)
				lock.wait();
		}
		long totalTime = TestTask.totalTime;
		long totalFlow = TestTask.flowAmount;
		TestTask.clear();
		double time = (double) totalTime / 1000;
		System.out.println("size(B):" + totalFlow + " time(s):" + time
				+ " rate(B/s):" + totalFlow / time);
	}

	public static void main(String[] args) throws InterruptedException {

		init(true/*, "/test512B"*/);
		test(100);
	}
}
