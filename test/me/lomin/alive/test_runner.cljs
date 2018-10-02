(ns me.lomin.alive.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [me.lomin.alive.alive-test]))

(doo-tests 'me.lomin.alive.alive-test)