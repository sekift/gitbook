package com.gitbook.www;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * 文件下载管理类
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(locations = { "file:src/main/resources/applicationContext.xml" })
public class DownLoadManagerTest { // extends
									// AbstractTransactionalJUnit4SpringContextTests
	private static final Logger LOGGER = LoggerFactory.getLogger(DownLoadManagerTest.class);

	/**
	 * 每个线程下载的字节数
	 */
	private long unitSize = 5 * 1024 * 1024;

	@Autowired
	private TaskExecutor taskExecutor;

	private CloseableHttpClient httpClient;

	private Long starttimes;

	private Long endtimes;

	@Before
	public void setUp() throws Exception {
		starttimes = System.currentTimeMillis();
		System.out.println("测试开始....");
	}

	@After
	public void tearDown() throws Exception {
		endtimes = System.currentTimeMillis();
		System.out.println("测试结束!!");
		System.out.println("********************");
		System.out.println("下载总耗时:" + (endtimes - starttimes) / 1000 + "s");
		System.out.println("********************");
	}

	public DownLoadManagerTest() {
	}

	/**
	 * 
	 * 启动多个线程下载文件
	 */
	@Test
	public void doDownload() throws IOException {
		System.out.println("初始化测试类....");
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(100);
		httpClient = HttpClients.custom().setConnectionManager(cm).build();
		String remoteFileUrl = "https://d.pcs.baidu.com/file/bad5f7032dc0c921f590b746e9f706d5?fid=2483207920-250528-310894743363024&time=1498543226&rt=sh&sign=FDTAERVY-DCb740ccc5511e5e8fedcff06b081203-Gis%2FjXYCO8DQvtz%2FZpKbJ61Eubs%3D&expires=8h&chkv=1&chkbd=0&chkpc=et&dp-logid=4118666093708400253&dp-callid=0&r=943476364";
		//https://www.gitbook.com/download/pdf/book/yourtion/sparkinternals
		//
		String localPath = "D://test//";

		String fileName = new URL(remoteFileUrl).getFile();

		System.out.println("远程文件名称：" + fileName);
		fileName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length()).replace("%20", " ");
		fileName = fileName+".pdf";
		System.out.println("本地文件名称：" + fileName);
		System.out.println("本地文件路径：" + localPath + fileName);

		long fileSize = this.getRemoteFileSize(remoteFileUrl);
		System.out.println("远程文件大小：" + fileSize);

		//this.createFile(localPath + System.currentTimeMillis() + fileName, fileSize);

		Long threadCount = (fileSize / unitSize) + (fileSize % unitSize != 0 ? 1 : 0);
		long offset = 0;
		System.out.println("threadCount：" + threadCount);
		CountDownLatch end = new CountDownLatch(threadCount.intValue());

		if (fileSize <= unitSize) {// 如果远程文件尺寸小于等于unitSize
			System.out.println("本地文件名称：" + fileName);
			DownloadThreadTest downloadThread = new DownloadThreadTest(remoteFileUrl, localPath + fileName, offset,
					fileSize, end, httpClient);
			taskExecutor.execute(downloadThread);
		} else {// 如果远程文件尺寸大于unitSize
			System.out.println("本地文件名称：" + fileName);
			for (int i = 1; i < threadCount; i++) {
				DownloadThreadTest downloadThread = new DownloadThreadTest(
						remoteFileUrl, localPath + fileName, offset, unitSize, end, httpClient);
				taskExecutor.execute(downloadThread);
				offset = offset + unitSize;
			}
			if (fileSize % unitSize != 0) {// 如果不能整除，则需要再创建一个线程下载剩余字节
				DownloadThreadTest downloadThread = new DownloadThreadTest(remoteFileUrl, localPath + fileName, offset,
						fileSize - unitSize * (threadCount - 1), end, httpClient);
				taskExecutor.execute(downloadThread);
			}
		}
		try {
			end.await();
		} catch (InterruptedException e) {
			LOGGER.error("DownLoadManager exception msg:{}", ExceptionUtils.getFullStackTrace(e));
			e.printStackTrace();
		}
		LOGGER.info("下载完成！{} ", localPath + fileName);
		// return localPath+fileName;
	}

	/**
	 * TODO 有错误，pdf的获取不到大小
	 * 获取远程文件尺寸
	 */
	private long getRemoteFileSize(String remoteFileUrl) throws IOException {
		long fileSize = 0;
		HttpURLConnection httpConnection = (HttpURLConnection) new URL(remoteFileUrl).openConnection();
		httpConnection.setRequestMethod("HEAD");
		int responseCode = httpConnection.getResponseCode();
		System.out.println("服务器返回码:" + responseCode);
		if (responseCode >= 400) {
			LOGGER.debug("Web服务器响应错误!");
			return 0;
		}
		String sHeader = null;
		for (int i = 1; i < 12; i++) {
			sHeader = httpConnection.getHeaderFieldKey(i);
			System.out.println("参数:" + sHeader+":值:"+httpConnection.getHeaderField(sHeader));
			if (sHeader != null && sHeader.equals("Content-Length")) {
				System.out.println("文件大小ContentLength:" + httpConnection.getContentLength());
				fileSize = Long.parseLong(httpConnection.getHeaderField(sHeader));
				break;
			}
		}
		if (fileSize == 0) {
			fileSize = 5 * 1024 * 1024;
		}
		return fileSize;
	}

	/**
	 * 
	 * 创建指定大小的文件
	 */
	@SuppressWarnings("unused")
	private void createFile(String fileName, long fileSize) throws IOException {
		File newFile = new File(fileName);
		RandomAccessFile raf = new RandomAccessFile(newFile, "rw");
		raf.setLength(fileSize);
		raf.close();
	}

	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
