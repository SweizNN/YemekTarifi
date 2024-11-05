package com.example.proje;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;

public class Main extends Application {

    public static Connection connection;

    static {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/tarif_database", "root", "yigit123");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Statement statement;

    static {
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    };

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("hello-view.fxml")));
        Scene scene = new Scene(root);
        primaryStage.setTitle("Tarif Uygulamasi");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // JavaFX uygulamasını başlat
        launch();
    }


    public static ArrayList<String> malzemeler(){
        ArrayList<String > malzemeler=new ArrayList<>();
        try(ResultSet resultSet=statement.executeQuery("SELECT* FROM malzemeler")){
            while (resultSet.next()){
                malzemeler.add(resultSet.getString("MalzemeAdi"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return malzemeler;
    }


    public static ArrayList<String> tarifler(){
        ArrayList<String> tarifler = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery("SELECT * FROM tarifler")) {
            while (resultSet.next()) {
                tarifler.add(resultSet.getString("TarifAdi"));
            }
        }

        // Update sorgusu
        /*    String malzemeKod = "UPDATE tarif_database.malzemeler SET MalzemeAdi = 'domates' WHERE (MalzemeID = 2)";
            statement.executeUpdate(malzemeKod);
*/
        catch (SQLException e) {
            e.printStackTrace();
        }

        return tarifler;
    }
    public static ArrayList<Integer> tarifIdAl(){
        ArrayList<Integer> tariflerID = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery("SELECT * FROM tarifler")) {
            while (resultSet.next()) {
                tariflerID.add(resultSet.getInt("TarifID"));
            }
        }

        // Update sorgusu
        /*    String malzemeKod = "UPDATE tarif_database.malzemeler SET MalzemeAdi = 'domates' WHERE (MalzemeID = 2)";
            statement.executeUpdate(malzemeKod);
*/
        catch (SQLException e) {
            e.printStackTrace();
        }

        return tariflerID;
    }


}
