package keycloak.adapters.spring.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.view.RedirectView
import java.security.Principal
import javax.servlet.http.HttpServletRequest

@SpringBootApplication
class SpringAdapterDemoApplication

fun main(args: Array<String>) {
    runApplication<SpringAdapterDemoApplication>(*args)
}

@Controller
class ViewController {
    @ResponseBody
    @GetMapping(value = ["/"])
    fun index(principal: Principal?): String {
        return Index.template(principal)
    }

    @GetMapping(value = ["/login"])
    fun login(): RedirectView {
        return RedirectView("/");
    }

    @GetMapping(value = ["/logout"])
    fun logout(request: HttpServletRequest?): RedirectView {
        request?.logout()
        return RedirectView("/")
    }
}
