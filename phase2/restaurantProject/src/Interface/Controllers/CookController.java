package Interface.Controllers;

import RestaurantModel.Interfaces.RestaurantModel;
import RestaurantModel.RestaurantObjects.Food;
import RestaurantModel.RestaurantObjects.Order;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

/*
* @startuml
* class CookController{
* -receiveShipment: Button
* -prevLabel: Label
* -postLabel: Label
* -infoLabel: Label
* -prevOrderButton: Button
* -postOrderCook: Button
* -prevOrderList: ListView
* -postOrderList: ListView
* -localToCook: ObservableList<Order>
* -restaurant: RestaurantModel
* +init(restaurant: RestaurantModel): void
* +initialize(): void
* +receiveShipment(event: ActionEvent): void
* +cancelOrder(): void
* }
* @enduml
*/

public class CookController implements WorkerController {

    @FXML
    private Label prevLabel;
    @FXML
    private Label postLabel;
    @FXML
    private Label infoLabel;

    @FXML
    private Button prevOrderButton;
    @FXML
    private Button postOrderCook;
    @FXML
    private Button postOrderButton;
    @FXML
    private Button postOrderCancel;
    @FXML
    private Button receiveShipment;

    @FXML
    private ListView prevOrderList;
    @FXML
    private ListView postOrderList;

    @FXML
    private GridPane subGrid;

    @FXML
    private HBox postOrderCancelBox;

    private ObservableList<Order> localToCook;

    private int cookID;
    private static int totalCooks = 1;

    private RestaurantModel restaurant;

    public  void init(RestaurantModel restaurant){
        this.restaurant = restaurant;
        prevOrderList.setItems(restaurant.getOrdersAtStage("Pending"));
        postOrderList.setItems(localToCook);

        restaurant.getOrdersAtStage("Cooked").addListener((ListChangeListener) c -> {
            for (Object order : restaurant.getOrdersAtStage("InProgress")){
                if (((Order) order).getCookNumber() == cookID){
                    localToCook.add(((Order) order));
                }
            }
        });
    }

    public void initialize() {
        postOrderButton.setVisible(false);
        postOrderCancelBox.setVisible(true);

        cookID = totalCooks;
        totalCooks += 1;

        localToCook = FXCollections.observableArrayList();
        GridPane currentGrid = ((GridPane) prevOrderList.getParent());

        Label infoTitle = new Label();
        currentGrid.add(infoTitle, 0, 0);
        infoTitle.setText("Order Info");
        currentGrid.setHalignment(infoTitle, HPos.CENTER);

        currentGrid.getChildren().remove(subGrid);
        currentGrid.add(postOrderList, 2, 1);


        prevLabel.setText("To Confirm");
        postLabel.setText("To Cook");
        prevOrderButton.setText("Confirm Order");
        postOrderCook.setText("Cook Order");


        prevOrderButton.setOnAction(event -> confirmOrder());
        postOrderCook.setOnAction(event -> cookOrder());
        postOrderCancel.setOnAction(event -> postOrderCancel());

        prevOrderList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            displayOrderInfo((Order) newValue); });
        postOrderList.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            displayOrderInfo((Order) newValue); }));
    }

    private void displayOrderInfo(Order order) {
        if (order != null){
            StringBuilder instructions = new StringBuilder();

            if (!order.getInstructions().isEmpty()){
                instructions.append(order.getInstructions());
                instructions.append("\n\n");
            }

            for (Food food : order.getFoods()){
                if (food.getInstructions() != null || !food.getChangedIngredients().isEmpty()){
                    instructions.append(food.getName() + ": ");
                    if (food.getInstructions() != null){
                        instructions.append(food.getInstructions());
                        instructions.append("\n");
                    }
                    for (String ingredient : food.getChangedIngredients().keySet()){
                        int changedAmount = food.getChangedIngredients().get(ingredient);
                        if (changedAmount > 0){
                            instructions.append("Add " + ingredient + " x " + Integer.toString(changedAmount) + "\n");
                        } else if (changedAmount < 0){
                            instructions.append("Remove " + ingredient + " x " + Integer.toString(Math.abs(changedAmount)) + "\n");
                        }
                    }
                }
            }

            infoLabel.setText(instructions.toString());
        }
    }

    private void cookOrder() {
        if (!postOrderList.getSelectionModel().isEmpty()){
            ObservableList<Order> orders = postOrderList.getSelectionModel().getSelectedItems();

            for (Order order : orders){
                restaurant.cookOrder(order);
                localToCook.remove(order);
            }
        }
    }

    private void confirmOrder() {
        if (!prevOrderList.getSelectionModel().isEmpty()){
            ObservableList<Order> orders = prevOrderList.getSelectionModel().getSelectedItems();

            for (Order order : orders){
                order.setCookNumber(cookID);
                restaurant.confirmOrder(order);
                localToCook.add(order);
            }
        }
    }

    public void receiveShipment(ActionEvent event){
        Stage sourceStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene sourceScene = ((Node) event.getSource()).getScene();

        try {
            FXMLLoader takeOrderLoader = new FXMLLoader(getClass().getResource("/Interface/Views/ReceiveShipment.fxml"));
            Parent root = takeOrderLoader.load();

            ReceiveShipmentsController receiveShipmentsController = takeOrderLoader.getController();
            receiveShipmentsController.previousScene = sourceScene;
            receiveShipmentsController.init(restaurant);

            sourceStage.setScene(new Scene(root, 600, 400));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancelOrder(){
        if (!prevOrderList.getSelectionModel().isEmpty()){
            Order order = (Order) prevOrderList.getSelectionModel().getSelectedItem();
            restaurant.getOrdersAtStage("Pending").remove(order);
            restaurant.receiveShipment(order.getAllIngredients());
            infoLabel.setText("");
        }
    }

    private void postOrderCancel(){
        if (!postOrderList.getSelectionModel().isEmpty()){
            Order order = (Order) postOrderList.getSelectionModel().getSelectedItem();
            restaurant.getOrdersAtStage("InProgress").remove(order);
        }
    }

}