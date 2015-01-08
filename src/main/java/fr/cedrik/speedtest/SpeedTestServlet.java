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
	displayName="SpeedTest Servlet",
	loadOnStartup=0,
	urlPatterns={"/*"},
	initParams={/*@WebInitParam(name="", value="")*/})
@SuppressWarnings("serial")
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
		// ensure we get an un-encrypted channel, so that CPU is not a limiting factor
		if (request.isSecure()) {
			response.sendRedirect(response.encodeRedirectURL(getUnsecureRequestURLWithQueryString(request)));
			return;
		}
		String servletPath = request.getPathInfo();
		if (servletPath == null || servletPath.isEmpty() || "/".equals(servletPath)) {
			// display help
			response.setContentType("text/plain;charset=UTF-8");//$NON-NLS-1$
			try (PrintWriter output = response.getWriter()) {
				output.println("Usage");
				output.println("=====");
				output.println();
				output.print("Test download:	curl -o /dev/null ");output.print(request.getRequestURL());output.println("<size>[kKmMgG]");
				output.println("	k: kilo");
				output.println("	K: kili");
				output.println("	m: mega");
				output.println("	M: megi");
				output.println("	g: giga");
				output.println("	G: gigi");
				output.println();
				output.print("Test upload:	curl -X POST -T <big_file> ");output.println(request.getRequestURL());
				output.println("the returned number is your upload speed in bytes / second.");
				output.println();
				output.println();
				output.println();
				output.println("Sources: https://github.com/javabean/SpeedTest");
			}
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
		if (requestedSize < Integer.MAX_VALUE) {
			response.setContentLength((int)requestedSize);
		}
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
		// ensure we get an un-encrypted channel, so that CPU is not a limiting factor
		if (request.isSecure()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Can not test network speed over encrypted channel (CPU limitation)");
			return;
		}
		response.setContentType("text/plain;charset=UTF-8");//$NON-NLS-1$
		try (ServletInputStream input = request.getInputStream(); PrintWriter output = response.getWriter()) {
			int bytesRead;
			long totalBytes = 0;
			byte[] buffer = new byte[65536];
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


	/**
	 * Reconstructs the URL the client used to make the request,
	 * using information in the <code>HttpServletRequest</code> object.
	 * The returned URL contains a protocol, server name, port
	 * number, and server path, and include query
	 * string parameters.
	 *
	 * <p>This method is useful for creating redirect messages
	 * and for reporting errors.
	 *
	 * @param request	a <code>HttpServletRequest</code> object
	 *			containing the client's request
	 *
	 * @return		a <code>String</code> object containing
	 *			the reconstructed URL
	 */
	private static String getUnsecureRequestURLWithQueryString(HttpServletRequest request) {
		/*
		StringBuffer requestURL = request.getRequestURL();
		String queryString = request.getQueryString();
		if (queryString != null && ! queryString.isEmpty()) {
			requestURL.append('?').append(queryString);
		}
		return requestURL.toString();
		*/
		StringBuilder url = new StringBuilder(32);
		//String scheme = request.getScheme();
		int port = request.getServerPort();
		if (port < 0) {
			port = 80; // Work around java.net.URL bug
		}
		//String 	pathInfo = req.getPathInfo();
		String queryString = request.getQueryString();

		url.append("http");	// http, https
		url.append("://"); //$NON-NLS-1$
		url.append(request.getServerName());
		//if ((scheme.equals ("http") && port != 80)
		//		|| (scheme.equals ("https") && port != 443)) {
		//	url.append (':');
		//	url.append (port);
		//}
		//url.append(req.getContextPath());
		//url.append (req.getServletPath());
		//if (pathInfo != null)
		//	url.append (pathInfo);
		url.append(request.getRequestURI());
		if (queryString != null) {
			url.append('?').append(queryString);
		}
		return url.toString();
	}
}
