/*
Represents a food
 */
import java.util.Map;

public class Food{
    //Make list of types of ingredients
    Map<String, Integer> ingredients;
    private float price;

    public void addIngredient(){}

    public void removeIngredient(){}

    public void setPrice(float price) {
        this.price = price;
    }

    public float getPrice() { return price; }

    //This is here so when we make an order we can give a copy of the Food
    public Food(Food another){}
}