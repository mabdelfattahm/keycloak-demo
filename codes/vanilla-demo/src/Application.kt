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
import io.ktor.sessions.*
import io.ktor.util.*
import kotlinx.html.*
import org.json.simple.JSONObject

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalLocationsAPI
@Location("/authorization-callback") class AuthorizationCallback
@KtorExperimentalLocationsAPI
@Location("/login") class Login
@KtorExperimentalLocationsAPI
@Location("/logout") class Logout
@KtorExperimentalLocationsAPI
@Location("/") class Index

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(Sessions) {
        cookie<UserSession>("MY_SESSION", SessionStorageMemory()) {
            val secretEncryptKey = hex("657965532F412E385829672E30253173")
            val secretAuthKey = hex("8FBB4855EF323")
            cookie.extensions["SameSite"] = "lax"
            cookie.httpOnly = true
            transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretAuthKey))
        }
    }
    install(Locations)
    install(ForwardedHeaderSupport)
    install(XForwardedHeaderSupport)

    val port = environment.config.property("ktor.deployment.port").getString()
    val keycloakOAuth = "keycloakOAuth"
    val keycloakAddress = environment.config.property("ktor.keycloak.path").getString()
    val userUrl = "$keycloakAddress/auth/realms/demo/account"
    val logoutUrl = "$keycloakAddress/auth/realms/demo/protocol/openid-connect/logout"
    val keycloakProvider = OAuthServerSettings.OAuth2ServerSettings(
        name = "keycloak",
        authorizeUrl = "$keycloakAddress/auth/realms/demo/protocol/openid-connect/auth",
        accessTokenUrl = "$keycloakAddress/auth/realms/demo/protocol/openid-connect/token",
        clientId = "demo",
        clientSecret = "0b663c27-7f8e-49c9-8e8a-e4d8923ed316",
        requestMethod = HttpMethod.Post,
        defaultScopes = listOf("roles", "openid")
    )

    install(Authentication) {
        oauth(keycloakOAuth) {
            client = HttpClient(Apache)
            providerLookup = { keycloakProvider }
            urlProvider = {
                redirectUrl(port,AuthorizationCallback())
            }
        }
    }

    routing {
        authenticate(keycloakOAuth) {

            location<Login> {
                handle {
                    call.respondRedirect("/")
                }
            }

            location<AuthorizationCallback> {
                handle {
                    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                    if (principal != null) {
                        val idTokenString = principal.extraParameters["id_token"] ?: throw Exception("id_token wasn't returned")
                        val idToken = JWT.decode(idTokenString)
                        val fullName = idToken.claims["preferred_username"]?.asString() ?: "Unknown name"
                        val session = UserSession(fullName, idTokenString)
                        call.sessions.set(session)
                        call.respondRedirect(call.redirectUrl(port, Index()))
                    } else {
                        call.respondHtml {
                            head {
                                title { +"Login failed" }
                            }
                            body {
                                h1 {
                                    +"Login error"
                                }
                                call.parameters.getAll("error")?.forEach {
                                    p {
                                        + it
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        location<Logout> {
            handle {
                val token = call.sessions.get<UserSession>()?.token
                call.sessions.clear<UserSession>()
                val redirectLogout = when (token) {
                    null -> "/"
                    else -> URLBuilder(logoutUrl).run {
                        parameters.append("post_logout_redirect_uri", call.redirectUrl(port, Index()))
                        parameters.append("id_token_hint", token)
                        buildString()
                    }
                }
                call.respondRedirect(redirectLogout)
            }
        }

        location<Index> {
            handle {
                val session = call.sessions.get<UserSession>()
                if (session?.username == null) {
                    call.respondHtml {
                        head {
                            title { +"Not Logged in" }
                        }
                        body {
                            h1 {
                                + "No User is Logged In"
                            }
                            h2 {
                                + "Please Login!"
                            }
                            a("/login", ) {
                                + "Login"
                            }
                        }
                    }
                } else {
                    val decoded =  JWT.decode(session.token)
                    val name = decoded.getClaim("preferred_username").asString()
                    call.respondHtml {
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
                            a(userUrl) {
                                + "User portal"
                            }
                            p {
                                + "Your claims are:"
                            }
                            ul{
                                decoded.claims.forEach { (k,v) ->
                                    li {
                                        + "$k: ${v.stringValue()}"
                                    }
                                }
                            }
                            a("logout") {
                                + "Logout"
                            }
                        }
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
private fun <T : Any> ApplicationCall.redirectUrl(p: String, t: T, secure: Boolean = true): String {
    val protocol = request.header("X-Forwarded-Proto") ?: "http"
    val host = request.header("X-Forwarded-Host") ?: request.host()
    val port = request.header("X-Forwarded-Port") ?: p
    return "$protocol://$host:$port${application.locations.href(t)}"
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

