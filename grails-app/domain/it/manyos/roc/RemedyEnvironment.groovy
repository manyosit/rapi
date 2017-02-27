package it.manyos.roc

class RemedyEnvironment {

    String name
    String category
    Long lastFieldID = 800000000

    static hasMany = [server:Server, metrics:Metric]

    static constraints = {
        name(blank: false)
        category(inList: ["Production", "Integration", "Training", "Test", "Development"])
        lastFieldID(nullable: true)
    }

    @Override
    String toString() {
        return name
    }
}
