package com.upi.expensetracker.utils

import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    @Test
    fun testHdfcDebitSms() {
        val sms = "Alert: Rs. 1500.00 debited from A/c XX1234 on 21-05-26 to SWIGGY. Ref No 654321098765"
        val transaction = SmsParser.parseSMS(sms)
        assertNotNull(transaction)
        assertEquals(1500.00, transaction!!.amount, 0.0)
        assertEquals("Swiggy", transaction.merchant)
        assertEquals("1234", transaction.accountLast4)
        assertEquals("654321098765", transaction.refId)
        assertEquals("2026-05-21", transaction.date)
        assertEquals("Food & Dining", transaction.category)
    }

    @Test
    fun testIciciDebitSms() {
        val sms = "Dear Customer, your Acct ending 4567 has been debited by INR 250.00 on 21-May-26. Info: ZOMATO. UPI Ref: 123456789012."
        val transaction = SmsParser.parseSMS(sms)
        assertNotNull(transaction)
        assertEquals(250.00, transaction!!.amount, 0.0)
        assertEquals("Zomato", transaction.merchant)
        assertEquals("4567", transaction.accountLast4)
        assertEquals("123456789012", transaction.refId)
        assertEquals("2026-05-21", transaction.date)
        assertEquals("Food & Dining", transaction.category)
    }

    @Test
    fun testSbiDebitSms() {
        val sms = "SBI: Rs 500 debited for online transfer to OLA. UPI Ref 098765432109 on 21/05/2026. A/c ending in 7890."
        val transaction = SmsParser.parseSMS(sms)
        assertNotNull(transaction)
        assertEquals(500.00, transaction!!.amount, 0.0)
        assertEquals("Ola", transaction.merchant)
        assertEquals("7890", transaction.accountLast4)
        assertEquals("098765432109", transaction.refId)
        assertEquals("2026-05-21", transaction.date)
        assertEquals("Transport", transaction.category)
    }

    @Test
    fun testAxisDebitSms() {
        val sms = "Axis Bank: Sent Rs. 120.00 to RAPIDO from A/c XX3456. Ref: 234567890123. Date 21-05-26 15:30:00."
        val transaction = SmsParser.parseSMS(sms)
        assertNotNull(transaction)
        assertEquals(120.00, transaction!!.amount, 0.0)
        assertEquals("Rapido", transaction.merchant)
        assertEquals("3456", transaction.accountLast4)
        assertEquals("234567890123", transaction.refId)
        assertEquals("2026-05-21", transaction.date)
        assertEquals("15:30", transaction.time)
        assertEquals("Transport", transaction.category)
    }

    @Test
    fun testKotakDebitSms() {
        val sms = "Kotak Acct XX5678 debited Rs 300.00. Paid to UBER. UPI Ref No: 345678901234. 21-May-26 14:15."
        val transaction = SmsParser.parseSMS(sms)
        assertNotNull(transaction)
        assertEquals(300.00, transaction!!.amount, 0.0)
        assertEquals("Uber", transaction.merchant)
        assertEquals("5678", transaction.accountLast4)
        assertEquals("345678901234", transaction.refId)
        assertEquals("2026-05-21", transaction.date)
        assertEquals("14:15", transaction.time)
        assertEquals("Transport", transaction.category)
    }

    @Test
    fun testStarbucksGenericSms() {
        val sms = "Rs 50.00 paid from A/c 9012. Spent at STARBUCKS. Ref: 456789012345."
        val transaction = SmsParser.parseSMS(sms)
        assertNotNull(transaction)
        assertEquals(50.00, transaction!!.amount, 0.0)
        assertEquals("Starbucks", transaction.merchant)
        assertEquals("9012", transaction.accountLast4)
        assertEquals("456789012345", transaction.refId)
        assertEquals("Food & Dining", transaction.category)
    }

    @Test
    fun testGrowwGenericSms() {
        val sms = "Transferred Rs.1,000.00 to GROWW on 21/05/26. Ref: 567890123456."
        val transaction = SmsParser.parseSMS(sms)
        assertNotNull(transaction)
        assertEquals(1000.00, transaction!!.amount, 0.0)
        assertEquals("Groww", transaction.merchant)
        assertEquals("567890123456", transaction.refId)
        assertEquals("Investments", transaction.category)
    }

    @Test
    fun testSbiCardSpentSms() {
        val sms = "Your SBI card ending 1234 spent Rs 2000.00 at AMAZON on 21-May-26."
        val transaction = SmsParser.parseSMS(sms)
        assertNotNull(transaction)
        assertEquals(2000.00, transaction!!.amount, 0.0)
        assertEquals("Amazon", transaction.merchant)
        assertEquals("1234", transaction.accountLast4)
        assertEquals("Shopping", transaction.category)
    }

    @Test
    fun testNetflixSubscriptionSms() {
        val sms = "Netflix renewal: Rs. 199.00 debited from a/c 1111 on 21-05-26. Ref 987654321098."
        val transaction = SmsParser.parseSMS(sms)
        assertNotNull(transaction)
        assertEquals(199.00, transaction!!.amount, 0.0)
        assertEquals("Netflix", transaction.merchant)
        assertEquals("1111", transaction.accountLast4)
        assertEquals("987654321098", transaction.refId)
        assertEquals("Subscriptions", transaction.category)
        assertTrue(transaction.isRecurring)
    }

    @Test
    fun testNonDebitSms() {
        val sms = "Your verification code is 123456. Do not share."
        val transaction = SmsParser.parseSMS(sms)
        assertNull(transaction)
    }
}
