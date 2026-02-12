# iOS Crash & Login 400 Fix Guide
## React Native Expo App - Production Fixes

---

## ISSUE 1: iOS Simulator Crash (C++ Exception)

### Root Cause
- Incompatible Hermes/Reanimated setup
- React 19 experimental version
- Babel config issues
- Expo SDK compatibility

### Fix: Update package.json

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
    "react-native-paper": "^5.11.1",
    "react-native-vector-icons": "^10.0.3"
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

**Key Changes:**
- ✅ React 18.2.0 (NOT 19)
- ✅ Expo SDK 51 (stable)
- ✅ Reanimated 3.10.1 (compatible)
- ✅ React Native 0.74.5 (stable)

---

### Fix: Update app.json

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

**Key Changes:**
- ✅ `newArchEnabled: false` (disable New Architecture)
- ✅ Hermes enabled explicitly
- ✅ API URL in extra config

---

### Fix: babel.config.js (CRITICAL)

```javascript
module.exports = function(api) {
  api.cache(true);
  return {
    presets: ['babel-preset-expo'],
    plugins: [
      // Reanimated plugin MUST be last
      'react-native-reanimated/plugin',
    ],
  };
};
```

**CRITICAL RULES:**
- ✅ Only `react-native-reanimated/plugin`
- ✅ MUST be the LAST plugin
- ✅ NO other experimental plugins
- ✅ NO Hermes-specific plugins

---

## ISSUE 2: Login HTTP 400 Error

### Root Cause
- Incorrect request payload format
- Missing Content-Type header
- Wrong BASE_URL for device

### Fix: API Client Configuration

**File: `src/api/client.ts`**

```typescript
import axios, { AxiosInstance, AxiosError } from 'axios';
import * as SecureStore from 'expo-secure-store';
import Constants from 'expo-constants';

// Use config from app.json or environment
const getBaseUrl = () => {
  // Development: Use device IP
  if (__DEV__) {
    return Constants.expoConfig?.extra?.apiUrl || 'http://192.168.1.201:8080/api';
  }
  // Production
  return 'http://51.21.3.85:8080/api';
};

const BASE_URL = getBaseUrl();

const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// Request interceptor: Add access token
apiClient.interceptors.request.use(
  async (config) => {
    // Ensure Content-Type is set
    if (!config.headers['Content-Type']) {
      config.headers['Content-Type'] = 'application/json';
    }

    const token = await SecureStore.getItemAsync('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Log request for debugging (remove in production)
    if (__DEV__) {
      console.log('API Request:', {
        method: config.method?.toUpperCase(),
        url: config.url,
        baseURL: config.baseURL,
        headers: config.headers,
        data: config.data,
      });
    }
    
    return config;
  },
  (error) => {
    console.error('API Request Error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor: Handle token refresh + error logging
apiClient.interceptors.response.use(
  (response) => {
    if (__DEV__) {
      console.log('API Response:', {
        status: response.status,
        url: response.config.url,
        data: response.data,
      });
    }
    return response;
  },
  async (error: AxiosError) => {
    // Log error response for debugging
    if (error.response) {
      console.error('API Error Response:', {
        status: error.response.status,
        statusText: error.response.statusText,
        url: error.config?.url,
        data: error.response.data,
        headers: error.response.headers,
      });
    } else if (error.request) {
      console.error('API Request Failed (No Response):', {
        url: error.config?.url,
        message: error.message,
      });
    } else {
      console.error('API Error:', error.message);
    }

    const originalRequest = error.config as any;

    // Handle 401: Token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = await SecureStore.getItemAsync('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const response = await axios.post<{ data: any }>(
          `${BASE_URL}/auth/refresh`,
          { refreshToken },
          {
            headers: {
              'Content-Type': 'application/json',
            },
          }
        );

        const { accessToken, refreshToken: newRefreshToken } = response.data.data!;

        await SecureStore.setItemAsync('accessToken', accessToken);
        await SecureStore.setItemAsync('refreshToken', newRefreshToken);

        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        await SecureStore.deleteItemAsync('accessToken');
        await SecureStore.deleteItemAsync('refreshToken');
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

---

### Fix: Auth Service - Login Method

**File: `src/services/auth.service.ts`**

```typescript
import apiClient from '../api/client';
import { LoginRequest, LoginResponse, ApiResponse } from '../types/api';
import * as SecureStore from 'expo-secure-store';

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    // CRITICAL: Ensure payload matches backend exactly
    const payload = {
      username: credentials.username.trim(),
      password: credentials.password,
    };

    console.log('Login Request Payload:', payload);
    console.log('Login Request URL:', `${apiClient.defaults.baseURL}/auth/login`);

    try {
      const response = await apiClient.post<ApiResponse<LoginResponse>>(
        '/auth/login',
        payload,
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      console.log('Login Response:', {
        success: response.data.success,
        message: response.data.message,
        hasData: !!response.data.data,
      });

      if (response.data.success && response.data.data) {
        const { accessToken, refreshToken } = response.data.data;
        await SecureStore.setItemAsync('accessToken', accessToken);
        await SecureStore.setItemAsync('refreshToken', refreshToken);
        return response.data.data;
      }

      // Extract error message from response
      const errorMessage = response.data.message || 
                          response.data.errors?.join(', ') || 
                          'Login failed';
      throw new Error(errorMessage);
    } catch (error: any) {
      // Log detailed error
      if (error.response) {
        const errorData = error.response.data;
        console.error('Login Error Response:', {
          status: error.response.status,
          statusText: error.response.statusText,
          data: errorData,
          message: errorData?.message,
          errors: errorData?.errors,
        });
        
        // Extract backend error message
        const backendMessage = errorData?.message || 
                              errorData?.errors?.join(', ') || 
                              `HTTP ${error.response.status}: ${error.response.statusText}`;
        throw new Error(backendMessage);
      } else if (error.request) {
        console.error('Login Request Failed:', {
          message: error.message,
          url: error.config?.url,
        });
        throw new Error('Network error: Could not reach server');
      } else {
        console.error('Login Error:', error.message);
        throw error;
      }
    }
  },

  // ... other methods
};
```

---

### Fix: Login Screen Error Display

**File: `src/screens/auth/LoginScreen.tsx`**

```typescript
import React, { useState } from 'react';
import { View, StyleSheet, Alert, KeyboardAvoidingView, Platform } from 'react-native';
import { TextInput, Button, Text, Surface } from 'react-native-paper';
import { useAuth } from '../../context/AuthContext';

export const LoginScreen: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const { login } = useAuth();

  const handleLogin = async () => {
    // Clear previous error
    setErrorMessage(null);

    // Validation
    if (!username || !username.trim()) {
      setErrorMessage('Kullanıcı adı gereklidir');
      return;
    }

    if (!password) {
      setErrorMessage('Şifre gereklidir');
      return;
    }

    setLoading(true);
    try {
      await login(username.trim(), password);
      // Navigation handled by AuthContext
    } catch (error: any) {
      // Show backend error message
      const message = error.message || 'Giriş yapılamadı';
      setErrorMessage(message);
      
      // Also show alert for visibility
      Alert.alert(
        'Giriş Hatası',
        message,
        [{ text: 'Tamam' }]
      );
      
      console.error('Login failed:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}
    >
      <Surface style={styles.surface}>
        <Text variant="headlineMedium" style={styles.title}>
          SaraAsansor
        </Text>
        
        {errorMessage && (
          <View style={styles.errorContainer}>
            <Text style={styles.errorText}>{errorMessage}</Text>
          </View>
        )}

        <TextInput
          label="Kullanıcı Adı"
          value={username}
          onChangeText={(text) => {
            setUsername(text);
            setErrorMessage(null); // Clear error on input change
          }}
          mode="outlined"
          style={styles.input}
          autoCapitalize="none"
          autoCorrect={false}
          editable={!loading}
        />
        
        <TextInput
          label="Şifre"
          value={password}
          onChangeText={(text) => {
            setPassword(text);
            setErrorMessage(null); // Clear error on input change
          }}
          mode="outlined"
          secureTextEntry
          style={styles.input}
          editable={!loading}
        />
        
        <Button
          mode="contained"
          onPress={handleLogin}
          loading={loading}
          disabled={loading || !username.trim() || !password}
          style={styles.button}
        >
          Giriş Yap
        </Button>

        {/* Debug info in dev mode */}
        {__DEV__ && (
          <View style={styles.debugContainer}>
            <Text style={styles.debugText}>
              API URL: {require('../../api/client').default.defaults.baseURL}
            </Text>
          </View>
        )}
      </Surface>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  surface: {
    padding: 20,
    borderRadius: 8,
    elevation: 2,
  },
  title: {
    textAlign: 'center',
    marginBottom: 30,
    fontWeight: 'bold',
  },
  errorContainer: {
    backgroundColor: '#ffebee',
    padding: 12,
    borderRadius: 4,
    marginBottom: 16,
    borderLeftWidth: 4,
    borderLeftColor: '#f44336',
  },
  errorText: {
    color: '#c62828',
    fontSize: 14,
  },
  input: {
    marginBottom: 15,
  },
  button: {
    marginTop: 10,
  },
  debugContainer: {
    marginTop: 20,
    padding: 10,
    backgroundColor: '#e3f2fd',
    borderRadius: 4,
  },
  debugText: {
    fontSize: 12,
    color: '#1976d2',
  },
});
```

---

## CLEANUP & REINSTALL STEPS

### Step 1: Clean Project

```bash
# Navigate to mobile app directory
cd /path/to/sara-asansor-mobile

# Remove build artifacts
rm -rf node_modules
rm -rf .expo
rm -rf ios
rm -rf android
rm -rf .expo-shared
rm package-lock.json
rm yarn.lock

# Clear cache
npm cache clean --force
# or
yarn cache clean
```

### Step 2: Update Configuration Files

1. Update `package.json` with versions above
2. Update `app.json` with config above
3. Update `babel.config.js` with config above

### Step 3: Reinstall Dependencies

```bash
# Install dependencies
npm install
# or
yarn install

# Verify no peer dependency warnings
npm ls
```

### Step 4: Update API Client Files

1. Update `src/api/client.ts` with code above
2. Update `src/services/auth.service.ts` with code above
3. Update `src/screens/auth/LoginScreen.tsx` with code above

### Step 5: Restart Development Server

```bash
# Clear cache and start
npx expo start -c

# For iOS simulator
npx expo start --ios --clear

# For Android
npx expo start --android --clear
```

---

## VERIFICATION CHECKLIST

### iOS Simulator
- [ ] App launches without crash
- [ ] No C++ exception errors
- [ ] Login screen appears
- [ ] Network requests work

### Real Device (iPhone 15 Pro)
- [ ] App installs and opens
- [ ] Login screen appears
- [ ] API URL shows correct device IP (192.168.1.201:8080)
- [ ] Login request sends correct payload:
  ```json
  {
    "username": "...",
    "password": "..."
  }
  ```
- [ ] Content-Type header is `application/json`
- [ ] Backend error messages display correctly
- [ ] Login succeeds with valid credentials

### Debug Logs
Check console for:
- ✅ API Request logs showing correct URL and payload
- ✅ API Response logs showing success/error
- ✅ Error response data logged on 400
- ✅ No React/Reanimated warnings

---

## TROUBLESHOOTING

### If iOS still crashes:

1. **Disable Hermes temporarily:**
   ```json
   // app.json
   "ios": {
     "jsEngine": "jsc"  // Instead of "hermes"
   }
   ```

2. **Check Metro bundler cache:**
   ```bash
   npx expo start -c --reset-cache
   ```

3. **Check React version:**
   ```bash
   npm ls react react-native
   ```
   Should show React 18.2.0, NOT 19.x

### If Login still returns 400:

1. **Verify backend is accessible:**
   ```bash
   curl -X POST http://192.168.1.201:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"test"}'
   ```

2. **Check backend CORS allows device IP:**
   - Backend should allow `http://192.168.1.201:*` in CORS config

3. **Verify exact payload format:**
   - Backend expects: `{"username": "...", "password": "..."}`
   - NO extra fields
   - NO trailing commas

4. **Check network connectivity:**
   - Device and computer must be on same network
   - Firewall not blocking port 8080

---

## EXPECTED BEHAVIOR AFTER FIX

### iOS Simulator
- ✅ App launches successfully
- ✅ No crashes or exceptions
- ✅ Login screen functional
- ✅ Can connect to backend (use computer's IP if testing)

### Real Device
- ✅ App installs and opens
- ✅ Login screen shows
- ✅ Correct API URL (192.168.1.201:8080)
- ✅ Login request sends correct JSON payload
- ✅ Backend error messages display if login fails
- ✅ Login succeeds with valid credentials

---

**END OF FIX GUIDE**

*Apply these fixes in order, then test on both simulator and device. All configuration files are production-ready and compatible with Expo SDK 51.*
