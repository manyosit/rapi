package it.manyos.roc

class MetricResult {
    Date executionDate = new Date()
    Integer runTime = 0
    String status = 'success'

    String value

    static belongsTo = [metric:Metric, server:Server]

    static constraints = {
        runTime(nullable: false)
        status(blank: false, inList: ["success", "failed"])
    }

    static mapping = {
        value type: 'text'
    }

    @Override
    String toString() {
        return '' + executionDate + ' ' + server + ' ' + metric
    }
}
