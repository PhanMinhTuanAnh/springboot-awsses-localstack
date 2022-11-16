package com.example.anh.ses

import org.junit.jupiter.api.Test

class Test {
    @Test
    fun test() {
        val sesv2 = SendMessageSESv2()
        sesv2.send()
    }
}