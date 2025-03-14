package com.github.t1.openapi.ui.test;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static jakarta.xml.bind.annotation.XmlAccessType.FIELD;

@Path("/products")
@Produces({APPLICATION_JSON, APPLICATION_XML})
@Consumes({APPLICATION_JSON, APPLICATION_XML})
public class Products {
    @Operation(summary = "Create a new or update an existing product; the id must be unique and specified by the client")
    @APIResponse(responseCode = "200", description = "the product")
    @APIResponse(responseCode = "400", description = "invalid request, e.g. invalid id")
    @POST
    @Tag(name = "products")
    @Tag(name = "writing")
    @Produces(APPLICATION_JSON) // this is only to demo a single response type
    public Product addProduct(@Valid Product product) {
        return product;
    }

    @Operation(summary = "Get all products", description =
            "You could use many words here to describe this, but it's actually quite simple. " +
            "Currently, there are no filters, no sorting, and no pagination. " +
            "But that could come later.\n" +
            "Currently, it's just a list of all products and this text just has to be loong ;-)")
    @GET
    @Tag(name = "products")
    @Tag(name = "reading")
    @Produces(APPLICATION_JSON)
    public List<Product> products() {
        return List.of(product("1"), product("2"), product("3"));
    }

    @Operation(summary = "Get all products", description =
            "You could use many words here to describe this, but it's actually quite simple. " +
            "Currently, there are no filters, no sorting, and no pagination. " +
            "But that could come later.\n" +
            "Currently, it's just a list of all products and this text just has to be loong ;-)")
    @GET
    @Tag(name = "products")
    @Tag(name = "reading")
    @Produces(APPLICATION_XML)
    public ProductList productsXml() {return new ProductList(products());}

    @Operation(summary = "Get a product by its id")
    @APIResponse(responseCode = "200", description = "the product")
    @APIResponse(responseCode = "400", description = "invalid request, e.g. invalid id")
    @GET
    @Path("/{id}")
    @Tag(name = "products")
    @Tag(name = "reading")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Product product(
            @PathParam("id") @Parameter(description = "the id of the product")
            @Valid @NotEmpty @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "invalid id")
            String id) {
        if (id.startsWith("-")) throw new BadRequestException("invalid id: " + id);
        if ("foo".equals(id)) throw new InternalServerErrorException("don't foo me");
        if ("red".equals(id))
            throw new WebApplicationException(Response.temporaryRedirect(URI.create("products/blue")).build());
        return new Product(id,
                "Tabula Rasa #" + id,
                "A clean table",
                123_00,
                List.of(new Rating("user1", (byte) 5, "Great!"),
                        new Rating("user2", (byte) 4, "Good!"),
                        new Rating("user3", (byte) 3, "Okay!")));
    }

    @PUT
    @Path("/{id}")
    @Tag(name = "products")
    @Tag(name = "writing")
    @Operation(summary = "Store a product by its id, and if it already exists, replace it completely.")
    @Consumes({APPLICATION_JSON, APPLICATION_XML})
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Product overwriteProduct(@PathParam("id") String id, Product product) {
        assert Objects.equals(product.getId(), id);
        return product;
    }

    @DELETE
    @Path("/{id}")
    @Tag(name = "products")
    @Tag(name = "writing")
    public Product deleteProduct(@PathParam("id") String id) {
        return product(id);
    }

    /// Workaround for [issue#33865](https://github.com/quarkusio/quarkus/issues/33865)
    @XmlRootElement(name = "products")
    @XmlAccessorType(FIELD)
    public static class ProductList {
        @XmlElement(name = "product")
        List<Product> products;

        @SuppressWarnings("unused") public ProductList() {}

        public ProductList(List<Product> products) {this.products = products;}
    }
}
