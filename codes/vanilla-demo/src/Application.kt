package keycloak.vanilla

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.Claim
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.html.*
import org.json.simple.JSONObject

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalLocationsAPI
@Location("/") class Index

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Locations)

    if (!testing) {
        install(HttpsRedirect) {
            sslPort = 8765
            permanentRedirect = true
        }
        install(ForwardedHeaderSupport)
        install(XForwardedHeaderSupport)
    }

    val keycloakOAuth = "keycloakOAuth"
    val keycloakAddress = environment.config.property("ktor.keycloak.path").getString()
    val keycloakProvider = OAuthServerSettings.OAuth2ServerSettings(
        name = "keycloak",
        authorizeUrl = "$keycloakAddress/auth/realms/demo/protocol/openid-connect/auth",
        accessTokenUrl = "$keycloakAddress/auth/realms/demo/protocol/openid-connect/token",
        clientId = "demo",
        clientSecret = "0b663c27-7f8e-49c9-8e8a-e4d8923ed316",
        requestMethod = HttpMethod.Post,
        defaultScopes = listOf("roles")
    )

    install(Authentication) {
        oauth(keycloakOAuth) {
            client = HttpClient(Apache)
            providerLookup = { keycloakProvider }
            urlProvider = {
                redirectUrl("/")
            }
        }
    }

    routing {
        authenticate(keycloakOAuth) {
            location<Index> {
                param("error") {
                    handle {
                        call.loginFailedPage(call.parameters
                                .getAll("error").orEmpty())
                    }
                }

                handle {
                    val principal =
                        call.authentication
                            .principal<OAuthAccessTokenResponse.OAuth2>()
                    if (principal != null) {
                        call.loggedInSuccessResponse(principal)
                    } else {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
            }
        }

        install(StatusPages) {
            exception<AuthenticationException> {
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> {
                call.respond(HttpStatusCode.Forbidden)
            }

        }
    }
}

@KtorExperimentalLocationsAPI
private fun <T : Any> ApplicationCall.redirectUrl(t: T, secure: Boolean = true): String {
    val hostPort = request.host() + request.port().let { port -> if (port == 80) "" else ":$port" }
    val protocol = when {
        secure -> "https"
        else -> "http"
    }
    return "$protocol://$hostPort${application.locations.href(t)}"
}

private suspend fun ApplicationCall.loginFailedPage(errors: List<String>) {
    respondHtml {
        head {
            title { +"Login failed" }
        }
        body {
            h1 {
                +"Login error"
            }

            for (e in errors) {
                p {
                    +e
                }
            }
        }
    }
}

private suspend fun ApplicationCall.loggedInSuccessResponse(callback: OAuthAccessTokenResponse.OAuth2) {
    val jwtToken = callback.accessToken
    val token =  JWT.decode(jwtToken)
    val name = token.getClaim("preferred_username").asString()

    respondHtml {
        head {
            title { +"Logged in" }
        }
        body {
            h1 {
                + "Login successful!"
            }
            h2 {
                + "Welcome $name!"
            }

            p {
                + "Your claims are:"
            }

            ul{
                token.claims.forEach { (k,v) ->
                    li {
                        + "$k: ${v.stringValue()}"
                    }
                }
            }
        }
    }
}

private fun Claim.stringValue(): String {
    when {
        this.isNull -> {
            return "null value"
        }
        this.asBoolean() != null -> {
            return this.asBoolean().toString()
        }
        this.asDate() != null -> {
            return this.asDate().toString()
        }
        this.asString() != null -> {
            return this.asString().toString()
        }
        this.asInt() != null -> {
            return this.asInt().toString()
        }
        this.asLong() != null -> {
            return this.asLong().toString()
        }
        this.asDouble() != null -> {
            return this.asDouble().toString()
        }
        this.asString() != null -> {
            return this.asString().toString()
        }
        this.asMap() != null -> {
            return JSONObject.toJSONString(this.asMap())
        }
        else -> {
            return this.toString()
        }
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

