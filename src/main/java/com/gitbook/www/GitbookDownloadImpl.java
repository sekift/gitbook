package com.gitbook.www;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 下载Gitbook书籍
 */
public class GitbookDownloadImpl implements Runnable {
	// 连接时间 单位是毫秒
	private static final int connectTime = 1 * 60 * 1000;// 1分钟
	private static final int pageConnectTime = 1 * 60 * 1000;// 1分钟

	private static String tempDir = "D://gitbook//";

	private String origin_url;
	private String book_type;

	public String getOrigin_url() {
		return origin_url;
	}

	public String getBook_type() {
		return book_type;
	}

	// 下载什么版本：pdf、mobi、epub
	public static final String PDF = "pdf";
	public static final String MOBI = "mobi";
	public static final String EPUB = "epub";

	public GitbookDownloadImpl(String url, String type) {
		this.origin_url = url;
		this.book_type = type;
	}

	@Override
	public void run() {
		System.out.println(new Date(System.currentTimeMillis()) + " : " + this.getOrigin_url() + " 开始执行……");
		getListPage(this.getOrigin_url(), this.getBook_type());
		System.out.println(this.getOrigin_url() + " 执行完成！");
	}

	private static void getListPage(String url, String type) {
		try {
			Document document = Jsoup.connect(url).userAgent("Mozilla").timeout(connectTime).get(); // 处理首页
			Elements htmls = document.select("div.Book .book-infos a");
			String html = "";
			for (Element element : htmls) {
				html = element.attr("href");
				if (html != null && !html.equals("") && html.startsWith("https://www.gitbook.com/book/")) {
					getPage(html, type, url);
				}
			}
		} catch (IOException e) {
			int times = 0;
			System.out.println(url + " 发生了错误，" + e);
			if (e.getMessage().contains("Connection timed out")) {
				System.out.println(url + " 正在重新连接……");
				times++;
				if (times < 5) { // 最多重新连接5次
					getListPage(url, type);
				} else {
					System.out.println(url + " 重试次数大于5，已放弃。");
				}
			}
			times = 0; // 虽然没有必要
		}
	}

	private static void getPage(String url, String type, String originUrl) {
		try {
			Document document = Jsoup.connect(url).userAgent("Mozilla").timeout(pageConnectTime).get();
			String file_name = "";
			String book_path = "";

			Elements titleLinkElements = document.select("title");
			for (Element element : titleLinkElements) {
				file_name = element.text();
				file_name = file_name.substring(0, file_name.length() - 10);
			}

			book_path = url.replace("https://www.gitbook.com/", "https://www.gitbook.com/download/" + type + "/");
			downloadFile(file_name, book_path, type, originUrl);
		} catch (IOException e) {
			int times = 0;
			System.out.println(originUrl + " : " + url + " 发生了错误，" + e);
			if (e.getMessage().contains("Connection timed out")) {
				System.out.println(originUrl + " : " + url + " 正在重新连接……");
				times++;
				if (times < 5) { // 最多重新连接5次
					getPage(url, type, originUrl);
				} else {
					System.out.println(originUrl + " : " + url + " 重试次数大于5，已放弃。");
				}
			}
			times = 0;
		}
	}

	private final static int BUFFER = 1024;

	private static void downloadFile(String dir, String file, String type, String originUrl) {
		if (dir == null) {
			System.out.println(originUrl + " : " + dir + " 不存在");
			return;
		}

		String[] sigal = { "/", "\\", ":", "?", "\"", "<", ">", "|" };
		for (String str : sigal) {
			if (dir.contains(str)) {
				dir = dir.replace(str, " ");
			}
		}
		File firdir = new File(tempDir);
		if (!firdir.exists()) {
			firdir.mkdirs();
		}
		String filePath = tempDir + dir + "." + type;
		File fileName = new File(filePath);
		if (fileName.isFile() && fileName.exists()) {
			System.out.println(originUrl + " : " + dir + " 已经存在");
			return;
		}
		// int bytesum = 0;
		// int byteread = 0;
		// URL url = null;
		FileOutputStream out = null;
		GetMethod httpGet = null;
		HttpClient client = null;
		// URLConnection conn = null;
		try {
			// url = new URL(file);
			// conn = url.openConnection();
			// conn.setRequestProperty("accept", "*/*");
			// conn.setRequestProperty("connection", "Keep-Alive");
			// conn.setRequestProperty("user-agent",
			// "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// // 建立实际的连接
			// conn.connect();

			client = new HttpClient();
			// 链接超时
			client.getHttpConnectionManager().getParams().setConnectionTimeout(60 * 1000);
			// 读取超时
			client.getHttpConnectionManager().getParams().setSoTimeout(60 * 1000);
			httpGet = new GetMethod(file);
			client.executeMethod(httpGet);
			// InputStream inStream = conn.getInputStream();
			InputStream is = httpGet.getResponseBodyAsStream();
			out = new FileOutputStream(filePath);
			System.out.println(originUrl + " : " + dir + " 准备下载……");
			long beginTime = System.currentTimeMillis();
			/*
			 * byte[] buffer = new byte[1024]; while ((byteread =
			 * inStream.read(buffer)) != -1) { bytesum += byteread;
			 * fs.write(buffer, 0, byteread); fs.flush(); }
			 */
			byte[] buffer = new byte[BUFFER];
			int ch = 0;
			while ((ch = is.read(buffer)) > 0) {
				out.write(buffer, 0, ch);
			}
			is.close();
			long endTime = System.currentTimeMillis();
			System.out.println(originUrl + " : " + dir + " 下载完成，用时 " + ((endTime - beginTime) / 1000) + " 秒！");
		} catch (Exception e) {
			System.out.println(originUrl + " : " + dir + " 无法下载 " + e);
			int times = 0;
			if (e.getMessage().contains("Premature EOF")) {
				System.out.println(originUrl + " : " + dir + " 正在重新下载……");
				times++;
				if (times < 3) { // 最多重新连接3次
					downloadFile(dir, file, type, originUrl);
				} else {
					System.out.println(originUrl + " : " + dir + " 重试次数大于3，已放弃。");
				}
			}
			times = 0;
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				httpGet.releaseConnection();
				client.getHttpConnectionManager().closeIdleConnections(0);
			} catch (Exception e) {
			}
		}
	}
}
