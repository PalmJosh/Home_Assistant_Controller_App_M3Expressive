# Home Assistant Controller

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-Design-purple?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-blue?style=for-the-badge)

**A lightweight, minimalist Android application designed to act as a focused Home Assistant controller.**

Built with **Material 3 Expressive Design**, this app serves a single primary purpose: controlling your lights efficiently.

While standard Home Assistant companion apps offer full dashboard capabilities, *Home Assistant Controller* strips away the complexity to provide immediate access to lighting and sensor data. It is perfect for family members who aren't tech-heavy or anyone who prefers a stylish, minimalist interface.

Avaiable Languages: English, German, Polish and Russian

---

## Screenshots

<div align="center">
  <img src="https://github.com/user-attachments/assets/08319cc9-b32b-4738-8f26-8942e6215cd3" width="30%" />
  <img src="https://github.com/user-attachments/assets/3e768307-671b-4797-941d-2c042bae094d" width="30%" />
  <img src="https://github.com/user-attachments/assets/5edacf9c-146e-48db-98ba-6243dad091c1" width="30%" />
</div>
<br/>
<div align="center">
  <img src="https://github.com/user-attachments/assets/01d90dbd-ad47-4d7a-92eb-32eec7f88a9d" width="30%" />
  <img src="https://github.com/user-attachments/assets/2ad739f3-3582-4121-a4c1-e75139b1515e" width="30%" />
  <img src="https://github.com/user-attachments/assets/621b84a0-93da-4810-bc91-596abc7caf42" width="30%" />
</div>

---

## Features

### Light Control
* **Room Management:** Toggle entire rooms on or off with a single tap.
* **Granular Control:** Manage individual light entities within a specific room.
* **Visual Adjustments:** Intuitive sliders for brightness, RGB color selection, and Color Temperature.

### Monitoring
* **Sensor Dashboard:** View the real-time status of all active sensors connected to your Home Assistant instance.

### User Interface
* **Material 3 Expressive:** Utilizes the latest Android design guidelines, featuring large distinct typography, fluid animations, and dynamic coloring based on the device theme.
* **Focus Mode:** A clutter-free interface designed for rapid interaction.

---

## Prerequisites

To use this application, you need:

1.  **Home Assistant Instance:** A running installation of Home Assistant (Core, Container, or OS).
2.  **Access:** Credentials to log in to your instance.
3.  **Android Device:** Android 12 or higher (recommended for full Material You dynamic coloring support).

---

## Installation & Configuration

### 1. Download
You can download the latest signed APK from the [Releases section](../../releases) of this repository.

### 2. Connect
Upon launching the application for the first time, you will be prompted to connect to your Home Assistant instance.

1.  **Base URL:** Enter the full URL of your instance.
    * *Example:* `http://192.168.1.5:8123` or your Nabu Casa URL.
2.  **Login:** Press the Login button. This will direct you to the official Home Assistant login page to authenticate and retrieve a token.

> [!NOTE]
> **Security:** The app stores the Access Token locally in `EncryptedSharedPreferences`. Your login credentials (username/password) are **not** stored by the app.

---

## Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Networking:** Retrofit & OkHttp
* **Async:** Kotlin Coroutines & Flow

---

## Contributing

Contributions are welcome! If you find a bug or want to suggest a feature that aligns with the **minimalist philosophy** of the app, please open an issue to discuss it before submitting a pull request.

---

## License

Based on the Home Assistant API by the Open Home Foundation.

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
