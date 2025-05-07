package com.github.t1.openapi.ui.app;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlRootElement
public class Product {
    @Schema(examples = "123")
    private @NotEmpty @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "invalid id") String id;

    @Schema(examples = "Tabula Rasa #123")
    private @NotEmpty String name;

    // no example here
    private String description;

    @Schema(examples = "12300")
    private @PositiveOrZero int price;

    private List<Rating> ratings = new ArrayList<>();

    public Product() {} // JAXB

    public Product(String id, String name, String description, int price, List<Rating> ratings) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.ratings = ratings;
    }

    public String getId() {return id;}

    public void setId(String id) {this.id = id;}

    public String getName() {return name;}

    public void setName(String name) {this.name = name;}

    public String getDescription() {return description;}

    public void setDescription(String description) {this.description = description;}

    public int getPrice() {return price;}

    public void setPrice(int price) {this.price = price;}

    public List<Rating> getRatings() {return ratings;}

    public void setRatings(List<Rating> ratings) {this.ratings = ratings;}

    public Product addRating(Rating rating) {
        this.ratings.add(rating);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Product) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.name, that.name) &&
               Objects.equals(this.description, that.description) &&
               this.price == that.price &&
               Objects.equals(this.ratings, that.ratings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, price, ratings);
    }

    @Override
    public String toString() {
        return "Product[" +
               "id=" + id + ", " +
               "name=" + name + ", " +
               "description=" + description + ", " +
               "price=" + price + ", " +
               "ratings=" + ratings + ']';
    }
}
