package com.example.proje;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListCell;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class sonsonSayfa extends Main {
    @FXML
    private JFXButton araButon;

    @FXML
    private JFXTextArea aramaCubugu;

    @FXML
    private JFXListView<String> malzemelerList;

    @FXML
    private AnchorPane sonsonAnchor;

    @FXML
    private JFXListView<String> sonucList;

    String[] malzemelerArray = {};
    String sectigimizMalzeme;




    public void initialize() {
        //Burada karşımıza çıkan malzeme listesini yapıyoruz
        malzemelerList.getItems().clear();
        malzemelerArray = malzemeler().toArray(new String[0]);
        malzemelerList.getItems().addAll(malzemelerArray);
        malzemelerList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
                sectigimizMalzeme = malzemelerList.getSelectionModel().getSelectedItem();
                String malzemeDetay = "";

                try (ResultSet resultSet = statement.executeQuery("SELECT MalzemeAdi FROM malzemeler WHERE MalzemeAdi = '" + sectigimizMalzeme + "'")) {
                    while (resultSet.next()) {
                        malzemeDetay = resultSet.getString("MalzemeAdi");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                //aramaCubuguna tıkladıgımız malzemeleri yazıyoruz
                String mevcutMetin = aramaCubugu.getText();
                if (!mevcutMetin.isEmpty()) {
                    aramaCubugu.appendText(",");
                }
                aramaCubugu.appendText(malzemeDetay);
            }
        });

        // Arama butonuna tıklanma işlemi
        araButon.setOnAction(event -> {
            String tarifAdi = aramaCubugu.getText().trim(); // Kullanıcının arama çubuğuna girdiği tarif adı
            sonucList.getItems().clear();
            //burda kullanıcı malzeme mi yoksa tarif adı mı giriyor onu kontrol ediyoruz
            if (!tarifAdi.isEmpty() && !tarifAdi.contains(",")) {
                // Eğer kullanıcı arama çubuğuna tarif adı girmişse isme göre arama yapıyoruz
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM tarifler WHERE TarifAdi = '" + tarifAdi + "'")) {
                    if (resultSet.next()) {
                        String tarifBilgileri = String.format(
                                "%s                                             %s                                             %d                                             %s",
                                resultSet.getString("TarifAdi"),
                                resultSet.getString("Kategori"),
                                resultSet.getInt("HazirlamaSuresi"),
                                resultSet.getString("Talimatlar")
                        );
                        sonucList.getItems().add(tarifBilgileri.trim());
                    } else {
                        sonucList.getItems().add("Aradığınız tarif bulunamadı.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                // Eğer arama çubuğunda malzemeler varsa malzeme listesine göre arama yapma yeri
                String[] malzemeListesi = aramaCubugu.getText().split(",");
                ArrayList<String> girilenMalzemeler = new ArrayList<>();//Malzemeleri tutan arraylist
                for (String malzeme : malzemeListesi) {
                    girilenMalzemeler.add(malzeme.trim());
                }

                // Seçtiğimiz malzemelerin ID'lerini bulduğumuz yer
                ArrayList<Integer> malzemeIDleri = new ArrayList<>();
                for (String s : girilenMalzemeler) {
                    try (ResultSet resultSet = statement.executeQuery("SELECT MalzemeID FROM malzemeler WHERE MalzemeAdi = '" + s + "'")) {
                        while (resultSet.next()) {
                            malzemeIDleri.add(resultSet.getInt("MalzemeID"));
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                if (malzemeIDleri.isEmpty()) {
                    sonucList.getItems().add("Girilen malzemelerden hiçbiri bulunamadı.");
                    return;
                }

                // tarifmalzeme tablosundan malzemeID'lerini bulduğumuz tariflerin ID'lerini buluyoruz
                ArrayList<Integer> tarifIDleri = new ArrayList<>();
                for (int i : malzemeIDleri) {
                    try (ResultSet resultSet = statement.executeQuery("SELECT DISTINCT TarifID FROM tarifmalzeme WHERE MalzemeID = '" + i + "'")) {
                        while (resultSet.next()) {
                            if (!tarifIDleri.contains(resultSet.getInt("TarifID")))
                                tarifIDleri.add(resultSet.getInt("TarifID"));
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                if (tarifIDleri.isEmpty()) {
                    sonucList.getItems().add("Girilen malzemelerle eşleşen tarif bulunamadı.");
                    return;
                }
                float birimyuzde = 0;
                float yuzde = 0;
                int tarifmalzemesayisi = 0;
                int eslesenmalzemesayisi = 0;
                ArrayList<Float[]> sirasizTarifler = new ArrayList<>();
                // tarifID'sini bulduğumuz tarifleri tarifler tablosundan getirip bilgilerini yazdırma
                for (int i : tarifIDleri) {
                    yuzde = 0;
                    tarifmalzemesayisi = 0;
                    eslesenmalzemesayisi = 0;
                    try (ResultSet resultSet1 = statement.executeQuery(
                            "SELECT * FROM tarifmalzeme WHERE TarifID = '" + i + "'")) {
                        while (resultSet1.next()) {
                            for (int i1 : malzemeIDleri) {
                                if (i1 == resultSet1.getInt("MalzemeID")) {
                                    eslesenmalzemesayisi++;
                                }
                            }
                            tarifmalzemesayisi++;

                        }
                        birimyuzde = (float) (100.0 / tarifmalzemesayisi);
                        yuzde = birimyuzde * eslesenmalzemesayisi;
                        sirasizTarifler.add(new Float[]{(float) i,  yuzde});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                ArrayList<Float[]> siraliTarifler = bubbleSort(sirasizTarifler);
                sonucList.getItems().clear();
                System.out.println(tarifIDleri);
                ArrayList<Integer> olamayantarifler = new ArrayList<Integer>();
                for (Float[] i : siraliTarifler) {
                    boolean eksikMalzemeVar = false;

                    try (ResultSet resultSet1 = statement.executeQuery("SELECT * FROM tarifmalzeme WHERE TarifID = '" + i[0] + "'")) {
                        while (resultSet1.next()) {
                            int gerekliMalzemeID = resultSet1.getInt("MalzemeID");
                            int gerekliMiktar = resultSet1.getInt("MalzemeMiktar"); // Tarif için gerekli miktar

                            // Şimdi elimizdeki malzeme miktarını kontrol edelim
                            try (ResultSet resultSet2 = connection.createStatement().executeQuery("SELECT ToplamMiktar FROM malzemeler WHERE MalzemeID = '" + gerekliMalzemeID + "'")) {
                                if (resultSet2.next()) {
                                    int eldekiMiktar = resultSet2.getInt("ToplamMiktar"); // Elimizdeki malzeme miktarı

                                    if (eldekiMiktar < gerekliMiktar) {
                                        olamayantarifler.add(siraliTarifler.indexOf(i));
                                        break; // Eğer bir eksik malzeme varsa, tarifi kırmızıya boyayacağız, devam etmeye gerek yok
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // Tarifi listede gösterme ve renklendirme kısmı
                    try (ResultSet resultSet = statement.executeQuery("SELECT DISTINCT * FROM tarifler WHERE TarifID = '" + i[0] + "'")) {
                        while (resultSet.next()) {
                            String tarifBilgileri = String.format(
                                    "                              %s                              %s                              %d                              %s                          %.2f",
                                    resultSet.getString("TarifAdi"),
                                    resultSet.getString("Kategori"),
                                    resultSet.getInt("HazirlamaSuresi"),
                                    resultSet.getString("Talimatlar"),
                                    i[1]
                            );

                            sonucList.getItems().add(tarifBilgileri.trim());


                            sonucList.setCellFactory(param -> new JFXListCell<String>() {
                                @Override
                                protected void updateItem(String item, boolean empty) {
                                    super.updateItem(item, empty);

                                    if (empty || item == null) {
                                        setText(null);
                                        setStyle(""); // Varsayılan stil
                                    } else {
                                        setText(item);

                                        // Arka plan rengini değiştirme
                                        if (olamayantarifler.contains(getIndex())) {
                                            // Eksik malzemeli tarifler kırmızı renkte
                                            setStyle("-fx-text-fill: red;-fx-background-color: black;-fx-font-family: Arial Black;-fx-background-radius: 15;-fx-text-alignment: justify;");
                                        } else {
                                            // Tüm malzemeleri yeterli tarifler yeşil renkte
                                            setStyle("-fx-text-fill: green;-fx-background-color: black;-fx-font-family: Arial Black;-fx-background-radius: 15;-fx-text-alignment: justify;");
                                        }
                                    }
                                }
                            });
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }



            }
        });
    }





    static ArrayList<Float[]> bubbleSort(ArrayList<Float[]> arr) {
        int n = arr.size();
        boolean swapped;

        for (int i = 0; i < n - 1; i++) {
            swapped = false; // Her döngüde değişim olup olmadığını kontrol etmek için

            for (int j = 0; j < n - 1 - i; j++) {
                if (arr.get(j)[1] < arr.get(j + 1)[1]) {
                    // Elemanları değiştir
                    Float[] temp = arr.get(j);
                    arr.set(j, arr.get(j+1));
                    arr.set(j + 1,temp);
                    swapped = true; // Değişim oldu
                }
            }

            // Eğer iç döngüde hiç değişim olmadıysa, dizi zaten sıralıdır
            if (!swapped) {
                break;
            }
        }
        return arr;
    }


    public void geriGel() throws IOException {
        new EkranDegis(sonsonAnchor, "digerSayfa.fxml");
    }
}
