(ns me.lomin.alive.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [pjstadig.humane-test-output]
            [me.lomin.alive.views-test]))

(doo-tests 'me.lomin.alive.views-test)
