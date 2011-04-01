(ns hello-cometd.core
  "A minimal hello world example how to use CometD in Clojure."
  (:use hello-cometd.jetty
        ring.middleware.file
        clojure.contrib.json)
  (:import
   (org.cometd.bayeux.server BayeuxServer ServerChannel$MessageListener
                             ConfigurableServerChannel$Initializer)
   (javax.servlet GenericServlet ServletException)
   (org.cometd.server CometdServlet AbstractService)))

(def cometd-servlet (CometdServlet.))

(defn process-message [message]
  (let [json (read-json (.getJSON message))]
    {"greeting" (str "Hello, " (get-in json [:data :name]))}))

(defn add-listener [bayeux channel-id name topic]
  (.createIfAbsent bayeux channel-id (make-array ConfigurableServerChannel$Initializer 0))
  (let [session (.newLocalSession bayeux name)]
    (.handshake session)
    (let [channel (.getChannel bayeux channel-id)
          message-listener (proxy [ServerChannel$MessageListener] []
                             (onMessage [from channel message]
                                        
                                        (let [server-session (.getServerSession session)]
                                          (if (not (= from server-session))
                                            (do  
                                              (.deliver from server-session topic
                                                        (process-message message)
                                                        nil)
                                              true)                                                
                                            true))
                                           ))]
      (.addListener channel message-listener))))

(def bayeux-initializer (proxy [GenericServlet] []
                          (init [config]
                                (proxy-super init config)
                                (let [sc (proxy-super getServletContext)
                                      b (.getAttribute sc BayeuxServer/ATTRIBUTE)]
                                  (if (nil? b)
                                    (throw (Exception. "No BayeuxServer available. Servlet start order correct?"))
                                    (add-listener b "/service/hello" "hello" "/hello")
                                    )))
                          (service [request response]
                                   (throw (ServletException.)))))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "Hello World from Ring"})

(defn wrap-app [app]
  (-> app
      (wrap-file "public")))

(def wrapped-app (wrap-app app))

(defn start-server []
  (let [servlets [{:url-pattern "/cometd/*"
                   :servlet cometd-servlet
                   :load-on-startup 1}
                  {:servlet bayeux-initializer
                   :load-on-startup 2}]]
    (run-jetty (var wrapped-app) {:port 8080 :join? false :servlets servlets})))

;(def server (start-server))
;(.stop server)
