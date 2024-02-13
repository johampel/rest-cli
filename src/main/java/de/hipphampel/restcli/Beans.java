/*
 * The MIT License
 * Copyright © ${year} Johannes Hampel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.hipphampel.restcli;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.inject.Produces;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;


@RegisterForReflection(targets = {
    // For conversion Swagger 2.x -> OpenAPI 3.0.x
    io.swagger.v3.oas.models.Components.class,
    io.swagger.v3.oas.models.ExternalDocumentation.class,
    io.swagger.v3.oas.models.OpenAPI.class,
    io.swagger.v3.oas.models.Operation.class,
    io.swagger.v3.oas.models.PathItem.class,
    io.swagger.v3.oas.models.Paths.class,
    io.swagger.v3.oas.models.SpecVersion.class,
    io.swagger.v3.oas.models.callbacks.Callback.class,
    io.swagger.v3.oas.models.examples.Example.class,
    io.swagger.v3.oas.models.headers.Header.class,
    io.swagger.v3.oas.models.info.Contact.class,
    io.swagger.v3.oas.models.info.Info.class,
    io.swagger.v3.oas.models.info.License.class,
    io.swagger.v3.oas.models.links.Link.class,
    io.swagger.v3.oas.models.links.LinkParameter.class,
    io.swagger.v3.oas.models.media.ArraySchema.class,
    io.swagger.v3.oas.models.media.BinarySchema.class,
    io.swagger.v3.oas.models.media.BooleanSchema.class,
    io.swagger.v3.oas.models.media.ByteArraySchema.class,
    io.swagger.v3.oas.models.media.ComposedSchema.class,
    io.swagger.v3.oas.models.media.Content.class,
    io.swagger.v3.oas.models.media.DateSchema.class,
    io.swagger.v3.oas.models.media.DateTimeSchema.class,
    io.swagger.v3.oas.models.media.Discriminator.class,
    io.swagger.v3.oas.models.media.EmailSchema.class,
    io.swagger.v3.oas.models.media.Encoding.class,
    io.swagger.v3.oas.models.media.EncodingProperty.class,
    io.swagger.v3.oas.models.media.FileSchema.class,
    io.swagger.v3.oas.models.media.IntegerSchema.class,
    io.swagger.v3.oas.models.media.JsonSchema.class,
    io.swagger.v3.oas.models.media.MapSchema.class,
    io.swagger.v3.oas.models.media.MediaType.class,
    io.swagger.v3.oas.models.media.NumberSchema.class,
    io.swagger.v3.oas.models.media.ObjectSchema.class,
    io.swagger.v3.oas.models.media.PasswordSchema.class,
    io.swagger.v3.oas.models.media.Schema.class,
    io.swagger.v3.oas.models.media.StringSchema.class,
    io.swagger.v3.oas.models.media.UUIDSchema.class,
    io.swagger.v3.oas.models.media.XML.class,
    io.swagger.v3.oas.models.parameters.CookieParameter.class,
    io.swagger.v3.oas.models.parameters.HeaderParameter.class,
    io.swagger.v3.oas.models.parameters.Parameter.class,
    io.swagger.v3.oas.models.parameters.PathParameter.class,
    io.swagger.v3.oas.models.parameters.QueryParameter.class,
    io.swagger.v3.oas.models.parameters.RequestBody.class,
    io.swagger.v3.oas.models.responses.ApiResponse.class,
    io.swagger.v3.oas.models.responses.ApiResponses.class,
    io.swagger.v3.oas.models.security.OAuthFlow.class,
    io.swagger.v3.oas.models.security.OAuthFlows.class,
    io.swagger.v3.oas.models.security.Scopes.class,
    io.swagger.v3.oas.models.security.SecurityRequirement.class,
    io.swagger.v3.oas.models.security.SecurityScheme.class,
    io.swagger.v3.oas.models.servers.Server.class,
    io.swagger.v3.oas.models.servers.ServerVariable.class,
    io.swagger.v3.oas.models.servers.ServerVariables.class,
    io.swagger.v3.oas.models.tags.Tag.class,
})
public class Beans {

  @Produces
  public DocumentBuilderFactory documentBuilderFactory() {
    return DocumentBuilderFactory.newDefaultInstance();
  }

  @Produces
  public Transformer transformer() throws TransformerConfigurationException {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    return transformer;
  }
}
