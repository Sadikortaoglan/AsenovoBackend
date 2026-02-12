# Quick Fix - Configuration Files
## Copy-Paste Ready Configurations

---

## 1. package.json (Critical Versions)

```json
{
  "name": "sara-asansor-mobile",
  "version": "1.0.0",
  "main": "node_modules/expo/AppEntry.js",
  "scripts": {
    "start": "expo start -c",
    "android": "expo start --android",
    "ios": "expo start --ios",
    "web": "expo start --web"
  },
  "dependencies": {
    "expo": "~51.0.0",
    "expo-status-bar": "~1.12.1",
    "react": "18.2.0",
    "react-native": "0.74.5",
    "react-native-safe-area-context": "4.10.1",
    "react-native-screens": "~3.31.1",
    "@react-navigation/native": "^6.1.9",
    "@react-navigation/bottom-tabs": "^6.5.11",
    "@react-navigation/drawer": "^6.6.6",
    "react-native-reanimated": "~3.10.1",
    "react-native-gesture-handler": "~2.16.1",
    "axios": "^1.6.2",
    "@react-native-async-storage/async-storage": "1.21.0",
    "expo-secure-store": "~13.0.1",
    "date-fns": "^2.30.0",
    "react-native-paper": "^5.11.1"
  },
  "devDependencies": {
    "@babel/core": "^7.24.0",
    "@types/react": "~18.2.79",
    "@types/react-native": "~0.73.0",
    "typescript": "^5.3.3"
  },
  "private": true
}
```

---

## 2. babel.config.js (CRITICAL - Must be exactly this)

```javascript
module.exports = function(api) {
  api.cache(true);
  return {
    presets: ['babel-preset-expo'],
    plugins: [
      'react-native-reanimated/plugin',
    ],
  };
};
```

**IMPORTANT:**
- Reanimated plugin MUST be last
- NO other plugins
- NO Hermes config here

---

## 3. app.json (iOS Crash Fix)

```json
{
  "expo": {
    "name": "SaraAsansor Mobile",
    "slug": "sara-asansor-mobile",
    "version": "1.0.0",
    "orientation": "portrait",
    "icon": "./assets/icon.png",
    "userInterfaceStyle": "light",
    "splash": {
      "image": "./assets/splash.png",
      "resizeMode": "contain",
      "backgroundColor": "#ffffff"
    },
    "assetBundlePatterns": ["**/*"],
    "ios": {
      "supportsTablet": true,
      "bundleIdentifier": "com.saraasansor.mobile",
      "jsEngine": "hermes",
      "newArchEnabled": false
    },
    "android": {
      "adaptiveIcon": {
        "foregroundImage": "./assets/adaptive-icon.png",
        "backgroundColor": "#ffffff"
      },
      "package": "com.saraasansor.mobile",
      "jsEngine": "hermes",
      "newArchEnabled": false
    },
    "web": {
      "favicon": "./assets/favicon.png"
    },
    "plugins": [
      "expo-secure-store"
    ],
    "extra": {
      "apiUrl": "http://192.168.1.201:8080/api"
    }
  }
}
```

**Key Points:**
- `newArchEnabled: false` (fixes crash)
- `apiUrl` in `extra` for device access

---

## 4. API Client Base URL Fix

**In `src/api/client.ts`:**

```typescript
import Constants from 'expo-constants';

const getBaseUrl = () => {
  if (__DEV__) {
    // Use device IP for real devices
    return Constants.expoConfig?.extra?.apiUrl || 'http://192.168.1.201:8080/api';
  }
  return 'http://51.21.3.85:8080/api';
};

const BASE_URL = getBaseUrl();
```

---

## 5. Login Payload Fix

**In `src/services/auth.service.ts` - login method:**

```typescript
async login(credentials: LoginRequest): Promise<LoginResponse> {
  // EXACT payload format backend expects
  const payload = {
    username: credentials.username.trim(),
    password: credentials.password,
  };

  const response = await apiClient.post<ApiResponse<LoginResponse>>(
    '/auth/login',
    payload,  // Send exactly this object
    {
      headers: {
        'Content-Type': 'application/json',  // Explicit header
      },
    }
  );

  // ... rest of login logic
}
```

---

## QUICK COMMANDS

```bash
# 1. Clean
rm -rf node_modules .expo ios android package-lock.json

# 2. Install
npm install

# 3. Start with cache clear
npx expo start -c

# 4. iOS Simulator
npx expo start --ios --clear

# 5. Real Device
# Scan QR code, or:
npx expo start --tunnel
```

---

## VERIFICATION

After fixes, check:

1. **iOS Simulator:**
   - ✅ No crash on launch
   - ✅ Login screen appears

2. **Real Device:**
   - ✅ App opens
   - ✅ Check console: API URL = `http://192.168.1.201:8080/api`
   - ✅ Login request shows:
     ```json
     {
       "username": "...",
       "password": "..."
     }
     ```
   - ✅ Response shows backend error message if 400

---

**Apply these 5 fixes and restart with `npx expo start -c`**
