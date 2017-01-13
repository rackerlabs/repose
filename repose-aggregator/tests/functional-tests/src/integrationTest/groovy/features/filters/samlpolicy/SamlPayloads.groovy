/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */

package features.filters.samlpolicy

import org.openrepose.commons.utils.http.CommonHttpHeader

/**
 * WARNING! The white space in the these payloads matters. Do not re-format them.
 */
class SamlPayloads {
    static final String CONTENT_TYPE = CommonHttpHeader.CONTENT_TYPE.toString()
    static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded"
    static final String CONTENT_TYPE_XML = "application/xml"
    static final String CONTENT_TYPE_INVALID = "application/potato"

    static final String PARAM_SAML_RESPONSE = "SAMLResponse"
    static final String PARAM_RELAY_STATE = "RelayState"
    static final String PARAM_EXTRANEOUS = "Banana"

    static final String HTTP_POST = "POST"
    static final List<String> HTTP_UNSUPPORTED_METHODS = ["GET", "OPTIONS", "PUT", "PATCH", "DELETE", "CONNECT", "TRACE"]

    static final String SAML_ONE_ASSERTION_SIGNED = """\
<saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" xmlns:xs="http://www.w3.org/2001/XMLSchema" ID="_7fcd6173-e6e0-45a4-a2fd-74a4ef85bf30" IssueInstant="2015-12-04T15:47:15.057Z" Version="2.0">
    <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">http://idp.external.com</saml2:Issuer>
    <saml2p:Status>
        <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
    </saml2p:Status>
    <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" xmlns:xs="http://www.w3.org/2001/XMLSchema" ID="pfx5861722e-892e-7f5c-475d-e2b5f84bb11c" IssueInstant="2013-11-15T16:19:06.310Z" Version="2.0">
        <saml2:Issuer>http://idp.external.com</saml2:Issuer><ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
  <ds:SignedInfo><ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
    <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
  <ds:Reference URI="#pfx5861722e-892e-7f5c-475d-e2b5f84bb11c"><ds:Transforms><ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/><ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/></ds:Transforms><ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/><ds:DigestValue>SFwS5r5WzM77rBEYtisnkLvh3U4=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>nJEiom08C2ioT10FDvj0KwgW4vdO2eadGKbHWd8yDvOcYPKpTde+r9rGNc2wMFO31BuVLlY3zopBYOXV1+XYvcG7LPHZbPv3I5jnUaWNFq4xg4V5Bs1SDUr1YYcUHczyoCI6E8lvUu9DhoLP8xd5wYCJ3nrgWH8jRVd2GlNZqiFUc9Qtq8AvHe4qNdLjclt8xDH82B2Mk6+QZqknpwICpPnLcbYsh4tfpGYQ5Tx1xkfkQzIWqdThsEGZ4dJoPd22liCMlAgHfUBeNwaJccNSw8kEQOJf9fo4i+L9HMhriT8aFZx/jG6lGIS5vh4wP+wsJDEPHZIyW+GGoWpfNHlwvw==</ds:SignatureValue>
<ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIID1zCCAr+gAwIBAgIJANXRE4AvFkE/MA0GCSqGSIb3DQEBCwUAMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wIBcNMTcwMTEyMDA1MjA0WhgPMjExNjEyMTkwMDUyMDRaMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCyVdLk8tyB7oPgfs5BWnttcB4QDfdKAIUvK67temK2HVlX7DQj4SHmP0Xgs45l/MwVcdI+yyqxf2kuPIrGgQ7TfsdE9b/ATePjsS8FhBYCFI0v+HmV0x7tDwwQchYPKmNVwpNx9otqC/0pRjemOhtZuhmTe/V31TGWH/Pq5+89pIYbiT4TqV0RTuN15RbJ/rHfGiCyQSH85CW4308f+qiHqnoD4S4q4xAZvZZEeJ/04a16WIoSOLI1/X63lHJ82VDh3POiuZVQYyyqC7EWcYmrNJzVvJ17GSRJR48oUiwijQUYSiX7l98XKAJfTnmuLy3J/xdvGGlOIyLdksJnE5UbAgMBAAGjUDBOMB0GA1UdDgQWBBRxOHOh+cErc+V0fu71BjZNw4FalTAfBgNVHSMEGDAWgBRxOHOh+cErc+V0fu71BjZNw4FalTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQCP3v1/CmsaTLS4HKnGy+rURLC5hMApMIs9CERGfYfrRsC2WR1aRCGgORfPRi5+laxFxhqcK6XtW/kkipWsHLsY1beGtjji3ag6zxtCmjK/8Oi4q1c+LQx0Kf/6gie6wPI7bBYxuLgIrp6hG9wWhQWsx42ra6NLHTJXO5TxnN2RT0dbaD24d6OWY0yxB9wKwyLhND7Basrm34A1UYdlEy5mce9KywneFux67Fe0Rksfq4BAWfRW49dIYY+kVHfHqf95aSQtEpqkmMr15yVDexpixo658oRd+XebSGlPn/1y5pe7gytj/g9OvBdkVCw67MtADjpvaVW9lDnpU4v6nCnn</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature>
        <saml2:Subject>
            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">john.doe</saml2:NameID>
            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                <saml2:SubjectConfirmationData NotOnOrAfter="2113-11-17T16:19:06.298Z"/>
            </saml2:SubjectConfirmation>
        </saml2:Subject>
        <saml2:AuthnStatement AuthnInstant="2113-11-15T16:19:04.055Z">
            <saml2:AuthnContext>
                <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
            </saml2:AuthnContextClassRef>
            </saml2:AuthnContext>
        </saml2:AuthnStatement>
        <saml2:AttributeStatement>
            <saml2:Attribute Name="roles">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">nova:admin</saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="domain">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">323676</saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="email">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">no-reply@external.com</saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="FirstName">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">John</saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="LastName">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">Doe</saml2:AttributeValue>
            </saml2:Attribute>
        </saml2:AttributeStatement>
    </saml2:Assertion>
</saml2p:Response>"""

    /**
     * This is an externally base64 encoded version of the previous variable. This is the only payload planned for
     * external base64 encoding to ensure we're not fooling ourselves by potentially using the same base64 encoder and
     * decoder in our tests and implementation that may not work the way we expect it to.
     */
    static final String SAML_ONE_ASSERTION_SIGNED_BASE64 = """\
PHNhbWwycDpSZXNwb25zZSB4bWxuczpzYW1sMnA9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpwcm90b2NvbCIgeG1sbnM6
eHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hIiBJRD0iXzdmY2Q2MTczLWU2ZTAtNDVhNC1hMmZkLTc0YTRlZjg1
YmYzMCIgSXNzdWVJbnN0YW50PSIyMDE1LTEyLTA0VDE1OjQ3OjE1LjA1N1oiIFZlcnNpb249IjIuMCI+DQogICAgPHNhbWwyOklz
c3VlciB4bWxuczpzYW1sMj0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFzc2VydGlvbiI+aHR0cDovL2lkcC5leHRlcm5h
bC5jb208L3NhbWwyOklzc3Vlcj4NCiAgICA8c2FtbDJwOlN0YXR1cz4NCiAgICAgICAgPHNhbWwycDpTdGF0dXNDb2RlIFZhbHVl
PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6c3RhdHVzOlN1Y2Nlc3MiLz4NCiAgICA8L3NhbWwycDpTdGF0dXM+DQogICAg
PHNhbWwyOkFzc2VydGlvbiB4bWxuczpzYW1sMj0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFzc2VydGlvbiIgeG1sbnM6
eHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hIiBJRD0icGZ4NTg2MTcyMmUtODkyZS03ZjVjLTQ3NWQtZTJiNWY4
NGJiMTFjIiBJc3N1ZUluc3RhbnQ9IjIwMTMtMTEtMTVUMTY6MTk6MDYuMzEwWiIgVmVyc2lvbj0iMi4wIj4NCiAgICAgICAgPHNh
bWwyOklzc3Vlcj5odHRwOi8vaWRwLmV4dGVybmFsLmNvbTwvc2FtbDI6SXNzdWVyPjxkczpTaWduYXR1cmUgeG1sbnM6ZHM9Imh0
dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyMiPg0KICA8ZHM6U2lnbmVkSW5mbz48ZHM6Q2Fub25pY2FsaXphdGlvbk1l
dGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPg0KICAgIDxkczpTaWduYXR1
cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTEiLz4NCiAgPGRzOlJl
ZmVyZW5jZSBVUkk9IiNwZng1ODYxNzIyZS04OTJlLTdmNWMtNDc1ZC1lMmI1Zjg0YmIxMWMiPjxkczpUcmFuc2Zvcm1zPjxkczpU
cmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjZW52ZWxvcGVkLXNpZ25hdHVyZSIv
PjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48L2RzOlRy
YW5zZm9ybXM+PGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNzaGEx
Ii8+PGRzOkRpZ2VzdFZhbHVlPlNGd1M1cjVXek03N3JCRVl0aXNua0x2aDNVND08L2RzOkRpZ2VzdFZhbHVlPjwvZHM6UmVmZXJl
bmNlPjwvZHM6U2lnbmVkSW5mbz48ZHM6U2lnbmF0dXJlVmFsdWU+bkpFaW9tMDhDMmlvVDEwRkR2ajBLd2dXNHZkTzJlYWRHS2JI
V2Q4eUR2T2NZUEtwVGRlK3I5ckdOYzJ3TUZPMzFCdVZMbFkzem9wQllPWFYxK1hZdmNHN0xQSFpiUHYzSTVqblVhV05GcTR4ZzRW
NUJzMVNEVXIxWVljVUhjenlvQ0k2RThsdlV1OURob0xQOHhkNXdZQ0ozbnJnV0g4alJWZDJHbE5acWlGVWM5UXRxOEF2SGU0cU5k
TGpjbHQ4eERIODJCMk1rNitRWnFrbnB3SUNwUG5MY2JZc2g0dGZwR1lRNVR4MXhrZmtReklXcWRUaHNFR1o0ZEpvUGQyMmxpQ01s
QWdIZlVCZU53YUpjY05TdzhrRVFPSmY5Zm80aStMOUhNaHJpVDhhRlp4L2pHNmxHSVM1dmg0d1Ard3NKREVQSFpJeVcrR0dvV3Bm
Tkhsd3Z3PT08L2RzOlNpZ25hdHVyZVZhbHVlPg0KPGRzOktleUluZm8+PGRzOlg1MDlEYXRhPjxkczpYNTA5Q2VydGlmaWNhdGU+
TUlJRDF6Q0NBcitnQXdJQkFnSUpBTlhSRTRBdkZrRS9NQTBHQ1NxR1NJYjNEUUVCQ3dVQU1JR0FNUXN3Q1FZRFZRUUdFd0pWVXpF
T01Bd0dBMVVFQ0F3RlZHVjRZWE14RkRBU0JnTlZCQWNNQzFOaGJpQkJiblJ2Ym1sdk1Sa3dGd1lEVlFRS0RCQkZlSFJsY201aGJD
QkRiMjF3WVc1NU1SVXdFd1lEVlFRTERBeEZlSFJsY201aGJDQlBjbWN4R1RBWEJnTlZCQU1NRUdsa2NDNWxlSFJsY201aGJDNWpi
MjB3SUJjTk1UY3dNVEV5TURBMU1qQTBXaGdQTWpFeE5qRXlNVGt3TURVeU1EUmFNSUdBTVFzd0NRWURWUVFHRXdKVlV6RU9NQXdH
QTFVRUNBd0ZWR1Y0WVhNeEZEQVNCZ05WQkFjTUMxTmhiaUJCYm5SdmJtbHZNUmt3RndZRFZRUUtEQkJGZUhSbGNtNWhiQ0JEYjIx
d1lXNTVNUlV3RXdZRFZRUUxEQXhGZUhSbGNtNWhiQ0JQY21jeEdUQVhCZ05WQkFNTUVHbGtjQzVsZUhSbGNtNWhiQzVqYjIwd2dn
RWlNQTBHQ1NxR1NJYjNEUUVCQVFVQUE0SUJEd0F3Z2dFS0FvSUJBUUN5VmRMazh0eUI3b1BnZnM1QldudHRjQjRRRGZkS0FJVXZL
Njd0ZW1LMkhWbFg3RFFqNFNIbVAwWGdzNDVsL013VmNkSSt5eXF4ZjJrdVBJckdnUTdUZnNkRTliL0FUZVBqc1M4RmhCWUNGSTB2
K0htVjB4N3REd3dRY2hZUEttTlZ3cE54OW90cUMvMHBSamVtT2h0WnVobVRlL1YzMVRHV0gvUHE1Kzg5cElZYmlUNFRxVjBSVHVO
MTVSYkovckhmR2lDeVFTSDg1Q1c0MzA4ZitxaUhxbm9ENFM0cTR4QVp2WlpFZUovMDRhMTZXSW9TT0xJMS9YNjNsSEo4MlZEaDNQ
T2l1WlZRWXl5cUM3RVdjWW1yTkp6VnZKMTdHU1JKUjQ4b1Vpd2lqUVVZU2lYN2w5OFhLQUpmVG5tdUx5M0oveGR2R0dsT0l5TGRr
c0puRTVVYkFnTUJBQUdqVURCT01CMEdBMVVkRGdRV0JCUnhPSE9oK2NFcmMrVjBmdTcxQmpaTnc0RmFsVEFmQmdOVkhTTUVHREFX
Z0JSeE9IT2grY0VyYytWMGZ1NzFCalpOdzRGYWxUQU1CZ05WSFJNRUJUQURBUUgvTUEwR0NTcUdTSWIzRFFFQkN3VUFBNElCQVFD
UDN2MS9DbXNhVExTNEhLbkd5K3JVUkxDNWhNQXBNSXM5Q0VSR2ZZZnJSc0MyV1IxYVJDR2dPUmZQUmk1K2xheEZ4aHFjSzZYdFcv
a2tpcFdzSExzWTFiZUd0amppM2FnNnp4dENtaksvOE9pNHExYytMUXgwS2YvNmdpZTZ3UEk3YkJZeHVMZ0lycDZoRzl3V2hRV3N4
NDJyYTZOTEhUSlhPNVR4bk4yUlQwZGJhRDI0ZDZPV1kweXhCOXdLd3lMaE5EN0Jhc3JtMzRBMVVZZGxFeTVtY2U5S3l3bmVGdXg2
N0ZlMFJrc2ZxNEJBV2ZSVzQ5ZElZWStrVkhmSHFmOTVhU1F0RXBxa21NcjE1eVZEZXhwaXhvNjU4b1JkK1hlYlNHbFBuLzF5NXBl
N2d5dGovZzlPdkJka1ZDdzY3TXRBRGpwdmFWVzlsRG5wVTR2Nm5Dbm48L2RzOlg1MDlDZXJ0aWZpY2F0ZT48L2RzOlg1MDlEYXRh
PjwvZHM6S2V5SW5mbz48L2RzOlNpZ25hdHVyZT4NCiAgICAgICAgPHNhbWwyOlN1YmplY3Q+DQogICAgICAgICAgICA8c2FtbDI6
TmFtZUlEIEZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6MS4xOm5hbWVpZC1mb3JtYXQ6dW5zcGVjaWZpZWQiPmpvaG4u
ZG9lPC9zYW1sMjpOYW1lSUQ+DQogICAgICAgICAgICA8c2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbiBNZXRob2Q9InVybjpvYXNp
czpuYW1lczp0YzpTQU1MOjIuMDpjbTpiZWFyZXIiPg0KICAgICAgICAgICAgICAgIDxzYW1sMjpTdWJqZWN0Q29uZmlybWF0aW9u
RGF0YSBOb3RPbk9yQWZ0ZXI9IjIxMTMtMTEtMTdUMTY6MTk6MDYuMjk4WiIvPg0KICAgICAgICAgICAgPC9zYW1sMjpTdWJqZWN0
Q29uZmlybWF0aW9uPg0KICAgICAgICA8L3NhbWwyOlN1YmplY3Q+DQogICAgICAgIDxzYW1sMjpBdXRoblN0YXRlbWVudCBBdXRo
bkluc3RhbnQ9IjIxMTMtMTEtMTVUMTY6MTk6MDQuMDU1WiI+DQogICAgICAgICAgICA8c2FtbDI6QXV0aG5Db250ZXh0Pg0KICAg
ICAgICAgICAgICAgIDxzYW1sMjpBdXRobkNvbnRleHRDbGFzc1JlZj51cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YWM6Y2xh
c3NlczpQYXNzd29yZFByb3RlY3RlZFRyYW5zcG9ydA0KICAgICAgICAgICAgPC9zYW1sMjpBdXRobkNvbnRleHRDbGFzc1JlZj4N
CiAgICAgICAgICAgIDwvc2FtbDI6QXV0aG5Db250ZXh0Pg0KICAgICAgICA8L3NhbWwyOkF1dGhuU3RhdGVtZW50Pg0KICAgICAg
ICA8c2FtbDI6QXR0cmlidXRlU3RhdGVtZW50Pg0KICAgICAgICAgICAgPHNhbWwyOkF0dHJpYnV0ZSBOYW1lPSJyb2xlcyI+DQog
ICAgICAgICAgICAgICAgPHNhbWwyOkF0dHJpYnV0ZVZhbHVlIHhtbG5zOnhzaT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxT
Y2hlbWEtaW5zdGFuY2UiIHhzaTp0eXBlPSJ4czpzdHJpbmciPm5vdmE6YWRtaW48L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPg0KICAg
ICAgICAgICAgPC9zYW1sMjpBdHRyaWJ1dGU+DQogICAgICAgICAgICA8c2FtbDI6QXR0cmlidXRlIE5hbWU9ImRvbWFpbiI+DQog
ICAgICAgICAgICAgICAgPHNhbWwyOkF0dHJpYnV0ZVZhbHVlIHhtbG5zOnhzaT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxT
Y2hlbWEtaW5zdGFuY2UiIHhzaTp0eXBlPSJ4czpzdHJpbmciPjMyMzY3Njwvc2FtbDI6QXR0cmlidXRlVmFsdWU+DQogICAgICAg
ICAgICA8L3NhbWwyOkF0dHJpYnV0ZT4NCiAgICAgICAgICAgIDxzYW1sMjpBdHRyaWJ1dGUgTmFtZT0iZW1haWwiPg0KICAgICAg
ICAgICAgICAgIDxzYW1sMjpBdHRyaWJ1dGVWYWx1ZSB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1h
LWluc3RhbmNlIiB4c2k6dHlwZT0ieHM6c3RyaW5nIj5uby1yZXBseUBleHRlcm5hbC5jb208L3NhbWwyOkF0dHJpYnV0ZVZhbHVl
Pg0KICAgICAgICAgICAgPC9zYW1sMjpBdHRyaWJ1dGU+DQogICAgICAgICAgICA8c2FtbDI6QXR0cmlidXRlIE5hbWU9IkZpcnN0
TmFtZSI+DQogICAgICAgICAgICAgICAgPHNhbWwyOkF0dHJpYnV0ZVZhbHVlIHhtbG5zOnhzaT0iaHR0cDovL3d3dy53My5vcmcv
MjAwMS9YTUxTY2hlbWEtaW5zdGFuY2UiIHhzaTp0eXBlPSJ4czpzdHJpbmciPkpvaG48L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPg0K
ICAgICAgICAgICAgPC9zYW1sMjpBdHRyaWJ1dGU+DQogICAgICAgICAgICA8c2FtbDI6QXR0cmlidXRlIE5hbWU9Ikxhc3ROYW1l
Ij4NCiAgICAgICAgICAgICAgICA8c2FtbDI6QXR0cmlidXRlVmFsdWUgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAx
L1hNTFNjaGVtYS1pbnN0YW5jZSIgeHNpOnR5cGU9InhzOnN0cmluZyI+RG9lPC9zYW1sMjpBdHRyaWJ1dGVWYWx1ZT4NCiAgICAg
ICAgICAgIDwvc2FtbDI6QXR0cmlidXRlPg0KICAgICAgICA8L3NhbWwyOkF0dHJpYnV0ZVN0YXRlbWVudD4NCiAgICA8L3NhbWwy
OkFzc2VydGlvbj4NCjwvc2FtbDJwOlJlc3BvbnNlPg==""".replace("\n", "")

    static final String SAML_ASSERTION_SIGNED = """\
<saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" xmlns:xs="http://www.w3.org/2001/XMLSchema" ID="pfx5861722e-892e-7f5c-475d-e2b5f84bb11c" IssueInstant="2013-11-15T16:19:06.310Z" Version="2.0">
        <saml2:Issuer>http://idp.external.com</saml2:Issuer><ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
  <ds:SignedInfo><ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
    <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
  <ds:Reference URI="#pfx5861722e-892e-7f5c-475d-e2b5f84bb11c"><ds:Transforms><ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/><ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/></ds:Transforms><ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/><ds:DigestValue>SFwS5r5WzM77rBEYtisnkLvh3U4=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>nJEiom08C2ioT10FDvj0KwgW4vdO2eadGKbHWd8yDvOcYPKpTde+r9rGNc2wMFO31BuVLlY3zopBYOXV1+XYvcG7LPHZbPv3I5jnUaWNFq4xg4V5Bs1SDUr1YYcUHczyoCI6E8lvUu9DhoLP8xd5wYCJ3nrgWH8jRVd2GlNZqiFUc9Qtq8AvHe4qNdLjclt8xDH82B2Mk6+QZqknpwICpPnLcbYsh4tfpGYQ5Tx1xkfkQzIWqdThsEGZ4dJoPd22liCMlAgHfUBeNwaJccNSw8kEQOJf9fo4i+L9HMhriT8aFZx/jG6lGIS5vh4wP+wsJDEPHZIyW+GGoWpfNHlwvw==</ds:SignatureValue>
<ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIID1zCCAr+gAwIBAgIJANXRE4AvFkE/MA0GCSqGSIb3DQEBCwUAMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wIBcNMTcwMTEyMDA1MjA0WhgPMjExNjEyMTkwMDUyMDRaMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCyVdLk8tyB7oPgfs5BWnttcB4QDfdKAIUvK67temK2HVlX7DQj4SHmP0Xgs45l/MwVcdI+yyqxf2kuPIrGgQ7TfsdE9b/ATePjsS8FhBYCFI0v+HmV0x7tDwwQchYPKmNVwpNx9otqC/0pRjemOhtZuhmTe/V31TGWH/Pq5+89pIYbiT4TqV0RTuN15RbJ/rHfGiCyQSH85CW4308f+qiHqnoD4S4q4xAZvZZEeJ/04a16WIoSOLI1/X63lHJ82VDh3POiuZVQYyyqC7EWcYmrNJzVvJ17GSRJR48oUiwijQUYSiX7l98XKAJfTnmuLy3J/xdvGGlOIyLdksJnE5UbAgMBAAGjUDBOMB0GA1UdDgQWBBRxOHOh+cErc+V0fu71BjZNw4FalTAfBgNVHSMEGDAWgBRxOHOh+cErc+V0fu71BjZNw4FalTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQCP3v1/CmsaTLS4HKnGy+rURLC5hMApMIs9CERGfYfrRsC2WR1aRCGgORfPRi5+laxFxhqcK6XtW/kkipWsHLsY1beGtjji3ag6zxtCmjK/8Oi4q1c+LQx0Kf/6gie6wPI7bBYxuLgIrp6hG9wWhQWsx42ra6NLHTJXO5TxnN2RT0dbaD24d6OWY0yxB9wKwyLhND7Basrm34A1UYdlEy5mce9KywneFux67Fe0Rksfq4BAWfRW49dIYY+kVHfHqf95aSQtEpqkmMr15yVDexpixo658oRd+XebSGlPn/1y5pe7gytj/g9OvBdkVCw67MtADjpvaVW9lDnpU4v6nCnn</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature>
        <saml2:Subject>
            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">john.doe</saml2:NameID>
            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                <saml2:SubjectConfirmationData NotOnOrAfter="2113-11-17T16:19:06.298Z"/>
            </saml2:SubjectConfirmation>
        </saml2:Subject>
        <saml2:AuthnStatement AuthnInstant="2113-11-15T16:19:04.055Z">
            <saml2:AuthnContext>
                <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
            </saml2:AuthnContextClassRef>
            </saml2:AuthnContext>
        </saml2:AuthnStatement>
        <saml2:AttributeStatement>
            <saml2:Attribute Name="roles">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">nova:admin</saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="domain">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">323676</saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="email">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">no-reply@external.com</saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="FirstName">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">John</saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="LastName">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">Doe</saml2:AttributeValue>
            </saml2:Attribute>
        </saml2:AttributeStatement>
    </saml2:Assertion>"""
}
