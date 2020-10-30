package keycloak.adapters.spring.demo

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.keycloak.KeycloakPrincipal
import java.security.Principal

class Index {
    companion object {
        fun template(principal: Principal?): String {
            return (principal as? KeycloakPrincipal<*>)?.let {
                val idToken = it.keycloakSecurityContext.idToken
                createHTML().html {
                    head {
                        title { +"Logged in" }
                    }
                    body {
                        h1 {
                            + "Login successful!"
                        }
                        h2 {
                            + "Welcome ${idToken.preferredUsername}!"
                        }
                        a("${idToken.issuer}/account") {
                            + "User portal"
                        }
                        p {
                            + "Your claims are:"
                        }
                        ul{
                            li {
                                + "at_hash: ${idToken.accessTokenHash}"
                            }
                            li {
                                + "sub: ${idToken.subject}"
                            }
                            li {
                                + "email_verified: ${idToken.emailVerified}"
                            }
                            li {
                                + "iss: ${idToken.issuer}"
                            }
                            li {
                                + "typ: ${idToken.type}"
                            }
                            li {
                                + "preferred_username: ${idToken.preferredUsername}"
                            }
                            li {
                                + "aud: ${idToken.audience.joinToString(", ") }"
                            }
                            li {
                                + "acr: ${idToken.acr}"
                            }
                            li {
                                + "auth_time: ${idToken.auth_time}"
                            }
                            li {
                                + "exp: ${idToken.exp}"
                            }
                            li {
                                + "session_state: ${idToken.sessionState}"
                            }
                            li {
                                + "iat: ${idToken.iat}"
                            }
                            idToken.otherClaims.forEach { (k,v) ->
                                li {
                                    + "$k: $v"
                                }
                            }
                        }
                        a("logout") {
                            + "Logout"
                        }
                    }
                }
            } ?: createHTML().html {
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
                    a("/login") {
                        + "Login"
                    }
                }
            }
        }
    }
}