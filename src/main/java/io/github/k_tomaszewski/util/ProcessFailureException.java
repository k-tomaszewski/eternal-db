package io.github.k_tomaszewski.util;

import java.io.BufferedReader;
import java.io.IOException;

public class ProcessFailureException extends RuntimeException {

    public ProcessFailureException(int exitValue, BufferedReader stdErrOutput) {
        super("Exit value: %d".formatted(exitValue) + getFirstLine(stdErrOutput));
    }

    private static String getFirstLine(BufferedReader stdErrOutput) {
        try (stdErrOutput) {
            return " (" + stdErrOutput.readLine() + ")";
        } catch (IOException e) {
            return "";
        }
    }
}
