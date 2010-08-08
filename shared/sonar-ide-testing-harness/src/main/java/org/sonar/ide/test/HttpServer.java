package org.sonar.ide.test;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.B64Code;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper for the tests to start an embedded HTTP server powered by Jetty.
 * Create an instance of this class, use its mutators to configure the server and finally call {@link #start()}.
 */
public class HttpServer {

  private Server server;

  private int httpPort;

  private int httpsPort = -1;

  private String keyStoreLocation = "resources/ssl/keystore";

  private String keyStorePassword;

  private String trustStoreLocation;

  private String trustStorePassword;

  private boolean needClientAuth;

  private String proxyUsername;

  private String proxyPassword;

  private boolean redirectToHttps;

  private long latency;

  /**
   * Sets the port to use for HTTP connections.
   *
   * @param httpPort port to use, {@code 0} to pick a random port (default), if negative the HTTP connector will be disabled
   * @return this (for method chaining)
   */
  public HttpServer setHttpPort(int httpPort) {
    this.httpPort = httpPort;
    return this;
  }

  public int getHttpPort() {
    if (httpPort >= 0 && server != null && server.isRunning()) {
      return server.getConnectors()[0].getLocalPort();
    }
    return httpPort;
  }

  /**
   * @return base URL without trailing slash to the server's HTTP connector (e.g. {@code "http://localhost:8080"}),
   *         never {@code null}
   */
  public String getHttpUrl() {
    return "http://localhost:" + getHttpPort();
  }

  /**
   * Sets the port to use for HTTPS connections.
   *
   * @param httpsPort The port to use, {@code 0} to pick a random port, if negative the HTTPS connector will be disabled (default)
   * @return this (for method chaining)
   */
  public HttpServer setHttpsPort(int httpsPort) {
    this.httpsPort = httpsPort;
    return this;
  }

  public int getHttpsPort() {
    if (httpsPort >= 0 && server != null && server.isRunning()) {
      return server.getConnectors()[(httpPort < 0) ? 0 : 1].getLocalPort();
    }
    return httpsPort;
  }

  /**
   * Sets the keystore to use for the server certificate on the SSL connector.
   *
   * @param path     path to the keystore to use for the server certificate, may be {@code null}
   * @param password password for the keystore, may be {@code null}
   * @return this (for method chaining)
   */
  public HttpServer setKeyStore(String path, String password) {
    keyStoreLocation = path;
    keyStorePassword = password;
    return this;
  }

  /**
   * Sets the truststore to use for validating client credentials via the SSL connector.
   *
   * @param path     path to the truststore to use for the trusted client certificates, may be {@code null}
   * @param password password for the truststore, may be {@code null}
   * @return this (for method chaining)
   */
  public HttpServer setTrustStore(String path, String password) {
    trustStoreLocation = path;
    trustStorePassword = password;
    return this;
  }

  /**
   * Enables/disables client-side certificate authentication.
   *
   * @param needClientAuth Whether the server should reject clients whose certificate can't be verified via the
   *                       truststore.
   * @return This server, never {@code null}.
   */
  public HttpServer setNeedClientAuth(boolean needClientAuth) {
    this.needClientAuth = needClientAuth;
    return this;
  }

  /**
   * Sets the latency of the server.
   *
   * @param millis latency in miliseconds, negative for infinite delay
   * @return this (for method chaining)
   */
  public HttpServer setLatency(long millis) {
    this.latency = millis;
    return this;
  }

  /**
   * Sets the credentials to use for proxy authentication. If either username or password is {@code null},
   * no proxy authentication is required.
   *
   * @param username username
   * @param password password
   * @return this (for method chaining)
   */
  public HttpServer setProxyAuth(String username, String password) {
    this.proxyUsername = username;
    this.proxyPassword = password;
    return this;
  }

  /**
   * Enforces redirection from HTTP to HTTPS.
   *
   * @param redirectToHttps {@code true} to redirect any HTTP requests to HTTPS, {@code false} to handle HTTP normally
   * @return this (for method chaining)
   */
  public HttpServer setRedirectToHttps(boolean redirectToHttps) {
    this.redirectToHttps = redirectToHttps;
    return this;
  }

  protected Connector newHttpConnector() {
    SelectChannelConnector connector = new SelectChannelConnector();
    connector.setPort(httpPort);
    return connector;
  }

  protected Connector newHttpsConnector() {
    SslSocketConnector connector = new SslSocketConnector();
    connector.setPort(httpsPort);
    connector.setKeystore(new File(keyStoreLocation).getAbsolutePath());
    connector.setPassword(keyStorePassword);
    connector.setKeyPassword(keyStorePassword);
    connector.setTruststore(new File(trustStoreLocation).getAbsolutePath());
    connector.setTrustPassword(trustStorePassword);
    connector.setNeedClientAuth(needClientAuth);
    return connector;
  }

  protected Handler newSleepHandler(final long millis) {
    return new AbstractHandler() {
      public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        if (millis >= 0) {
          try {
            Thread.sleep(millis);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        } else {
          synchronized (this) {
            try {
              wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }

      @Override
      public String toString() {
        return "SleepHandler";
      }
    };
  }

  protected Handler newProxyHandler() {
    return new AbstractHandler() {
      public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        String auth = request.getHeader("Proxy-Authorization");
        if (auth != null) {
          auth = auth.substring(auth.indexOf(' ') + 1).trim();
          auth = B64Code.decode(auth);
        }
        if (!(proxyUsername + ':' + proxyPassword).equals(auth)) {
          response.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
          response.addHeader("Proxy-Authenticate", "Basic realm=\"Squid proxy-caching web server\"");
          response.getWriter().println("Proxy authentication required");
          ((Request) request).setHandled(true);
        }
      }

      @Override
      public String toString() {
        return "ProxyHandler";
      }
    };
  }

  protected Handler newSslRedirectHandler() {
    return new AbstractHandler() {
      public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) {
        int httpsPort = getHttpsPort();
        if (!((Request) request).isHandled() && request.getServerPort() != httpsPort) {
          String url = "https://" + request.getServerName() + ":" + httpsPort + request.getRequestURI();
          response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
          response.setHeader("Location", url);
          ((Request) request).setHandled(true);
        }
      }

      @Override
      public String toString() {
        return "SslRedirectHandler";
      }
    };
  }

  /**
   * Starts the server. Starting an already running server has no effect.
   *
   * @return this (for method chaining)
   * @throws Exception if server could not be started
   */
  public HttpServer start() throws Exception {
    if (server != null) {
      return this;
    }

    List<Connector> connectors = new ArrayList<Connector>();
    if (httpPort >= 0) {
      connectors.add(newHttpConnector());
    }
    if (httpsPort >= 0 && keyStoreLocation != null) {
      connectors.add(newHttpsConnector());
    }

    HandlerList handlerList = new HandlerList();
    if (latency != 0) {
      handlerList.addHandler(newSleepHandler(latency));
    }
    if (redirectToHttps) {
      handlerList.addHandler(newSslRedirectHandler());
    }
    if (proxyUsername != null && proxyPassword != null) {
      handlerList.addHandler(newProxyHandler());
    }
    handlerList.addHandler(context); // TODO
    handlerList.addHandler(new DefaultHandler());

    server = new Server(0);
    server.setSendServerVersion(false);
    server.setHandler(handlerList);
    server.setConnectors(connectors.toArray(new Connector[connectors.size()]));
    server.start();

    return this;
  }

  /**
   * Stops the server. Stopping an already stopped server has no effect.
   */
  public void stop() {
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
      server = null;
    }
  }

  // ===================================================
  // TODO Godin: reorganize following fields and methods

  private String baseDir;

  private Context context = new Context(Context.SESSIONS | Context.SECURITY);

  /**
   * @param baseDir location of sonar-data directory
   * @return this (for method chaining)
   */
  protected HttpServer setBaseDir(String baseDir) {
    this.baseDir = baseDir;
    return this;
  }

  /**
   * @return this (for method chaining)
   * @see org.mortbay.jetty.handler.ContextHandler#setContextPath(java.lang.String)
   */
  public HttpServer setContextPath(String contextPath) {
    context.setContextPath(contextPath);
    return this;
  }

  /**
   * @return this (for method chaining)
   * @see org.mortbay.jetty.servlet.Context#addServlet(java.lang.Class, java.lang.String)
   */
  public HttpServer addServlet(Class servlet, String pathSpec) {
    ServletHolder servletHolder = context.addServlet(servlet, pathSpec);
    servletHolder.setInitParameter("baseDir", baseDir);
    return this;
  }

}
