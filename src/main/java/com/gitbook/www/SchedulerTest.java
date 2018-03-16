package com.gitbook.www;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SchedulerTest {
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public void beepForAnHour() {
		/*final Runnable beeper = new Runnable() {
			public void run() {
				System.out.println("beep");
			}
		};*/
		GitbookDownloadImpl wcl = new GitbookDownloadImpl("aaa","pdf");
		/*final ScheduledFuture<?> beeperHandle = */scheduler.scheduleAtFixedRate(wcl, 0, 10, SECONDS);
		/*scheduler.schedule(new Runnable() {
			public void run() {
				beeperHandle.cancel(true);
			}
		}, 60 * 60, SECONDS);*/
	}
	
	public static void main(String args[]){
		SchedulerTest bc = new SchedulerTest();
		bc.beepForAnHour();
	}
}
