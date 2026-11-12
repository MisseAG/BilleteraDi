module co.edu.uniquindio.billeteradi {
    requires javafx.controls;
    requires javafx.fxml;


    opens co.edu.uniquindio.billeteradi to javafx.fxml;
    opens co.edu.uniquindio.billeteradi.model.service;
    opens co.edu.uniquindio.billeteradi.model.repository;
    exports co.edu.uniquindio.billeteradi;
    exports co.edu.uniquindio.billeteradi.tests;
}