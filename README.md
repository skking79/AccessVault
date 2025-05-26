# 🔐 AccessVault – Biometric Password Manager

A secure Android app that stores credentials using **AES encryption**, provides **biometric authentication**, and supports **Android Autofill Framework** for seamless login across apps.

## 📱 Features

- ✅ **Biometric Login**: Fingerprint or Face ID unlock support.
- 🔒 **Encrypted Storage**: Uses AES/CBC + Android Keystore to securely store usernames and passwords.
- 🧩 **Autofill Service**: Auto-fills credentials into external apps like browsers or login forms.
- 🖊️ **Add/Edit/Delete Credentials**: Full CRUD operations with database conflict handling.
- 📋 **Copy Username/Password**: Inline buttons to copy data to clipboard.
- 🗂️ **Custom XML Layouts**: Card-based UI for credentials with inline actions.

## 🛠 Built With

- **Java** – Android app logic
- **Android Studio** – IDE
- **SQLite + AES Encryption** – Secure local storage
- **AndroidX Biometric Library** – For biometric authentication
- **Android Autofill Framework** – For auto-login support
- **Material Design** – Modern UI components

## 🧾 How It Works

1. User authenticates via **fingerprint or face recognition**
2. Vault shows all saved credentials
3. Tap icons to **copy username or password**
4. Long press to **Edit or Delete** a credential
5. When logging into other apps, system prompts to use **AccessVault autofill**

---

## 📸 Screenshots


### 1. Biometric Login Screen  
![image](https://github.com/user-attachments/assets/4b778583-e32d-496c-ac55-4f6d1f9647f1)


### 2. Main Page View  
![image](https://github.com/user-attachments/assets/24c849c0-5b3d-4570-82fd-80c872c80ef8)


### 3. Add Credential Form  
![image](https://github.com/user-attachments/assets/9bc6fe36-c40c-459b-9c88-da12c91e72a2)

### 4. Saved Credential View  
![image](https://github.com/user-attachments/assets/984c8547-9205-4de2-9cb6-596fb8675125)

### 4. Edit Credential View
![image](https://github.com/user-attachments/assets/eb63337b-2ef7-441d-b148-be4bc074d96d)


---

## 📁 Core Files

| File | Description |
|------|-------------|
| `LoginActivity.java` | Handles biometric unlock before accessing the vault |
| `VaultActivity.java` | Main screen showing card-based credential list |
| `AddCredentialActivity.java` | UI for adding/editing credentials with validation |
| `DBHelper.java` | Manages encrypted SQLite operations |
| `MyAutofillService.java` | Implements Android Autofill Framework |
| `AutofillHelper.java` | Parses assist structures to detect fields in external apps |
| `CredentialsAdapter.java` | Binds credentials to the list view |
| `Credential.java` | Model class representing each credential |

---

## 🧪 How to Build & Run

### Requirements
- Android Studio 
- Target SDK: 35 (Android U)
- Java 17+
- Physical/Virtual(From AndroidStudio) device with fingerprint/face unlock


## 📱 Usage
Once a user saves a credential:
1. AccessVault encrypts and stores it securely.
2. When logging into other apps, the system triggers **Autofill**, and AccessVault suggests saved credentials.
3. The user authenticates with biometrics to autofill securely.

## 📄 License
This project is licensed under the **MIT License**. See the [LICENSE](./LICENSE) file for details.
