#pragma once
#include <Arduino.h>
#include <LiquidCrystal_I2C.h>
#include "config.h"

static LiquidCrystal_I2C lcd(LCD_ADDR, LCD_COLS, LCD_ROWS);

static String nonce_atual;
static unsigned long nonce_ts = 0;

void initDisplay() {
    lcd.init();
    lcd.backlight();
}

String gerarNonce() {
    String n;
    n.reserve(8);
    for (int i = 0; i < 8; i++)
        n += String(random(0, 16), HEX);
    return n;
}

String getNonce() {
    unsigned long agora = millis();
    if (nonce_atual.isEmpty() || (agora - nonce_ts) >= NONCE_TTL_MS) {
        nonce_atual = gerarNonce();
        nonce_ts = agora;
    }
    return nonce_atual;
}

void atualizarDisplay(const String& status, int bateria) {
    String nonce = getNonce();
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("ID:" + String(DEVICE_ID).substring(8));
    lcd.print(" " + String(bateria) + "%");
    lcd.setCursor(0, 1);
    lcd.print(status + " " + nonce);
}
