# AutoRescue

## Phone-First Crash Detection and Emergency Response Prototype

AutoRescue is an Android-based emergency response prototype designed to reduce the time between a possible vehicle crash and emergency notification.

The prototype uses smartphone sensors to detect unusual motion patterns and provides an emergency response workflow when a crash is suspected.

---

## Features

- Accelerometer-based motion monitoring
- Gyroscope-based rotation monitoring
- GPS location acquisition
- Local deterministic crash-risk assessment
- Automatic crash-suspicion workflow
- 10-second emergency countdown
- Driver response options
- Emergency risk assessment
- Local incident history
- SMS emergency notification support
- 112 emergency dialer integration
- Foreground monitoring service
- Offline-first local incident storage
- Simulated crash testing for demonstrations

---

## System Architecture

```text
                iQOO / Android Phone
                       |
          +------------+------------+
          |            |            |
   Accelerometer   Gyroscope       GPS
          |            |            |
          +------------+------------+
                       |
                Sensor Layer
                       |
                Risk Engine
                       |
              Crash Suspected
                       |
              10-Second Countdown
                       |
          +------------+------------+
          |            |            |
      I'M SAFE   CONTINUE EMERGENCY
                       |
                  NO RESPONSE
                       |
               Incident Assessment
                       |
          +------------+------------+
          |            |            |
       GPS          SMS           112
    Location      Contact       Dialer
                       |
                Incident History