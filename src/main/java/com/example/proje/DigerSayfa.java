package com.example.proje;

import com.jfoenix.controls.JFXListCell;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Optional;

public class DigerSayfa extends Main {
    @FXML
    private AnchorPane digerAnchor;
    @FXML
    private JFXTextArea listeText;

    @FXML
    private JFXListView<String> tarifListe;
    @FXML
    private JFXTextArea listeText2;
    @FXML
    private ChoiceBox<String> sıralamaSecim;
    @FXML
    private ChoiceBox<String> filtrelemeSecim;

    String[] yemekTarifleri = {};
    String seciliTarif;
    String seciliSıralama;
    ArrayList<ArrayList<String>> tarifMalzemeleri = new ArrayList<>();
    ArrayList<String> malzemeAdlari = new ArrayList<>();

    @FXML
    public void initialize() {
        tarifListe.getItems().clear();
        yemekTarifleri = tarifler().toArray(new String[0]);
        ArrayList<Integer> tarifIDler = tarifIdAl();
        ArrayList<Integer> olamayantarifler = new ArrayList<>();
        ArrayList<Float> eksikMaliyetler = new ArrayList<>(); // Eksik tariflerin maliyetlerini burada tutacağız

        DecimalFormat df = new DecimalFormat("#.00");

        for (Integer i : tarifIDler) {
            float eksikMalzemeMaliyeti = 0.0f; // Eksik malzemelerin maliyetini tutan değişken
            boolean malzemeEksikMi = false;  // Malzeme eksik mi kontrolü için

            try (ResultSet resultSet1 = statement.executeQuery("SELECT * FROM tarifmalzeme WHERE TarifID = '" + i + "'")) {
                while (resultSet1.next()) {
                    int gerekliMalzemeID = resultSet1.getInt("MalzemeID");
                    int gerekliMiktar = resultSet1.getInt("MalzemeMiktar"); // Tarif için gerekli miktar

                    // Şimdi elimizdeki malzeme miktarını kontrol edelim
                    try (ResultSet resultSet2 = connection.createStatement().executeQuery("SELECT ToplamMiktar, BirimFiyat FROM malzemeler WHERE MalzemeID = '" + gerekliMalzemeID + "'")) {
                        if (resultSet2.next()) {
                            int eldekiMiktar = resultSet2.getInt("ToplamMiktar"); // Elimizdeki malzeme miktarı
                            float birimFiyat = resultSet2.getFloat("BirimFiyat"); // Malzemenin birim fiyatı

                            // Yeterli malzeme olup olmadığını kontrol et
                            if (eldekiMiktar < gerekliMiktar) {
                                malzemeEksikMi = true;  // Malzeme eksik
                                int eksikMiktar = gerekliMiktar - eldekiMiktar;
                                eksikMalzemeMaliyeti += eksikMiktar * birimFiyat; // Eksik malzeme maliyeti hesapla
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // Eğer malzeme eksikse tarifi olamayantarifler listesine ekle
            if (malzemeEksikMi) {
                olamayantarifler.add(i);
                eksikMaliyetler.add(eksikMalzemeMaliyeti); // Eksik maliyeti kaydet
            } else {
                eksikMaliyetler.add(0.0F);  // Eksik malzeme yoksa maliyet sıfır
            }


            // Tarifi listede gösterme ve renklendirme kısmı
            try (ResultSet resultSet = statement.executeQuery("SELECT DISTINCT * FROM tarifler WHERE TarifID = '" + i + "'")) {
                while (resultSet.next()) {
                    String tarifBilgileri = resultSet.getString("TarifAdi");
                    tarifListe.getItems().add(tarifBilgileri.trim());
                    tarifListe.setCellFactory(param -> new JFXListCell<String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);

                            if (empty || item == null) {
                                setText(null);
                                setStyle(""); // Varsayılan stil
                            } else {
                                setText(item);

                                // Arka plan rengini değiştirme
                                if (olamayantarifler.contains(tarifIDler.get(getIndex()))) {
                                    // Eksik malzemeli tarifler kırmızı renkte
                                    float eksikMaliyet = eksikMaliyetler.get(getIndex()); // Eksik maliyeti çekiyoruz
                                    setText(item + "                     " + df.format(eksikMaliyet) + " TL");
                                    setStyle("-fx-text-fill: red;-fx-background-color:  black;-fx-font-family: Arial Black Bold;-fx-background-radius: 10;-fx-text-alignment: justify;");

                                } else {
                                    // Tüm malzemeleri yeterli tarifler yeşil renkte
                                    setStyle("-fx-text-fill: green;-fx-background-color:  black;-fx-font-family: Arial Black Bold;-fx-background-radius: 10;-fx-text-alignment: justify;");
                                }
                            }
                        }
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // ChoiceBox için sıralama seçenekleri
        sıralamaSecim.getItems().addAll("Hazırlama Süresi - En Hızlı", "Hazırlama Süresi - En Yavaş", "Maliyet - Artan", "Maliyet - Azalan");
        tarifListe.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
                seciliTarif = tarifListe.getSelectionModel().getSelectedItem();
                tarifDetayiGoster(seciliTarif);
            }
        });

        // ChoiceBox'ta seçim değiştiğinde sıralama işlemi
        sıralamaSecim.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
                seciliSıralama=sıralamaSecim.getSelectionModel().getSelectedItem();
                tarifleriSirala(seciliSıralama);
            }
        });

        filtrelemeSecim.getItems().addAll("Malzeme Sayısı", "Kategori", "Maliyet Aralığı");
        filtrelemeSecim.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
                String seciliFiltre = filtrelemeSecim.getSelectionModel().getSelectedItem();
            if(seciliFiltre.equals("Maliyet Aralığı")){
                tarifListe.getItems().clear();
                String sql="";
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Maliyet Aralığı");
                dialog.setHeaderText("Minimum Maliyet Değerini Girin:");
                dialog.setContentText("Minimum:");
                Optional<String> minresult = dialog.showAndWait();
                dialog.setHeaderText("Maksimum Maliyet Değerini Girin:");
                dialog.setContentText("Maksimum:");
                Optional<String> maxresult = dialog.showAndWait();
                String minMaliyet=minresult.orElse("0");
                String maxMaliyet=maxresult.orElse("99999");
                    // Tarifi maliyet aralığına göre filtrele
                    sql = "SELECT t.TarifAdi, SUM(m.BirimFiyat * tm.MalzemeMiktar) AS ToplamMaliyet " + "FROM tarifler t " + "JOIN tarifmalzeme tm ON t.TarifID = tm.TarifID " +
                            "JOIN malzemeler m ON m.MalzemeID = tm.MalzemeID " +
                            "GROUP BY t.TarifAdi " +
                            "HAVING ToplamMaliyet BETWEEN " +minMaliyet+ " AND " +maxMaliyet; // Buraya kullanıcıdan minimum ve maksimum maliyet alınabilir.
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    while (resultSet.next()) {
                        String tarifAdi = resultSet.getString("TarifAdi");
                        tarifListe.getItems().add(tarifAdi);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            else if(seciliFiltre.equals("Kategori")){
                tarifListe.getItems().clear();
                String sql="";
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Kategori");
                dialog.setHeaderText("Kategoriyi Giriniz:");
                dialog.setContentText("Kategori:");
                Optional<String> result = dialog.showAndWait();
                String secim=result.orElse("Ana Yemek");
                    // Tarifi kategorisine göre filtrele
                    sql = "SELECT TarifAdi FROM tarifler WHERE Kategori = '" +secim+ "'"; // Buraya da kullanıcıdan kategori değeri alınabilir.
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    while (resultSet.next()) {
                        String tarifAdi = resultSet.getString("TarifAdi");
                        tarifListe.getItems().add(tarifAdi);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            else if(seciliFiltre.equals("Malzeme Sayısı")){
                tarifListe.getItems().clear();
                String sql="";
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Malzeme Sayısı");
                dialog.setHeaderText("Malzeme Sayısı Girin:");
                dialog.setContentText("Malzeme Sayısı:");
                Optional<String> result = dialog.showAndWait();
                String secim=result.orElse("3");
                    // Tarifi kullandığı malzeme sayısına göre filtrele
                    sql = "SELECT t.TarifAdi, COUNT(tm.MalzemeID) AS MalzemeSayisi " +
                            "FROM tarifler t " +
                            "JOIN tarifmalzeme tm ON t.TarifID = tm.TarifID " +
                            "GROUP BY t.TarifAdi " +
                            "HAVING COUNT(tm.MalzemeID) = '"+secim+ "'"; // Buraya kullanıcıdan bir değer alınabilir.
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    while (resultSet.next()) {
                        String tarifAdi = resultSet.getString("TarifAdi");
                        tarifListe.getItems().add(tarifAdi);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            }
        });
    }


    // Tarif detaylarını gösterme
    private void tarifDetayiGoster(String seciliTarif) {
        String tarifDetayi = "";
        int secilenTarifID = 0;

        try (ResultSet resultSet = statement.executeQuery("SELECT * FROM tarifler WHERE TarifAdi = '" + seciliTarif + "'")) {
            while (resultSet.next()) {
                secilenTarifID = resultSet.getInt("TarifID");
                for (int i = 2; i <= 5; i++) {
                    tarifDetayi = tarifDetayi + resultSet.getString(i) + "         ";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        tarifMalzemeleri.clear(); // Eski malzeme bilgilerini temizle

        try (ResultSet resultSet = statement.executeQuery("SELECT MalzemeID, MalzemeMiktar FROM tarifmalzeme WHERE TarifID= " + secilenTarifID)) {
            while (resultSet.next()) {
                ArrayList<String> tarifMalzeme = new ArrayList<>();
                for (int i = 1; i <= 2; i++) {
                    tarifMalzeme.add(resultSet.getString(i));
                }
                tarifMalzemeleri.add(tarifMalzeme);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        float maliyet = 0.0F;
        malzemeAdlari.clear();
        for (ArrayList<String> tarifMalzeme : tarifMalzemeleri) {
            try (ResultSet resultSet = statement.executeQuery("SELECT BirimFiyat, MalzemeAdi FROM malzemeler WHERE MalzemeID= " + tarifMalzeme.get(0))) {
                while (resultSet.next()) {
                    maliyet += resultSet.getFloat("BirimFiyat") * Float.parseFloat(tarifMalzeme.get(1));
                    malzemeAdlari.add(resultSet.getString(2));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // virgülden sonra 2 basamak almak için
        DecimalFormat df = new DecimalFormat("#.00");
        String maliyetStr = df.format(maliyet);
        listeText.setText(tarifDetayi);
        listeText2.setText(malzemeAdlari + "                                     " + maliyetStr);
    }


    // Tarifleri sıralama
    private void tarifleriSirala(String secim) {
        tarifListe.getItems().clear(); // Mevcut listeyi temizle
        String sql = "";
        switch (secim) {
            case "Hazırlama Süresi - En Hızlı":
                sql = "SELECT TarifAdi FROM tarifler ORDER BY HazirlamaSuresi ASC";//artan sıralama
                break;
            case "Hazırlama Süresi - En Yavaş":
                sql = "SELECT TarifAdi FROM tarifler ORDER BY HazirlamaSuresi DESC";//azalan sıralama
                break;
            case "Maliyet - Artan":
                sql = "SELECT t.TarifAdi, SUM(m.BirimFiyat * tm.MalzemeMiktar) AS ToplamMaliyet " +
                        "FROM tarifler t " + "JOIN tarifmalzeme tm ON t.TarifID = tm.TarifID " + "JOIN malzemeler m ON m.MalzemeID = tm.MalzemeID " +
                        "GROUP BY t.TarifAdi " + "ORDER BY ToplamMaliyet ASC";
                break;
            case "Maliyet - Azalan":
                sql = "SELECT t.TarifAdi, SUM(m.BirimFiyat * tm.MalzemeMiktar) AS ToplamMaliyet " +
                        "FROM tarifler t " + "JOIN tarifmalzeme tm ON t.TarifID = tm.TarifID " + "JOIN malzemeler m ON m.MalzemeID = tm.MalzemeID " +
                        "GROUP BY t.TarifAdi " + "ORDER BY ToplamMaliyet DESC";
                break;
        }
        // Sıralanmış tarifleri listeye ekle
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String tarifAdi = resultSet.getString("TarifAdi");
                tarifListe.getItems().add(tarifAdi);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }























    public void geriGel() throws IOException {
        new EkranDegis(digerAnchor, "hello-view.fxml");
    }

    public void ekranDegis() throws IOException {
        new EkranDegis(digerAnchor, "sonsayfa.fxml");
    }

    public void ekranDegis2() throws IOException {
        new EkranDegis(digerAnchor, "sonsonsayfa.fxml");
    }
}
