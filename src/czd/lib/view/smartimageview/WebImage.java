package czd.lib.view.smartimageview;

import android.content.Context;
import android.graphics.Bitmap;
import czd.lib.application.ApplicationUtil;
import czd.lib.data.ImageUtil;
import czd.lib.view.smartimageview.SmartImageTask.OnCompleteHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebImage implements SmartImage {
	private String url;

	public WebImage(String url) {
		this.url = url;
	}

	@Override
	public Bitmap getBitmap(Context context) {
		if (ApplicationUtil.webImageCache == null) {
			ApplicationUtil.webImageCache = new WebImageCache(context);
		}

		// Try getting bitmap from cache first
		Bitmap bitmap = null;
		if (!Thread.currentThread().isInterrupted() && url != null) {
			bitmap = ApplicationUtil.webImageCache.get(url);
			if (bitmap == null) {
				bitmap = ImageUtil.getBitmapFromUrl(url);
				if (bitmap != null) {
					ApplicationUtil.webImageCache.put(url, bitmap);
				}
			}
		}
		return bitmap;
	}

	public Bitmap getBitmap(Context context, int width, int height) {
		if (ApplicationUtil.webImageCache == null) {
			ApplicationUtil.webImageCache = new WebImageCache(context);
		}

		// Try getting bitmap from cache first
		Bitmap bitmap = null;
		if (!Thread.currentThread().isInterrupted() && url != null) {
			bitmap = ApplicationUtil.webImageCache.get(url, width, height);
			if (bitmap == null) {
				bitmap = ImageUtil.getBitmapFromUrl(url);
				if (bitmap != null) {
					ApplicationUtil.webImageCache.put(url, bitmap);
				}
			}
		}
		return bitmap;
	}

	public Bitmap getBitmap(Context context, OnCompleteHandler handler) {
		if (ApplicationUtil.webImageCache == null) {
			ApplicationUtil.webImageCache = new WebImageCache(context);
		}

		// Try getting bitmap from cache first
		Bitmap bitmap = null;
		if (!Thread.currentThread().isInterrupted() && url != null) {
			bitmap = ApplicationUtil.webImageCache.get(url);
			if (bitmap == null) {
				if (getImageFromUrl(url, handler)) {
					bitmap = ApplicationUtil.webImageCache.get(url);
				}
			}
		}
		return bitmap;
	}

	public Bitmap getBitmap(Context context, int width, int height, OnCompleteHandler handler) {
		if (ApplicationUtil.webImageCache == null) {
			ApplicationUtil.webImageCache = new WebImageCache(context);
		}

		// Try getting bitmap from cache first
		Bitmap bitmap = null;
		if (!Thread.currentThread().isInterrupted() && url != null) {
			bitmap = ApplicationUtil.webImageCache.get(url, width, height);
			if (bitmap == null) {
				if (getImageFromUrl(url, handler)) {
					bitmap = ApplicationUtil.webImageCache.get(url, width, height);
				}
			}
		}
		return bitmap;
	}
	
	
	
	@Override
	public String toString() {
		return url;
	}

	public static void removeFromCache(String url) {
		if (ApplicationUtil.webImageCache != null) {
			ApplicationUtil.webImageCache.remove(url);
		}
	}

	private void sendProgressMessage(OnCompleteHandler handler, long current, long total) {
		handler.sendMessage(handler.obtainMessage(SmartImageTask.BITMAP_LOADING, new Object[] { current, total }));
	}

	private boolean getImageFromUrl(String url, OnCompleteHandler handler) {
		if (!Thread.currentThread().isInterrupted()) {
			String filepath = ApplicationUtil.webImageCache.getFilePath(url);
			InputStream is = null;
			FileOutputStream os = null;
			try {
				File file = new File(filepath);
				boolean append;
				long current = 0;
				long total = 0;
				if (!file.exists()) {
					file.createNewFile();
					append = false;
				}
				else if (!file.canWrite()) {
					return false;
				}
				else {
					append = true;
					current = file.length();
				}
				HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setConnectTimeout(2000);
				conn.setUseCaches(true);
				conn.setInstanceFollowRedirects(true);
				if (current > 0) {
					conn.setRequestProperty("RANGE", "bytes=" + current + "-");
				}

				int code = conn.getResponseCode();
				if (code == 416) {
					sendProgressMessage(handler, current, current);
					return true;
				}
				else if (code > 300) {
					return false;
				}
				else {
					String header = conn.getHeaderField("Content-Range");
					if (header == null || header.length() == 0) {
						append = false;
						current = 0;
					}

					os = new FileOutputStream(file, append);
					is = conn.getInputStream();
					total = conn.getContentLength() + current;
					int k = 0;
					byte[] buffer = new byte[4096];
					while (!Thread.currentThread().isInterrupted() && current < total && (k = is.read(buffer, 0, 4096)) > 0) {
						os.write(buffer, 0, k);
						current += k;
						sendProgressMessage(handler, current, total);
					}
					os.flush();
					if (Thread.currentThread().isInterrupted()) {
						return false;
					}
					else {
						return true;
					}
				}
			} catch (Exception e) {
				return false;
			} finally {
				try {
					is.close();
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

}
