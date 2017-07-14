import com.bmc.rsso.agent.*
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered
// Place your Spring DSL code here
beans = {
    /*def activate = grailsApplication.config.getProperty('authentication.rsso-enabled')

    log.debug("RSSO activate status = " +activate)

    if (activate) {
        myFilter(FilterRegistrationBean) {
            filter = new com.bmc.rsso.agent.RSSOFilter()
            order = Ordered.HIGHEST_PRECEDENCE
            urlPatterns = ["/*"]
        }
    }*/
}
