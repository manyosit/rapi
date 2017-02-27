package it.manyos.roc

class MetricSchedulerJob {
    def RemedyService

    static triggers = {
      simple repeatInterval: 900000l // execute job once in 5 seconds
    }

    def execute() {
        RemedyService.processMetrics()
        println "Execute Metric Job!"
    }
}
