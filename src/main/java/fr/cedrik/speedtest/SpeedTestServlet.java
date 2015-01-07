package fr.cedrik.speedtest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SpeedTestServlet
 */
@WebServlet(description="SpeedTest Servlet",
	name="SpeedTestServlet",
	displayName="MessAdmin Servlet",
	loadOnStartup=0,
	urlPatterns={"/*"},
	initParams={/*@WebInitParam(name="", value="")*/})
public class SpeedTestServlet extends HttpServlet {

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SpeedTestServlet() {
		super();
	}

	/** {@inheritDoc} */
	@Override
	public void init() throws ServletException {
		super.init();
		// code here
	}

	/** {@inheritDoc} */
	@Override
	public void destroy() {
		// code here
		super.destroy();
	}

	/** {@inheritDoc} */
	@Override
	public String getServletInfo() {
		return "SpeedTestServlet, copyright (c) 2015 Cédrik LIME";
	}

	/** {@inheritDoc} */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String servletPath = request.getPathInfo();
		if ("".equals(servletPath)) {
			// TODO display help
			return;
		}
	
		setNoCache(response);
		servletPath = servletPath.substring(1); // remove leading "/"
		char lastChar = servletPath.substring(servletPath.length()-1).charAt(0);
		long requestedSize;
		if (Character.isDigit(lastChar)) {
			requestedSize = Long.parseLong(servletPath);
		} else {
			servletPath = servletPath.substring(0, servletPath.length()-1); // remove last char
			requestedSize = Integer.parseInt(servletPath);
			switch (lastChar) {
			case 'k': // K
				requestedSize *= 1000;
				break;
			case 'K': // Ki
				requestedSize *= 1024;
				break;
			case 'm': // M
				requestedSize *= 1000 * 1000;
				break;
			case 'M': // Mi
				requestedSize *= 1024 * 1024;
				break;
			case 'g': // G
				requestedSize *= 1000 * 1000 * 1000;
				break;
			case 'G': // Gi
				requestedSize *= 1024 * 1024 * 1024;
				break;
			default:
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad size modifier: " + lastChar);
				return;
			}
		}

		response.setContentType("application/octet-stream");//$NON-NLS-1$
		byte[] buffer = new byte[65536];
		new Random().nextBytes(buffer); // fill with random to prevent upstream compression
		long totalWriten = 0;
		try (ServletOutputStream output = response.getOutputStream()) {
			while (totalWriten < requestedSize) {
				int toWrite = (int) Math.min(requestedSize - totalWriten, buffer.length);
				output.write(buffer, 0, toWrite);
				totalWriten += toWrite;
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		setNoCache(response);
		response.setContentType("text/plain;charset=UTF-8");//$NON-NLS-1$
		try (ServletInputStream input = request.getInputStream(); PrintWriter output = response.getWriter()) {
			int bytesRead;
			long totalBytes = 0;
			byte[] buffer = new byte[32768];
			long startTimeNano = System.nanoTime();
			while ((bytesRead = input.read(buffer)) != -1) {
				totalBytes += bytesRead;
			}
			long endTimeNano = System.nanoTime();
			output.write(Long.toString(TimeUnit.SECONDS.toNanos(totalBytes) / (endTimeNano-startTimeNano)));
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected static final String HEADER_LASTMOD      = "Last-Modified";//$NON-NLS-1$
	protected static final String HEADER_CACHECONTROL = "Cache-Control";//$NON-NLS-1$
	protected static final String HEADER_EXPIRES      = "Expires";//$NON-NLS-1$
	private void setNoCache(HttpServletResponse response) {
		// <strong>NOTE</strong> - This header will be overridden
		// automatically if a <code>RequestDispatcher.forward()</code> call is
		// ultimately invoked.
		//resp.setHeader("Pragma", "No-cache"); // HTTP 1.0 //$NON-NLS-1$ //$NON-NLS-2$
		response.setHeader(HEADER_CACHECONTROL, "no-cache,no-store,max-age=0"); // HTTP 1.1 //$NON-NLS-1$
		response.setDateHeader(HEADER_EXPIRES, 0); // 0 means now
		// should we decide to enable caching, here are the current vary:
		response.addHeader("Vary", "Accept-Language,Accept-Encoding,Accept-Charset");
	}
}
