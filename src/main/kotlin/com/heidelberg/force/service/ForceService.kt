package com.heidelberg.force.service

import com.heidelberg.force.model.GetSObjectsResponse
import com.heidelberg.force.model.ServiceLimit
import com.heidelberg.force.model.ServiceVersion
import com.heidelberg.force.model.SessionInfo
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URI
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

private const val DEFAULT_CLIENT_ID_PREFIX = "RestForce"
private const val DEFAULT_API_VERSION = "48.0"

@Service
class ForceService {
    private val logger: Logger = LoggerFactory.getLogger(ForceService::class.java)
    private var sessionInfo: SessionInfo? = null
    private val restTemplate = RestTemplate()

    fun login(username: String, password: String, securityToken: String, domain: String = "test"): SessionInfo {
        val body = renderLoginRequestXml(username, password, securityToken)
        val url = "https://$domain.salesforce.com/services/Soap/u/$DEFAULT_API_VERSION"
        logger.info("Login URL: $url")
        logger.info("User: $username")
        val headers = HttpHeaders()
        headers.contentType = MediaType.TEXT_XML
        headers["SOAPAction"] = "login"
        headers["Charset"] = "UTF-8"
        val request = RequestEntity<String>(body, headers, HttpMethod.POST, URI.create(url))
        try {
            val response = restTemplate.exchange(request, String::class.java)
            val sessionInfo = parseLoginResponseXml(responseXml = response.body!!)
            this.sessionInfo = sessionInfo
            return sessionInfo
        } catch (ex: RestClientResponseException) {
            logger.error(ex.responseBodyAsString)
            throw ex
        }
    }

    private fun renderLoginRequestXml(username: String, password: String, securityToken: String): String {
        val xml = xml("env:Envelope") {
            namespace("env", "http://schemas.xmlsoap.org/soap/envelope/")
            namespace("xsd", "http://www.w3.org/2001/XMLSchema")
            namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
            namespace("urn", "urn:partner.soap.sforce.com")
            "env:Header" {
                "urn:CallOptions" {
                    "urn:client" {
                        -DEFAULT_CLIENT_ID_PREFIX
                    }
                    "urn:defaultNamespace"  {
                        -"sf"
                    }
                }
            }
            "env:Body" {
                "n1:login" {
                    namespace("n1", "urn:partner.soap.sforce.com")
                    "n1:username" {
                        -username
                    }
                    "n1:password" {
                        -(password + securityToken)
                    }
                }
            }
        }
        return xml.toString(PrintOptions(pretty = true, singleLineTextElements = true))
    }

    private fun parseLoginResponseXml(responseXml: String): SessionInfo {
        val builder = DocumentBuilderFactory.newDefaultNSInstance().newDocumentBuilder()
        val document = builder.parse(InputSource(StringReader(responseXml)))
        var list = document.getElementsByTagName("sessionId")
        var node: Node = list.item(0)
        val sessionId = node.textContent.trim()
        list = document.getElementsByTagName("serverUrl")
        node = list.item(0)
        val serverUrl = node.textContent.trim()
        val baseUrl = URL(URL(serverUrl), "/").toString()
        return SessionInfo(sessionId = sessionId, serverUrl = baseUrl)
    }

    private fun getUrl(path: String): String = URL(URL(sessionInfo?.serverUrl), path).toString()

    private fun getHeaders(): MultiValueMap<String, String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.setBearerAuth(sessionInfo?.sessionId!!)
        return headers
    }

    // https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/dome_versions.htm
    fun getVersions(): Array<ServiceVersion>? {
        val url = getUrl("/services/data/")
        val headers = getHeaders()
        var requestEntity = RequestEntity<String>(headers, HttpMethod.GET, URI.create(url))
        return restTemplate.exchange(requestEntity, Array<ServiceVersion>::class.java).body
    }

    // https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_limits.htm
    fun getLimits(): Map<String, ServiceLimit>? {
        val url = getUrl("/services/data/v$DEFAULT_API_VERSION/limits/")
        val headers = getHeaders()
        var requestEntity = RequestEntity<String>(headers, HttpMethod.GET, URI.create(url))
        return restTemplate.exchange(requestEntity,
                object : ParameterizedTypeReference<Map<String, ServiceLimit>>() {}).body
    }

    fun getServices(): Map<String, String>? {
        val url = getUrl("/services/data/v$DEFAULT_API_VERSION/")
        val headers = getHeaders()
        var requestEntity = RequestEntity<String>(headers, HttpMethod.GET, URI.create(url))
        return restTemplate.exchange(requestEntity,
                object : ParameterizedTypeReference<Map<String, String>>() {}).body
    }

    // https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/dome_describeGlobal.htm
    fun getSObjects() : GetSObjectsResponse? {
        val url = getUrl("/services/data/v$DEFAULT_API_VERSION/sobjects/")
        val headers = getHeaders()
        var requestEntity = RequestEntity<String>(headers, HttpMethod.GET, URI.create(url))
        return restTemplate.exchange(requestEntity, GetSObjectsResponse::class.java).body
    }

}