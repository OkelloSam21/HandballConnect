# <img src="app/src/main/res/drawable/handball_logo.png" height="75"> HandballConnect

HandballConnect is a comprehensive social and tactical platform built specifically for handball enthusiasts. The application brings together players, coaches, and fans to create a vibrant community centered around the sport of handball.



## Features

### 🔐 Authentication
- Secure email/password registration and login
- Profile customization with position and experience level
- Profile image uploading with local storage optimization

### 📱 Social Feed
- Create and share posts with text and images
- Like and comment on community posts
- Official announcements from administrators
- Image compression and local storage for offline viewing

### 💬 Messaging
- Private conversations between users
- Support for text and image messages
- Conversation list with unread message indicators
- User discovery for initiating new conversations

### 📋 Tactics Board
- Interactive tactics creation and editing
- Customizable player positions and movement arrows
- Preset formations (6-0, 5-1, 3-2-1, Fast Break)
- Ability to share tactics with the community
- Save and manage private tactics collections

### 👑 Admin Features
- User management (promote/demote admins, disable accounts)
- Create official announcements
- Platform moderation capabilities

## Architecture

HandballConnect is built using modern Android development practices and follows a clean architecture approach:

### 📐 Architectural Pattern
- **MVVM (Model-View-ViewModel)** architecture for clean separation of concerns
- **Repository Pattern** for data operations abstraction
- **Dependency Injection** with Hilt for maintainable and testable code

### 📊 Data Flow
```
UI Layer (Composables) ↔ ViewModels ↔ Repositories ↔ Data Sources (Firebase/Local Storage)
```

### 🧩 Key Components
- **UI Layer**: Jetpack Compose screens and components
- **ViewModels**: State management and business logic
- **Repositories**: Abstracted data operations
- **Data Models**: Kotlin data classes representing domain entities
- **Storage Manager**: Local image storage and compression

## Tech Stack

HandballConnect leverages modern Android technologies:

### 📱 UI
- **Jetpack Compose**: Declarative UI toolkit for modern Android UI
- **Material 3**: Latest Material Design components and theming
- **Coil**: Image loading and caching

### 🛠️ Architecture Components
- **ViewModel**: UI state management and lifecycle awareness
- **Flow**: Reactive data streams
- **Hilt**: Dependency injection
- **Navigation Compose**: In-app navigation

### 🔥 Backend
- **Firebase Authentication**: User authentication
- **Firestore**: NoSQL database for social features
- **Firebase Storage**: Cloud storage for images
- **Firebase Realtime Database**: Real-time messaging

### 💾 Local Storage
- **Room**: SQLite database abstraction layer (prepared for offline capabilities)
- **DataStore**: Preferences storage
- **Custom Image Storage**: Local image caching and compression

### 🧰 Other Libraries
- **Coroutines**: Asynchronous programming
- **Accompanist**: Compose UI utilities

## Screenshots

| Authentication | Feed | Messages | Tactics |
|:---:|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/e144d31e-1fbe-48e5-bbe5-990f445e02b8" height="150"> | <img src="https://github.com/user-attachments/assets/2782c196-e3eb-406c-ae4d-a3b5d02aa48f"  height="150"> | <img src="https://github.com/user-attachments/assets/671d1da4-0cdc-4899-91eb-005a3abefad4"  height="150"> | <img src="https://github.com/user-attachments/assets/70be5205-e8f8-4eda-adc7-53cdfc40bb4c"  height="150"> |
| <img src="https://github.com/user-attachments/assets/7b551dc7-7678-4f91-8b17-d0f77d40c12e" height="150"> | <img src="https://github.com/user-attachments/assets/b6e69970-1f56-45f8-a41b-99cc1043687f"  height="150"> | <img src="https://github.com/user-attachments/assets/e144d31e-1fbe-48e5-bbe5-990f445e02b8"  height="150"> | <img src="https://github.com/user-attachments/assets/"  height="150"> |




## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.
