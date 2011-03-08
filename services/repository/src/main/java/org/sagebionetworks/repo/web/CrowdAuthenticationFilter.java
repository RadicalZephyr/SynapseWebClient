package org.sagebionetworks.repo.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.sagebionetworks.repo.web.controller.BaseController;
import org.springframework.http.HttpStatus;
import org.xml.sax.InputSource;

/**
 *
 */
public class CrowdAuthenticationFilter implements Filter {
	private static final Logger log = Logger.getLogger(CrowdAuthenticationFilter.class
			.getName());
	
	private String crowdProtocol; // http or https
	private String crowdServer;
	private int crowdPort;
	private boolean allowAnonymous = false;
	
	@Override
	public void destroy() {
	}
	
	private static void reject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// reject
		resp.setStatus(401);
		resp.setHeader("WWW-Authenticate", "authenticate Crowd");
		//String reqUri = req.getRequestURI();
		String contextPath = req.getContextPath();
		// TODO correctly construct the authentication path
		resp.setHeader("Crowd-Authentication-Service", contextPath+"/repo/v1/session");
		resp.getWriter().println("The session token provided was missing, invalid or expired.");
	}

	@Override
	public void doFilter(ServletRequest servletRqst, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {
		// If token present, ask Crowd to validate and get user id
		HttpServletRequest req = (HttpServletRequest)servletRqst;
		String sessionToken = req.getHeader("sessionToken");
		String userId = null;
		if (null!=sessionToken) {
			// validate against crowd
			try {
				userId = revalidate(sessionToken);
			} catch (Exception xee) {
				reject(req, (HttpServletResponse)servletResponse);
				log.log(Level.WARNING, "invalid session token", xee);
				return;
			}
		}
		if (userId!=null) {
			// pass along, including the user id
			@SuppressWarnings("unchecked")
			Map<String,String[]> modParams = new HashMap<String,String[]>(req.getParameterMap());
			modParams.put(ServiceConstants.USER_ID_PARAM, new String[]{userId});
			HttpServletRequest modRqst = new ModParamHttpServletRequest(req, modParams);
			filterChain.doFilter(modRqst, servletResponse);
		} else if (allowAnonymous) {
			// proceed anonymously
			filterChain.doFilter(req, servletResponse);
		} else {
			reject(req, (HttpServletResponse)servletResponse);
			return;
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		@SuppressWarnings("unchecked")
        Enumeration<String> paramNames = filterConfig.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
        	String paramName = paramNames.nextElement();
        	String paramValue = filterConfig.getInitParameter(paramName);
           	if ("crowd-protocol".equalsIgnoreCase(paramName)) crowdProtocol = paramValue;
           	if ("crowd-host".equalsIgnoreCase(paramName)) crowdServer = paramValue;
           	if ("crowd-port".equalsIgnoreCase(paramName)) crowdPort = Integer.parseInt(paramValue);
           	if ("allow-anonymous".equalsIgnoreCase(paramName)) allowAnonymous = Boolean.parseBoolean(paramValue);
        }
        
        acceptAllCertificates();
  	}

	//-----------------------------------------------------------------------------------------
	// TODO The following code is cut-and-pasted from CrowdAuthUtil and should be factored
	// into a common library
	
	private static final String CLIENT = "platform";
	private static final String CLIENT_KEY = "platform-pw";

	/**
	 * @param xPath
	 * @param xml
	 * @return found string matching the xpath expression
	 * @throws XPathExpressionException
	 */
	public static String getFromXML(String xPath, byte[] xml) throws XPathExpressionException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		return xpath.evaluate(xPath, new InputSource(new ByteArrayInputStream(xml)));
	}
	
	/**
	 * @return Crowd URL prefix
	 */
	public String urlPrefix() {
		return crowdProtocol+"://"+crowdServer+":"+crowdPort+"/crowd/rest/usermanagement/latest";
	}
	
	/**
	 * @param conn
	 */
	public static void setHeaders(HttpURLConnection conn) {
		conn.setRequestProperty("Accept", "application/xml");
		conn.setRequestProperty("Content-Type", "application/xml");
		String authString=CLIENT+":"+CLIENT_KEY;
		conn.setRequestProperty("Authorization", "Basic "+Base64.encodeBytes(authString.getBytes())); 
	}
	
		
	/**
	 * @param sessionToken
	 * @return userId
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	public String revalidate(String sessionToken) throws IOException, XPathExpressionException {
		byte[] sessionXML = new byte[0];
		int rc = 0;
		URL url = new URL(urlPrefix()+"/session/"+sessionToken);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		setHeaders(conn);
		try {
			rc = conn.getResponseCode();
			InputStream is = (InputStream)conn.getContent();
			if (is!=null) sessionXML = (readInputStream(is)).getBytes();
		} catch (IOException e) {
			InputStream is = (InputStream)conn.getErrorStream();
			if (is!=null) sessionXML = (readInputStream(is)).getBytes();
			throw new RuntimeException(new String(sessionXML), e);
		}

		if (HttpStatus.OK.value()!=rc) {
			throw new RuntimeException(new String(sessionXML));
		}

		return getFromXML("/session/user/@name", sessionXML);

	}
	
	private static String readInputStream(InputStream is) throws IOException {
		StringBuffer sb = new StringBuffer();
		int i=-1;
		do {
			i = is.read();
			if (i>0) sb.append((char)i);
		} while (i>0);
		return sb.toString().trim();
	}

	/**
	 * 
	 */
	public static void acceptAllCertificates() {
		// from http://www.exampledepot.com/egs/javax.net.ssl/trustall.html
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[]{
		    new X509TrustManager() {
		        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		            return null;
		        }
		        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		        }
		        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		        }
		    }
		};

		// Install the all-trusting trust manager
		try {
		    // SSLContext sc = SSLContext.getInstance("SSL");
		    SSLContext sc = SSLContext.getInstance("TLS");
		    sc.init(null, trustAllCerts, new java.security.SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// from http://stackoverflow.com/questions/2186543/java-secure-webservice
		HostnameVerifier hv = new HostnameVerifier() {
		    public boolean verify(String urlHostName, SSLSession session) {
		        if (!urlHostName.equals(session.getPeerHost())) System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
		        return true;
		    }
		};

		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}
	

}
