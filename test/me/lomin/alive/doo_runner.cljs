(ns me.lomin.alive.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [me.lomin.alive.alive-test]))

(doo-tests 'me.lomin.alive.alive-test)