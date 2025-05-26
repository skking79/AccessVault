# AccessVault – Biometric Password Manager

**AccessVault** is a secure and user-friendly Android application that helps users store, manage, and autofill their credentials using advanced biometric authentication and encryption techniques.

## 🚀 Features
- 🔐 **Biometric Authentication** – Supports fingerprint and face recognition for secure access.  
- 🧠 **Encrypted Credential Storage** – Uses **AES encryption** with the **Android Keystore** for protecting sensitive data.  
- ✍️ **Credential Management** – Add, edit, delete, and copy stored login credentials.  
- ⚙️ **Autofill Support** – Integrates with the **Android Autofill Framework** to autofill credentials across apps and browsers.  

## 🛠️ Built With
- Java
- Android Studio
- SQLite (with encryption)
- AndroidX Biometric Library
- Android Autofill Service API

## 📱 Usage
Once a user saves a credential:
1. AccessVault encrypts and stores it securely.
2. When logging into other apps, the system triggers **Autofill**, and AccessVault suggests saved credentials.
3. The user authenticates with biometrics to autofill securely.

## 📄 License
This project is licensed under the **MIT License**. See the [LICENSE](./LICENSE) file for details.
