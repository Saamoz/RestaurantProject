package RestaurantModel.UNUSED;

import RestaurantModel.Interfaces.InventorySystem;
import RestaurantModel.Managers.RequestManager;
import RestaurantModel.RestaurantObjects.Food;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.io.*;
import java.util.*;

/*
* @startuml
* class InventoryManager{
* -inventory: Map<String, SimpleEntry>
* -requests: RequestManager
* - {static} INVENTORY_FILE: String
* - {static} MINIMUM_FILE: String
* +InventoryManager()
* +checkIntegrity(ingredients: Set<String>): void
* +useIngredients(used: Map<String, Integer>): void
* +receiveShipment(shipment: Map<String, Integer>): void
* +getInventory(): ObservableMap<String, Integer>
* +toString(): String
* +hasEnough(food: Food): boolean
* +getCalories(food: Food): int
* }
* @enduml
 */
/**
 * The InventoryManager class that implements the Inventory interface.
 * Represents the inventory of a Restaurant and manages the stock of ingredients for cooking
 * */
class InventoryManager implements InventorySystem {
    private Map<String, SimpleEntry> inventory;
    private RequestManager requests;

    private static final String INVENTORY_FILE = "configs/inventory.txt";
    private static final String MINIMUM_FILE = "configs/minimums.txt";

    /**
     * Initializes the inventory and the RequestManager used to place reorder requests
     */
    InventoryManager (){
        try{
            inventory = new HashMap<>();

            parseFile(INVENTORY_FILE);
            Set<String> minimumKeys = parseFile(MINIMUM_FILE);

            requests = new RequestManager();

            updateInventoryFile();
            updateMinimumsFile(minimumKeys);

            checkAndReorder(inventory.keySet());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * reads the inventory or minimum file and updates the associated information in the inventory
     * also creates the file if it doesn't exist
     * @param fileName the file to be parsed. must either be the INVENTORY_FILE or MINIMUM_FILE
     * */
    private Set<String> parseFile(String fileName) throws IOException{
        if (!(new File(fileName).exists())) {
            new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
            return null;
        } else {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line = reader.readLine();
            Set<String> found = new HashSet<>();

            while (line != null) {
                String[] split = line.split("\\s\\|\\s");
                String key = split[0];
                int num = Integer.parseInt(split[1]);

                if (fileName.equals(INVENTORY_FILE)){
                    inventory.put(key, new SimpleEntry(num));
                }
                else if (fileName.equals(MINIMUM_FILE)){
                    found.add(key);

                    if (inventory.keySet().contains(key)){
                        inventory.get(key).setMinimum(num);
                    }
                    else{
                        inventory.put(key, new SimpleEntry(0, num));
                    }
                }

                line = reader.readLine();
            }
            return found;
        }
    }

    /**
     * updates the inventory.txt file to match the inventory map by wiping and rewriting the file
     * */
    private void updateInventoryFile(){
        try{
            FileWriter clear = new FileWriter(INVENTORY_FILE,false);
            clear.write("");
        } catch(IOException e){
            e.printStackTrace();
        }
        try(PrintWriter fresh = new PrintWriter(new BufferedWriter(new FileWriter(INVENTORY_FILE, true)))){
            for (String key : inventory.keySet()){
                fresh.println(key + " | " + inventory.get(key).getQuantity());
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Analyses for missing entries in the  the minimums.txt file and generates default minimums for each
     * By default, the default minimum for all ingredients is 10 units. This can be adjusted afterwards in the
     * minimums.txt file
     * */
    private void updateMinimumsFile(Set<String> currentInFile) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(MINIMUM_FILE, true)))) {
            for (String key : inventory.keySet()){
                if (!currentInFile.contains(key)){
                    out.println(key + " | " + 10);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * checks if each ingredient in a given set of ingredients is recorded in the inventory. if it is not, it is added
     * to the inventory with a stock-value of 0. any insufficient ingredients are then reordered. entries missing in the
     * minimums file are also added
     * @param ingredients the set of ingredients to check
     * */
    public void checkIntegrity(Set<String> ingredients){
        Set<String> oldKeys = inventory.keySet();

        for (String key : ingredients){
            if (!inventory.containsKey(key)){
                inventory.put(key, new SimpleEntry(0, 10));
            }
        }
        updateInventoryFile();
        updateMinimumsFile(oldKeys);

        checkAndReorder(inventory.keySet());
    }

    /**
     * subtracts the given ingredients from the inventory
     * @param used map of the ingredients (keys) and quantities (values) to be removed
     * */
    public void useIngredients(Map<String, Integer> used){
        for (String key : used.keySet()){
            if (inventory.containsKey(key)) {
                inventory.get(key).useQuantity(used.get(key));
            } else{
                throw new IllegalArgumentException(key + " is not a valid ingredient!");
            }
        }
        checkAndReorder(used.keySet());
        updateInventoryFile();
    }

    /**
     * adds a fresh batch of ingredients to the inventory
     * @param shipment a map of the ingredients (keys) and quantities (values) in the shipment
     * */
    public void receiveShipment(Map<String, Integer> shipment){
        for (String key : shipment.keySet()){
            inventory.get(key).addQuantity(shipment.get(key));
        }
        requests.clear();
        checkAndReorder(shipment.keySet());

        updateInventoryFile();
    }

    /**
     * checks the given set of ingredients and places a reorder request if they are under-stocked
     * @param keys the list of ingredients to be checked
     * */
    private void checkAndReorder(Set<String> keys){
        for (String key : keys){
            if (!inventory.get(key).hasEnough()){
                requests.placeRequest(key);
            }
        }
    }

    /**
     * creates a map of the inventory consisting of the ingredients and their amounts
     * @return a map with ingredients (keys) and their current quantity in the inventory (values)
     * */
    public ObservableMap<String, Integer> getInventory(){
        ObservableMap<String,Integer> quantities = FXCollections.observableHashMap();
        for (String key : inventory.keySet()){
            quantities.put(key, inventory.get(key).getQuantity());
        }
        return quantities;
    }

    /**
     * determines whether there is enough ingredients in the inventory to make the given food
     * @return true or false depending on whether the inventory has all the required ingredients
     * */
    public boolean hasEnough(Food food){
        boolean enough = true;
        Map<String, Integer> ingredients = food.getIngredients();
        for (String key : ingredients.keySet()){
            if (inventory.get(key).getQuantity() < ingredients.get(key)){
                enough = false;
            }
        }
        return enough;
    }

    @Override
    public String toString(){
        StringBuilder full = new StringBuilder();
        for (String key : inventory.keySet()){
            full.append(key);
            full.append(" x ");
            full.append(inventory.get(key).getQuantity());
            full.append(System.lineSeparator());
        }
        return full.toString();
    }
}