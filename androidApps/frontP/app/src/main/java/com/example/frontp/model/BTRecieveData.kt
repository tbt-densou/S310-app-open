package com.example.frontp.model

import java.sql.Timestamp

data class BTRecieveData(val deviceName: String?, val deviceAddress: String, val data: String, val timestamp: Timestamp)
