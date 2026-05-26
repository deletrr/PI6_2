#pragma once
#include <Arduino.h>
#include "config.h"

enum class EstadoVaga { LIVRE, OCUPADA };

EstadoVaga lerSensorUltrasonico() {
    digitalWrite(PIN_SENSOR_TRIGGER, LOW);
    delayMicroseconds(2);
    digitalWrite(PIN_SENSOR_TRIGGER, HIGH);
    delayMicroseconds(10);
    digitalWrite(PIN_SENSOR_TRIGGER, LOW);

    long duracao = pulseIn(PIN_SENSOR_ECHO, HIGH, 30000);
    float distCm = (duracao * 0.0343f) / 2.0f;
    return (distCm > 0 && distCm < DIST_OCUPADA_CM) ? EstadoVaga::OCUPADA : EstadoVaga::LIVRE;
}

EstadoVaga lerSensorMagnetico() {
    return digitalRead(PIN_SENSOR_MAG) == LOW ? EstadoVaga::OCUPADA : EstadoVaga::LIVRE;
}

EstadoVaga lerEstadoVaga() {
    EstadoVaga mag  = lerSensorMagnetico();
    EstadoVaga ult  = lerSensorUltrasonico();
    return (mag == EstadoVaga::OCUPADA || ult == EstadoVaga::OCUPADA)
        ? EstadoVaga::OCUPADA : EstadoVaga::LIVRE;
}

void initSensores() {
    pinMode(PIN_SENSOR_TRIGGER, OUTPUT);
    pinMode(PIN_SENSOR_ECHO, INPUT);
    pinMode(PIN_SENSOR_MAG, INPUT_PULLUP);
}
