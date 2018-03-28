package Interface.Controllers;

import RestaurantModel.Interfaces.RestaurantModel;
import RestaurantModel.Managers.Restaurant;
import RestaurantModel.RestaurantObjects.Order;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class BillController {

    @FXML
    public GridPane parentGrid;

    @FXML
    private Label finalPrice;
    @FXML
    private Label fullBill;
    @FXML
    private Label tipPrice;
    @FXML
    private Label subTotal;

    @FXML
    private TextField tipInput;

    private Button prevButton;
    private double tipAmount;
    private double totalPrice;

    public void init(ObservableList<Order> orders, RestaurantModel restaurant, Button button){
        double taxAmount = 0.15;
        prevButton = button;
        tipAmount = 0.15;
        totalPrice = 0;
        StringBuilder totalBill = new StringBuilder();

        for (Order order : orders){
            totalBill.append(order.toString() + ":\n");
            totalBill.append(restaurant.requestBill(order));
            totalBill.append("Order total: " + order.getTotalPrice() + "\n\n");

            totalPrice = totalPrice + order.getTotalPrice();
        }

        fullBill.setText(totalBill.toString());

        setLabels(taxAmount, tipAmount, totalPrice);

        tipInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*$")) {
                tipInput.setText(newValue.replaceAll("[^(\\d*.?\\d*$)]", ""));
            }
        });

        tipInput.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                tipAmount = Double.parseDouble(newValue)/100;
                setLabels(taxAmount, tipAmount, totalPrice);
            } catch (Exception ignored){} });
    }

    private void setLabels(double taxAmount, double tipAmount, double totalPrice){
        tipPrice.setText("Tip of " + tipAmount * totalPrice + "$ at " + tipAmount*100 + "%");
        subTotal.setText("Subtotal: " + totalPrice + "$ plus " + taxAmount*100 + "% tax of " + totalPrice * taxAmount + "$");
        finalPrice.setText("Total Price: " + Double.toString(totalPrice + totalPrice * taxAmount + tipAmount * totalPrice) + "$");
    }

    public void closeBill(){
        parentGrid.setVisible(false);
        prevButton.setVisible(true);
    }
}