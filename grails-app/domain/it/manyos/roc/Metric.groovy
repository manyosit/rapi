package it.manyos.roc

class Metric {

    String name
    String category
    String form
    String query

    static hasMany = [remedyEnvironments: RemedyEnvironment, results:MetricResult]
    static belongsTo = RemedyEnvironment

    static constraints = {
        name(blank: false)
        form(blank: false)
        query(blank: false)
        category(blank: false, inList: ["Response Time", "Count", "Value", "License"])
    }

    @Override
    String toString() {
        return name
    }
}
