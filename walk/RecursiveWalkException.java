package ru.ifmo.rain.akimov.walk;

//pass tests

public class RecursiveWalkException extends RuntimeException {
    RecursiveWalkException(String errorMessage) {
        super(errorMessage);
    }

    RecursiveWalkException(String errorMessage, Exception e) {
        super(errorMessage, e);
    }
}
