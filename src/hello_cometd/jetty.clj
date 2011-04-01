(ns hello-cometd.jetty
  "Adapter for the Jetty webserver which is almost identical with the one in ring.adapter.jetty.
   The adapter has been updated for the use with Jetty 7 and can be configured with additional
   servlets."
  (:import (org.eclipse.jetty.server.handler AbstractHandler)
           (org.eclipse.jetty.server Server Request Response)
           (org.eclipse.jetty.server.bio SocketConnector)
           (org.eclipse.jetty.server.ssl SslSocketConnector)
           (javax.servlet.http HttpServletRequest HttpServletResponse)
           (org.eclipse.jetty.servlet ServletContextHandler ServletHolder)
           (javax.servlet GenericServlet ServletException))
  (:require [ring.util.servlet :as servlet]))

(defn- add-ssl-connector!
  "Add an SslSocketConnector to a Jetty Server instance."
  [^Server server options]
  (let [ssl-connector (SslSocketConnector.)]
    (doto ssl-connector
      (.setPort        (options :ssl-port 443))
      (.setKeystore    (options :keystore))
      (.setKeyPassword (options :key-password)))
    (when (options :truststore)
      (.setTruststore ssl-connector (options :truststore)))
    (when (options :trust-password)
      (.setTrustPassword ssl-connector (options :trust-password)))
    (.addConnector server ssl-connector)))

(defn- create-server
  "Construct a Jetty Server instance."
  [options]
  (let [connector (doto (SocketConnector.)
                    (.setPort (options :port 80))
                    (.setHost (options :host)))
        server    (doto (Server.)
                    (.addConnector connector)
                    (.setSendDateHeader true))]
    (when (or (options :ssl?) (options :ssl-port))
      (add-ssl-connector! server options))
    server))

(defn ^Server run-jetty
  "Serve the given handler according to the options.
  Options:
    :configurator   - A function called with the Server instance.
    :port
    :host
    :join?          - Block the caller: defaults to true.
    :ssl?           - Use SSL.
    :ssl-port       - SSL port: defaults to 443, implies :ssl?
    :keystore
    :key-password
    :truststore
    :trust-password
    :servlets       - Additional servlets to register in the form
                      [{:url-pattern \"/orders/*\"
                        :servlet orders-servlet
                        :load-on-startup 1}]"
  [handler options]
  (let [^Server s (create-server (dissoc options :configurator))]
    (when-let [configurator (:configurator options)]
      (configurator s))
    (let [context (ServletContextHandler. ServletContextHandler/SESSIONS)
          handler-servlet (servlet/servlet handler)]
      (do
        (.setContextPath context "/")
        (.addServlet context (ServletHolder. handler-servlet) "/*"))
      (dorun
       (map #(let [{:keys [url-pattern servlet load-on-startup]} %1
                   holder (ServletHolder. servlet)]
               (.setInitOrder holder load-on-startup)
               (.addServlet context holder url-pattern))
            (:servlets options)))
      (doto s
        (.setHandler context)
        (.start)))
    (when (:join? options true)
      (.join s))
    s))
