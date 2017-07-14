package roc

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.context.EnvironmentAware

class Application extends GrailsAutoConfiguration  {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    /*
    @Override
    void setEnvironment(Environment environment) {
        String configPath = System.properties["local.config.location"]
        Resource resourceConfig = new FileSystemResource(configPath)
        //if (resourceConfig.exists()) {
            YamlPropertiesFactoryBean ypfb = new YamlPropertiesFactoryBean()
            ypfb.setResources([resourceConfig] as Resource[])
            ypfb.afterPropertiesSet()
            Properties properties = ypfb.getObject()
            environment.propertySources.addFirst(new PropertiesPropertySource("local.config.location", properties))
        //}
    }*/

    static class BootStrap {

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
}