package nl.jordie24.samics

import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlButton
import com.gargoylesoftware.htmlunit.html.HtmlPage

fun WebClient.enableJs() {
    this.options.isJavaScriptEnabled = true
}

fun WebClient.disableJs() {
    this.options.isJavaScriptEnabled = false
}

fun <P : Page> HtmlPage.clickFirstButton(): P {
    return this.getElementsByTagName(HtmlButton.TAG_NAME).first().click()
}
