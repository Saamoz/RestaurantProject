package Interface.Controllers;

import RestaurantModel.Interfaces.RestaurantModel;
import RestaurantModel.RestaurantObjects.Food;
import RestaurantModel.RestaurantObjects.Order;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

/*
* @startuml
* class TakeOrderController{
* -previousScene: Scene
* -menuList: ListView
* -orderList: ListView
* -ingredientList: ListView
* -tableNumberInput: TextField
* -ingredientBox: TextField
* -orderNickname: TextField
* -orderInstructions: TextField
* -customName: TextField
* -customPrice: TextField
* -customInstructions: TextFields
* -orderLabel: Label
* -order: Order
* -restaurant: RestaurantModel
* -ingredientsFiltered: FilteredList<String>
* +init(restaurant: RestaurantModel): void
* +addIngredient(): void
* +removeIngredient(): void
* +submitOrder(event: ActionEvent): void
* +cancelOrder(event: ActionEvent): void
* +removeFood(): void
* +addCustomFood(): void
* }
* @enduml
 */

public class TakeOrderController implements WorkerController {

    public Scene previousScene;

    @FXML
    private ListView menuList;
    @FXML
    private ListView orderList;
    @FXML
    private ListView ingredientList;

    @FXML
    private TextField tableNumberInput;
    @FXML
    private TextField ingredientBox;
    @FXML
    private TextField orderNickname;
    @FXML
    private TextField orderInstructions;

    @FXML
    private TextField customName;
    @FXML
    private TextField customPrice;
    @FXML
    private TextField customInstructions;

    @FXML
    private Label orderLabel;

    private Order order;
    private RestaurantModel restaurant;
    private FilteredList<String> ingredientsFiltered;

    int serverNumberPlacingOrder;

    @Override
    public void init(RestaurantModel restaurant) {
        ingredientsFiltered = new FilteredList<String>(FXCollections.observableArrayList(restaurant.getInventory().keySet()).sorted());

        order = new Order();
        order.setServerNumber(serverNumberPlacingOrder);
        this.restaurant = restaurant;

        menuList.getItems().setAll(getFixedMenu(restaurant.getMenu()));
        restaurant.getInventory().addListener((MapChangeListener<String, Integer>) change ->
                menuList.getItems().setAll(getFixedMenu(restaurant.getMenu())));

        orderList.setItems(order.getFoods());

        tableNumberInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                tableNumberInput.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        customPrice.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*$")) {
                customPrice.setText(newValue.replaceAll("[^(\\d*.?\\d*$)]", ""));
            }
        });

        orderList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null){
                ingredientList.setItems(getFixedListFromFood((Food) newValue));
            } }
        );

        order.getFoods().addListener((ListChangeListener<Food>) c -> {
            setLabelCalories();
        });

        ingredientList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue instanceof String) {
                ingredientBox.setText(((String) ingredientList.getSelectionModel().getSelectedItem()).split("x")[0].trim());
            }
        });

        ingredientBox.setOnKeyPressed(event -> {
            ingredientsFiltered.setPredicate(s -> {
                String lowerCaseSearch = ingredientBox.getText().toLowerCase();
                String extraChar = event.getText();
                String fullString;

                if (event.getCode() == KeyCode.BACK_SPACE){
                    fullString = lowerCaseSearch.substring(0, lowerCaseSearch.length() - 1);
                } else {
                    fullString = lowerCaseSearch + extraChar;;
                }
                return s.contains(fullString);
            });
            ingredientList.setItems(ingredientsFiltered);
        });
    }

    private void setLabelCalories(){
        int cals = 0;
        for (Food food : order.getFoods()){
            cals = cals + restaurant.getCalories(food);
        }
        orderLabel.setText("Order " + "(" + Integer.toString(cals) + " cals)");
    }

    @FXML
    private void addFood(){
        Food selectedFood = restaurant.getMenu().get(menuList.getSelectionModel().getSelectedItem());

        if (inventoryWillHaveEnough(selectedFood)){
            order.addFood(new Food(selectedFood));
        }
    }

    private Boolean inventoryWillHaveEnough(Food foodToAdd){
        Food testFood = new Food("test", 0);
        ObservableList<Food> allFoods = FXCollections.observableArrayList();
        allFoods.setAll(order.getFoods());
        allFoods.add(foodToAdd);
        for (Food food : allFoods){
            for (String ingredient : food.getIngredients().keySet()){
                testFood.addIngredient(ingredient, food.getIngredients().get(ingredient));
            }
        }

        return restaurant.hasEnough(testFood);
    }

    private Boolean checkIfIngredientValid(String ingredient){
        return restaurant.getInventory().containsKey(ingredient);
    }

    public void addIngredient() {
        if (orderList.getSelectionModel().isEmpty()){
            return;
        }

        Food selectedFood = (Food) orderList.getSelectionModel().getSelectedItem();

        if (checkIfIngredientValid(ingredientBox.getText())){
            String selectedIngredient = ingredientBox.getText();

            Food testFood = new Food();
            testFood.addIngredient(selectedIngredient, 1);

            if (inventoryWillHaveEnough(testFood) ){
                selectedFood.addIngredient(selectedIngredient, 1);
                setLabelCalories();
                ingredientList.setItems(getFixedListFromFood(selectedFood));
            }
        }
    }

    public void removeIngredient() {
        if (orderList.getSelectionModel().isEmpty()){
            return;
        }

        Food selectedFood = (Food) orderList.getSelectionModel().getSelectedItem();

        if (checkIfIngredientValid(ingredientBox.getText())){
            String selectedIngredient = ingredientBox.getText();

            selectedFood.removeIngredient(selectedIngredient, 1);
            setLabelCalories();
            ingredientList.setItems(getFixedListFromFood(selectedFood));
        }
    }

    private ObservableList<String> getFixedListFromFood(Food food){
        ObservableList<String> fixedIngredients = FXCollections.observableArrayList();
        for (String ingredient : food.getIngredients().keySet()){
            fixedIngredients.add(ingredient + " x " + food.getIngredients().get(ingredient));
        }

        return fixedIngredients.sorted();
    }

    private ObservableList<String> getFixedMenu(ObservableMap<String, Food> menu){
        ObservableList<String> fixedMenu = FXCollections.observableArrayList();
        for (String item : menu.keySet()){
            if (restaurant.hasEnough(menu.get(item))){
                fixedMenu.add(item);
            }
        }
        return fixedMenu.sorted();
    }


    public void submitOrder(ActionEvent event) {
        if (!tableNumberInput.getText().isEmpty() && !order.getFoods().isEmpty()){

            order.setNickname(orderNickname.getText());
            order.setInstructions(orderInstructions.getText());

            order.setTableNumber(Integer.parseInt(tableNumberInput.getText()));
            restaurant.placeOrder(order);
            ((Stage) ((Node) event.getSource()).getScene().getWindow()).setScene(previousScene);
        }
    }

    public void cancelOrder(ActionEvent event){
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).setScene(previousScene);
    }

    public void removeFood() {
        Food food = (Food) orderList.getSelectionModel().getSelectedItem();
        order.removeFood(food);
        ingredientList.getItems().clear();
    }

    public void addCustomFood(){
        String name = customName.getText().isEmpty() ? "Custom Food" : customName.getText();
        Float price = null;
        try{
            price = Float.parseFloat(customPrice.getText());
        } catch (Exception ignored){
        } finally {
            if (price != null){
                Food customFood = new Food(name, price);
                customFood.setInstructions(customInstructions.getText());
                order.addFood(customFood);

                customName.clear();
                customPrice.clear();
                customInstructions.clear();
            }
        }

    }
}
