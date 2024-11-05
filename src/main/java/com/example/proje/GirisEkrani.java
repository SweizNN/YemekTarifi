package com.example.proje;


import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;


public class GirisEkrani {

    @FXML
    private AnchorPane girisAnchor;


    @FXML
    public void ekranDegis() throws IOException {
        new EkranDegis(girisAnchor,"digerSayfa.fxml");


    }

}


