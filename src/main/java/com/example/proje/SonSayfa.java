package com.example.proje;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class SonSayfa extends Main {

    @FXML
    private JFXTextArea silincekTarifAdi;

    @FXML
    private AnchorPane sonAnchor;

    @FXML
    private JFXTextArea tarifAdiTextArea;

    @FXML
    private JFXButton tarifEkleButon;

    @FXML
    private JFXTextArea tarifKategoriTextArea;

    @FXML
    private JFXTextArea tarifMalzemeAdi;

    @FXML
    private JFXTextArea tarifMalzemeMiktar;

    @FXML
    private JFXButton tarifSilButon;

    @FXML
    private JFXTextArea tarifSureTextArea;

    @FXML
    private JFXTextArea tarifTalimatTextArea;

    @FXML
    private JFXTextArea yeniMalzemeAd;

    @FXML
    private JFXTextArea yeniMalzemeBirim;

    @FXML
    private JFXButton yeniMalzemeButon;

    @FXML
    private JFXTextArea yeniMalzemeFiyat;

    @FXML
    private JFXTextArea yeniMalzemeMiktar;
    @FXML
    private JFXTextArea tarifMalzemeAdiText;
    @FXML
    private Label malzemeYokLabel;
    @FXML
    private JFXListView<String> tarifMalzemeView;
    @FXML
    private Label malzemeLabel;
    @FXML
    private Label silLabel;


    String[] malzemelerArr={};
    String seciliMalzeme;


    public void initialize() {
        tarifMalzemeView.getItems().clear();
        malzemelerArr = malzemeler().toArray(new String[0]);
        tarifMalzemeView.getItems().addAll(malzemelerArr);

        tarifMalzemeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
                seciliMalzeme = tarifMalzemeView.getSelectionModel().getSelectedItem();
                String malzemeDetay = "";

                try (ResultSet resultSet = statement.executeQuery("SELECT MalzemeAdi FROM malzemeler WHERE MalzemeAdi = '" + seciliMalzeme + "'")) {
                    while (resultSet.next()) {
                        malzemeDetay = resultSet.getString("MalzemeAdi");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                // Önceki metni al ve yeni malzeme adını ekle
                String mevcutMetin = tarifMalzemeAdiText.getText();
                if (!mevcutMetin.isEmpty()) {
                    tarifMalzemeAdiText.appendText(","); // Öncekinden ayırmak için virgül ekleyin
                }
                tarifMalzemeAdiText.appendText(malzemeDetay);
            }
        });
    }



    public void tarifEkle() {
        String tarifAdi = tarifAdiTextArea.getText();
        String tarifKategori = tarifKategoriTextArea.getText();
        int tarifHazirlanmaSuresi = Integer.parseInt(tarifSureTextArea.getText());
        String tarifTalimat = tarifTalimatTextArea.getText();
        String[] tarifMalzemeAdi = tarifMalzemeAdiText.getText().split(",");
        String[] tarifMalzemeMiktari = tarifMalzemeMiktar.getText().split(",");

        try {
            // Aynı isimde tarif olup olmadığını kontrol et
            String checkSql = "SELECT TarifID FROM tarifler WHERE TarifAdi = ?";
            int tarifID = 0;
            try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
                checkStatement.setString(1, tarifAdi);
                ResultSet resultSet = checkStatement.executeQuery();

                if (resultSet.next()) {
                    // Eğer tarif mevcutsa, tarifID al
                    tarifID = resultSet.getInt("TarifID");
                }
            }

            if (tarifID > 0) {
                // Tarif güncelleme işlemi
                String updateSql = """
                UPDATE tarifler SET Kategori = ?, HazirlamaSuresi = ?, Talimatlar = ? WHERE TarifID = ? """;
                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setString(1, tarifKategori);
                    updateStatement.setInt(2, tarifHazirlanmaSuresi);
                    updateStatement.setString(3, tarifTalimat);
                    updateStatement.setInt(4, tarifID);
                    updateStatement.executeUpdate();
                }
                malzemeYokLabel.setText("Tarif Güncellendi!!");
            } else {
                // Yeni tarif ekleme işlemi
                String insertSql = """
                INSERT INTO tarifler (TarifAdi, Kategori, HazirlamaSuresi, Talimatlar) 
                VALUES (?, ?, ?, ?)
                """;
                try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, tarifAdi);
                    preparedStatement.setString(2, tarifKategori);
                    preparedStatement.setInt(3, tarifHazirlanmaSuresi);
                    preparedStatement.setString(4, tarifTalimat);
                    preparedStatement.executeUpdate();

                    ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        tarifID = generatedKeys.getInt(1);
                    }
                }
                malzemeYokLabel.setText("Tarif Eklendi!!");
            }

            // Malzemeleri güncelleme veya ekleme
            // Öncelikle tarifmalzeme tablosundaki mevcut malzemeleri sil
            String deleteMalzemeSql = "DELETE FROM tarifmalzeme WHERE TarifID = ?";
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteMalzemeSql)) {
                deleteStatement.setInt(1, tarifID);
                deleteStatement.executeUpdate();
            }

            // Yeni malzemeleri ekleme
            int i = 0;
            for (String malzemeAdi : tarifMalzemeAdi) {
                ResultSet malzemeIDrs = statement.executeQuery("SELECT MalzemeID FROM malzemeler WHERE MalzemeAdi = '%s'".formatted(malzemeAdi.trim()));
                if (malzemeIDrs.next()) {
                    int malzemeID = malzemeIDrs.getInt("MalzemeID");
                    int malzemeMiktar = Integer.parseInt(tarifMalzemeMiktari[i]);
                    String sqlMalzeme = """
                    INSERT INTO tarifmalzeme (TarifID, MalzemeID, MalzemeMiktar) VALUES (?, ?, ?)
                    """;
                    try (PreparedStatement malzemeStatement = connection.prepareStatement(sqlMalzeme)) {
                        malzemeStatement.setInt(1, tarifID);
                        malzemeStatement.setInt(2, malzemeID);
                        malzemeStatement.setInt(3, malzemeMiktar);
                        malzemeStatement.executeUpdate();
                    }
                } else {
                    malzemeYokLabel.setText("Koymak İstediğiniz Malzeme Yok Lütfen Ekleyin!");
                }
                i++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public void tarifSil() {
        int tarifID = 0;
        String tarifAdi = silincekTarifAdi.getText();
        System.out.println(tarifAdi);
        if(silincekTarifAdi.getText().isEmpty()){
            silLabel.setText("Lütfen Geçerli Tarif Adı Girin!!");
        }else{
            try {
                // TarifID'yi almak için sorgu
                ResultSet tarifIDrs = statement.executeQuery("SELECT TarifID FROM tarifler WHERE TarifAdi = '%s'".formatted(tarifAdi));

                if (tarifIDrs.next()) {
                    tarifID = tarifIDrs.getInt(1);

                    // Önce tarifmalzeme tablosundan sil
                    String sqlTarifMalzemeSil = "DELETE FROM tarifmalzeme WHERE TarifID = '%d'".formatted(tarifID);
                    statement.executeUpdate(sqlTarifMalzemeSil);
                }

                // Şimdi tarifler tablosundan sil
                String sqlSil = "DELETE FROM tarifler WHERE TarifAdi = '%s'".formatted(tarifAdi);
                statement.executeUpdate(sqlSil);
                silLabel.setText("Tarif Silindi!!");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }


    public void malzemeEkle() {
        String malzemeAdi = yeniMalzemeAd.getText();
        String malzemeMiktar = yeniMalzemeMiktar.getText();
        String malzemeBirim = yeniMalzemeBirim.getText();
        float malzemeFiyat = Float.parseFloat(yeniMalzemeFiyat.getText());

        try {
            // Aynı isimde malzeme olup olmadığını kontrol et
            String checkSql = "SELECT COUNT(*) FROM malzemeler WHERE MalzemeAdi = ?";
            try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
                checkStatement.setString(1, malzemeAdi);
                ResultSet resultSet = checkStatement.executeQuery();

                if (resultSet.next() && resultSet.getInt(1) > 0) {
                    malzemeLabel.setText("Bu malzeme sistemde zaten mevcut");
                    return;
                }
            }

            // Malzeme ekleme işlemi
            String sqlFonk = """
                INSERT INTO malzemeler (MalzemeAdi, ToplamMiktar, MalzemeBirim, BirimFiyat) 
                VALUES (?, ?, ?, ?)
                """;

            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlFonk)) {
                preparedStatement.setString(1, malzemeAdi);
                preparedStatement.setString(2, malzemeMiktar);
                preparedStatement.setString(3, malzemeBirim);
                preparedStatement.setFloat(4, malzemeFiyat);

                preparedStatement.executeUpdate();
                malzemeLabel.setText("Malzeme başarıyla eklendi.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

































    public void geriGel() throws IOException{
    new EkranDegis(sonAnchor,"digerSayfa.fxml");
}

}
