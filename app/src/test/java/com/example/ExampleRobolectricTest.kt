package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TaxiGo", appName)
  }

  @Test
  fun `inDrive roles and fare offer bounds test`() {
    val passengerRole = "passenger"
    val driverRole = "driver"
    val validRoles = listOf("passenger", "driver")

    assertEquals(true, validRoles.contains(passengerRole))
    assertEquals(true, validRoles.contains(driverRole))

    val basePrice = 14.0
    val offeredPrice = (basePrice - 2.0).coerceAtLeast(5.0)
    assertEquals(12.0, offeredPrice, 0.01)
  }
}
