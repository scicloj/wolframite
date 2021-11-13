;; (ns clojuratica.base.kernel)

;; (defn kernel [kernel-link]
;;   {:link                  kernel-link})

;; (def kernel-state (atom {:link                  nil
;;                          :latest-queue-run-time nil}))

;; (defn set-link [kernel-link]
;;   (swap! kernel-state :link kernel-link))

;; (defn set-latest-queue-run-time [lqrt]
;;   (swap! kernel-state :latest-queue-run-time lqrt))
