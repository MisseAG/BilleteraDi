module co.edu.uniquindio.billeteradi {
    requires javafx.controls;
    requires javafx.fxml;

    opens co.edu.uniquindio.billeteradi to javafx.fxml;
    opens co.edu.uniquindio.billeteradi.controller to javafx.fxml;
    opens co.edu.uniquindio.billeteradi.model.entity to javafx.base;
    opens co.edu.uniquindio.billeteradi.model.service;
    opens co.edu.uniquindio.billeteradi.model.repository;

    exports co.edu.uniquindio.billeteradi;
    exports co.edu.uniquindio.billeteradi.tests;
    exports co.edu.uniquindio.billeteradi.controller;
    exports co.edu.uniquindio.billeteradi.model.entity;
    exports co.edu.uniquindio.billeteradi.model.enums;
    exports co.edu.uniquindio.billeteradi.model.service;
    exports co.edu.uniquindio.billeteradi.model.repository;
    exports co.edu.uniquindio.billeteradi.model.exception;
    exports co.edu.uniquindio.billeteradi.model.structure;
}
