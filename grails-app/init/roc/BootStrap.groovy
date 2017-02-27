package roc

import it.manyos.roc.Metric
import it.manyos.roc.RemedyEnvironment
import it.manyos.roc.Server

class BootStrap {

    def init = { servletContext ->
        /*def prod = new RemedyEnvironment(name:'production', category: 'Production').save(failOnError: true)
        def arserver = new Server(name:'54.201.167.131', port:46200, username:'admin', password: 'Yaq12wsx#', remedyEnvironment: prod).save(failOnError: true)
        def metric = new Metric(name:'active user', category: 'Count', form:'User', query:'1=1')
                .addToRemedyEnvironments(prod)
                .save(failOnError:true)*/
    }
    def destroy = {
    }
}
