(ns api.core
  (:require-macros
    [mern-utils.macros :refer [node-require, node-config]])
  (:require
    [polyfill.compat]
    [cljs.nodejs :as nodejs]
    [mern-utils.db :as db]
    [mern-utils.backend-lib :refer [local-ip]]
    [mern-utils.express :refer [route]]
    [mern-utils.passport.strategy :refer [config-passport]]
    [mern-utils.amqp :refer [create-amqp-conn]]
    [common.config :refer [DATABASE DB-ENDPOINT AWS-CONFIG
                           USE-RABBITMQ
                           RABBITMQ-DOMAIN RABBITMQ-PORT RABBITMQ-DEFAULT-QUEUE
                           API-DOMAIN API-PORT 
                           WWW-DOMAIN WWW-PORT 
                           config-auth cors-options]]
    [common.models :refer [user-model api-token-model facebook-account-model google-account-model]]
    [api.handlers :refer [route-table
                          homepage-handler
                          me-handler
                          login-handler]]))

(enable-console-print!)

(node-require express "express")
(node-require express-session "express-session")
(node-require passport "passport")
(node-require connect-flash "connect-flash")
(node-require morgan "morgan")
(node-require body-parser "body-parser")
(node-require cookie-parser "cookie-parser")

(def amqp-endpoint (str "amqp://" RABBITMQ-DOMAIN ":" RABBITMQ-PORT))

(defn server [success]
  (if USE-RABBITMQ
    (create-amqp-conn amqp-endpoint))
  (doto (express)
    (.use (.static express "resources/public"))
    (.use (morgan "dev"))
    (.use (cookie-parser))
    (.use (body-parser))
    (.use (express-session (clj->js {:secret "very secret" :cookie {:maxAge (* 1000 60 60)}})))
    (.use (-> passport (.initialize)))
    (.use (-> passport (.session)))
    (.use (connect-flash))
    (route @route-table cors-options)
    (.listen API-PORT API-DOMAIN success)
    ))

(defn -main [& mess]
  (db/connect DATABASE DB-ENDPOINT AWS-CONFIG)
  (config-passport passport config-auth (user-model) (api-token-model) (facebook-account-model) (google-account-model))
  (server #(println (str "Server running at http://" local-ip ":" API-PORT "/"))))

(set! *main-cli-fn* -main)
