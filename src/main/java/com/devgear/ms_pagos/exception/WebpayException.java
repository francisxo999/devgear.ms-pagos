package com.devgear.ms_pagos.exception;

/** Falla al hablar con Transbank. HTTP 502. */
public class WebpayException extends RuntimeException {
    public WebpayException(String message, Throwable cause) { super(message, cause); }
}
