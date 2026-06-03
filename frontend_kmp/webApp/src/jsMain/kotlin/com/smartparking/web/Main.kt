package com.smartparking.web

import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.css.*

fun main() {
    renderComposable(rootElementId = "root") {
        Style(AppStylesheet)
        Div({ classes(AppStylesheet.container) }) {
            H1 { Text("PontoLivre — Web") }
            P { Text("A versão Web está em desenvolvimento usando Compose HTML.") }
            P { Text("Para a experiência completa, utilize o App Android.") }
            
            Div({ classes(AppStylesheet.card) }) {
                Text("O servidor Web está ativo e respondendo corretamente!")
            }
        }
    }
}

object AppStylesheet : StyleSheet() {
    val container by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        padding(20.px)
    }

    val card by style {
        marginTop(20.px)
        padding(20.px)
        border(1.px, LineStyle.Solid, Color.lightgray)
    }
}
