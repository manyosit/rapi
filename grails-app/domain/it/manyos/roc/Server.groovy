package it.manyos.roc

class Server {
    String name
    Integer port = 0
    String username
    String password

    static belongsTo = [remedyEnvironment: RemedyEnvironment]

    static constraints = {
        name(blank: false)
        port(defaultValue: 0)
        username(blank: false)
        password(password:true, blank: false)
    }

    @Override
    String toString() {
        return remedyEnvironment.toString() + " - " + name
    }
}
