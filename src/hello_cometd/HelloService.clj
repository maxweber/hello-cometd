(ns hello-cometd.HelloService
  (:import (org.cometd.bayeux.server BayeuxServer))
  (:gen-class
   :extends org.cometd.server.AbstractService
   :name comet.HelloService
   :post-init post-init
   :constructors {[org.cometd.bayeux.server.BayeuxServer String] [org.cometd.bayeux.server.BayeuxServer String]}
   :methods [[processHello [org.cometd.bayeux.server.ServerSession org.cometd.bayeux.Message] void]]))

(defn -post-init
  [this & args]
  (.addService this "/service/hello" "processHello")
  )

(defn -processHello [this remote message]
  (let [input (.getDataAsMap message)
        name (.get input "name")
        result {"greeting" (str "Hello, " name)}]
    (.deliver remote (.getServerSession this) "/hello" result nil)
    true))
