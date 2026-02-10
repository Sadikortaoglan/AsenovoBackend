# SaraAsansor Mobile App
## React Native + Expo + TypeScript Implementation Guide

**Version:** 1.0  
**Backend API:** Spring Boot REST API  
**Platforms:** iOS + Android  
**Language:** Turkish UI  
**Target Audience:** Mobile Engineers + Backend Integrators

---

## 1. PROJECT OVERVIEW

### App Purpose
SaraAsansor Mobile is a production-ready React Native mobile application for managing elevator maintenance, inspections, parts inventory, and related operations. The app integrates with the existing Spring Boot backend API.

### Technology Stack
- **Framework:** React Native (Expo)
- **Language:** TypeScript
- **Navigation:** React Navigation (Tab or Drawer based)
- **State Management:** Context API / Redux Toolkit (choose one)
- **HTTP Client:** Axios
- **Storage:** AsyncStorage / SecureStore (for tokens)
- **UI Components:** React Native Paper or NativeBase
- **Date Handling:** date-fns or moment.js

### Key Features
- ✅ JWT Authentication with Refresh Token
- ✅ Dashboard with summary statistics
- ✅ Elevator management (CRUD)
- ✅ Warnings with grouped view
- ✅ Inspections tracking
- ✅ Maintenance records with payment status
- ✅ Parts/Stock inventory
- ✅ Offers management
- ✅ User management (PATRON only)
- ✅ Offline caching (last successful list data)

### Base URL Configuration
```typescript
// Development
BASE_URL = 'http://localhost:8080/api'

// Production
BASE_URL = 'http://51.21.3.85:8080/api'

// User-configurable in Settings
```

### Authentication Flow
1. User logs in → receives `accessToken` (15 min) + `refreshToken` (7 days)
2. `accessToken` stored in memory / AsyncStorage
3. `refreshToken` stored securely (SecureStore)
4. When `accessToken` expires → auto-refresh using `refreshToken`
5. If refresh fails → redirect to Login

---

## 2. BACKEND CONTRACT MAP

### Base Response Format
All endpoints return this structure:
```json
{
  "success": boolean,
  "message": string | null,
  "data": T | null,
  "errors": string[] | null
}
```

### Authentication Endpoints

| Method | Endpoint | Auth | Request Body | Response |
|--------|----------|------|--------------|----------|
| POST | `/auth/login` | ❌ | `{ username, password }` | `LoginResponse` |
| POST | `/auth/register` | ❌ | `{ username, password, role }` | `LoginResponse` |
| POST | `/auth/refresh` | ❌ | `{ refreshToken }` | `LoginResponse` |
| POST | `/auth/logout` | ✅ | `{ refreshToken }` (optional) | `void` |

### Dashboard Endpoints

| Method | Endpoint | Auth | Query Params | Response |
|--------|----------|------|--------------|----------|
| GET | `/dashboard/summary` | ✅ | - | `DashboardSummaryDto` |

### Elevator Endpoints

| Method | Endpoint | Auth | Request Body | Response |
|--------|----------|------|--------------|----------|
| GET | `/elevators` | ✅ | - | `ElevatorDto[]` |
| GET | `/elevators/{id}` | ✅ | - | `ElevatorDto` |
| GET | `/elevators/{id}/status` | ✅ | - | `ElevatorStatusDto` |
| POST | `/elevators` | ✅ | `ElevatorDto` | `ElevatorDto` |
| PUT | `/elevators/{id}` | ✅ | `ElevatorDto` | `ElevatorDto` |
| DELETE | `/elevators/{id}` | ✅ | - | `void` |

**Sorting:** Lists are sorted by `id ASC` (stable order after updates).

### Warning Endpoints

| Method | Endpoint | Auth | Query Params | Response |
|--------|----------|------|--------------|----------|
| GET | `/warnings` | ✅ | `type?` (EXPIRED/WARNING) | `WarningDto[]` |
| GET | `/warnings/grouped` | ✅ | `type?` (EXPIRED/WARNING) | `WarningGroupDto[]` |

**Grouped Response:**
- Groups by `buildingName + address`
- EXPIRED buildings first, then WARNING
- Elevators within group sorted by `maintenanceEndDate ASC`

### Inspection Endpoints

| Method | Endpoint | Auth | Request Body | Response |
|--------|----------|------|--------------|----------|
| GET | `/inspections` | ✅ | - | `InspectionDto[]` |
| GET | `/inspections/{id}` | ✅ | - | `InspectionDto` |
| GET | `/inspections/elevator/{elevatorId}` | ✅ | - | `InspectionDto[]` |
| POST | `/inspections` | ✅ | `InspectionDto` | `InspectionDto` |
| PUT | `/inspections/{id}` | ✅ | `InspectionDto` | `InspectionDto` |
| DELETE | `/inspections/{id}` | ✅ | - | `void` |

**Note:** `result` field: `"PASSED"` or `"FAILED"` (string, uppercase).

### Maintenance Endpoints

| Method | Endpoint | Auth | Query Params | Request Body | Response |
|--------|----------|------|--------------|--------------|----------|
| GET | `/maintenances` | ✅ | `paid?`, `dateFrom?`, `dateTo?` | - | `MaintenanceDto[]` |
| GET | `/maintenances/summary` | ✅ | `month?` (optional) | - | `MaintenanceSummaryDto` |
| GET | `/maintenances/elevator/{elevatorId}` | ✅ | - | - | `MaintenanceDto[]` |
| GET | `/maintenances/{id}` | ✅ | - | - | `MaintenanceDto` |
| POST | `/maintenances` | ✅ | - | `MaintenanceDto` | `MaintenanceDto` |
| PUT | `/maintenances/{id}` | ✅ | - | `MaintenanceDto` | `MaintenanceDto` |
| DELETE | `/maintenances/{id}` | ✅ | - | - | `void` |
| POST | `/maintenances/{id}/mark-paid` | ✅ | `paid?` (boolean, default: true) | - | `MaintenanceDto` |

**Date Format:** `YYYY-MM-DD` (ISO Date)

### Parts Endpoints

| Method | Endpoint | Auth | Request Body | Response |
|--------|----------|------|--------------|----------|
| GET | `/parts` | ✅ | - | `Part[]` |
| GET | `/parts/{id}` | ✅ | - | `Part` |
| POST | `/parts` | ✅ | `Part` | `Part` |
| PUT | `/parts/{id}` | ✅ | `Part` | `Part` |
| DELETE | `/parts/{id}` | ✅ | - | `void` |

**Sorting:** Lists sorted by `id ASC`.

### Offers Endpoints

| Method | Endpoint | Auth | Request Body | Response |
|--------|----------|------|--------------|----------|
| GET | `/offers` | ✅ | - | `OfferDto[]` |
| GET | `/offers/{id}` | ✅ | - | `OfferDto` |
| GET | `/offers/elevator/{elevatorId}` | ✅ | - | `OfferDto[]` |
| POST | `/offers` | ✅ | `OfferDto` | `OfferDto` |
| PUT | `/offers/{id}` | ✅ | `OfferDto` | `OfferDto` |
| DELETE | `/offers/{id}` | ✅ | - | `void` |

**Sorting:** Lists sorted by `id ASC`.

### User Endpoints

| Method | Endpoint | Auth | Role | Request Body | Response |
|--------|----------|------|------|--------------|----------|
| GET | `/users` | ✅ | PATRON | - | `User[]` |
| GET | `/users/{id}` | ✅ | PATRON | - | `User` |
| POST | `/users` | ✅ | PATRON | `UserRequestDto` | `User` |
| PUT | `/users/{id}` | ✅ | PATRON | `UserRequestDto` | `User` |
| DELETE | `/users/{id}` | ✅ | PATRON | - | `User` (soft delete) |

**Important Rules:**
- Password is **required** on CREATE, **optional** on UPDATE
- Last active PATRON cannot be deleted/deactivated
- DELETE is soft delete (sets `active = false`)

**Sorting:** Lists sorted by `id ASC`.

### Fault Endpoints

| Method | Endpoint | Auth | Query Params | Request Body | Response |
|--------|----------|------|--------------|--------------|----------|
| GET | `/faults` | ✅ | `status?` (OPEN/COMPLETED) | - | `FaultDto[]` |
| GET | `/faults/{id}` | ✅ | - | - | `FaultDto` |
| POST | `/faults` | ✅ | - | `FaultDto` | `FaultDto` |
| PUT | `/faults/{id}` | ✅ | - | `FaultDto` | `FaultDto` |
| PUT | `/faults/{id}/status` | ✅ | `status` (query param) | - | `FaultDto` |
| DELETE | `/faults/{id}` | ✅ | - | - | `void` |

### Payment Receipt Endpoints

| Method | Endpoint | Auth | Query Params | Request Body | Response |
|--------|----------|------|--------------|--------------|----------|
| GET | `/payments` | ✅ | `dateFrom?`, `dateTo?` | - | `PaymentReceiptDto[]` |
| GET | `/payments/{id}` | ✅ | - | - | `PaymentReceiptDto` |
| POST | `/payments` | ✅ | - | `PaymentReceiptDto` | `PaymentReceiptDto` |
| PUT | `/payments/{id}` | ✅ | - | `PaymentReceiptDto` | `PaymentReceiptDto` |
| DELETE | `/payments/{id}` | ✅ | - | - | `void` |

### Health Check

| Method | Endpoint | Auth | Response |
|--------|----------|------|----------|
| GET | `/health` | ❌ | `{ status: "OK" }` |

---

## 3. DATA MODELS

### TypeScript Interfaces

```typescript
// ==================== AUTH ====================

interface LoginRequest {
  username: string;
  password: string;
}

interface RefreshTokenRequest {
  refreshToken: string;
}

interface RegisterRequest {
  username: string;
  password: string;
  role: 'PATRON' | 'PERSONEL';
}

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string; // "Bearer"
  userId: number;
  username: string;
  role: 'PATRON' | 'PERSONEL';
}

interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T | null;
  errors: string[] | null;
}

// ==================== DASHBOARD ====================

interface DashboardSummaryDto {
  totalElevators: number;
  totalMaintenances: number;
  totalIncome: number;
  totalDebt: number;
  expiredCount: number;
  warningCount: number;
}

// ==================== ELEVATOR ====================

interface ElevatorDto {
  id?: number;
  identityNumber: string;
  buildingName: string;
  address: string;
  elevatorNumber?: string | null;
  floorCount?: number | null;
  capacity?: number | null;
  speed?: number | null;
  technicalNotes?: string | null;
  driveType?: string | null;
  machineBrand?: string | null;
  doorType?: string | null;
  installationYear?: number | null;
  serialNumber?: string | null;
  controlSystem?: string | null;
  rope?: string | null;
  modernization?: string | null;
  inspectionDate: string; // ISO Date: "YYYY-MM-DD"
  expiryDate?: string; // ISO Date: "YYYY-MM-DD" (auto-calculated)
  blueLabel?: boolean | null;
}

interface ElevatorStatusDto {
  status: 'EXPIRED' | 'WARNING' | 'OK';
  daysLeft: number;
  identityNumber: string;
  buildingName: string;
  expiryDate: string; // ISO Date
}

// ==================== WARNING ====================

interface WarningDto {
  identityNo: string;
  buildingName: string;
  address: string;
  maintenanceEndDate: string; // ISO Date (expiryDate from elevator)
  status: 'EXPIRED' | 'WARNING' | 'OK';
}

interface WarningElevatorDto {
  identityNo: string;
  maintenanceEndDate: string; // ISO Date
  status: 'EXPIRED' | 'WARNING';
}

interface WarningGroupDto {
  buildingName: string;
  address: string;
  status: 'EXPIRED' | 'WARNING';
  elevators: WarningElevatorDto[];
}

// ==================== INSPECTION ====================

interface InspectionDto {
  id?: number;
  elevatorId: number;
  elevatorBuildingName?: string; // Populated from elevator
  elevatorIdentityNumber?: string; // Populated from elevator
  date: string; // ISO Date
  result: 'PASSED' | 'FAILED'; // Uppercase
  description?: string | null;
  createdAt?: string; // ISO DateTime
}

// ==================== MAINTENANCE ====================

interface MaintenanceDto {
  id?: number;
  elevatorId: number;
  elevatorBuildingName?: string; // Populated from elevator
  date: string; // ISO Date
  description?: string | null;
  technicianUserId?: number | null;
  technicianUsername?: string | null; // Populated from user
  amount?: number | null;
  isPaid: boolean;
  paymentDate?: string | null; // ISO Date (auto-set when isPaid=true)
}

interface MaintenanceSummaryDto {
  // Check MaintenanceSummaryDto.java for full structure
  // This might vary - check backend implementation
}

// ==================== PART ====================

interface Part {
  id?: number;
  name: string;
  description?: string | null; // Returns "" if null
  unitPrice: number;
  stock: number; // Can be negative
  createdAt?: string; // ISO DateTime
}

// ==================== OFFER ====================

interface OfferItemDto {
  id?: number;
  partId: number;
  partName?: string; // Populated from part
  quantity: number;
  unitPrice: number;
  lineTotal?: number; // Calculated: quantity * unitPrice
}

interface OfferDto {
  id?: number;
  elevatorId?: number | null; // Optional
  elevatorBuildingName?: string | null;
  elevatorIdentityNumber?: string | null;
  date: string; // ISO Date
  vatRate: number; // Default: 20.0
  discountAmount: number; // Default: 0.0
  subtotal: number; // Calculated from items
  totalAmount: number; // Calculated: (subtotal - discount) * (1 + vatRate/100)
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  items: OfferItemDto[];
  createdAt?: string; // ISO DateTime
}

// ==================== FAULT ====================

interface FaultDto {
  id?: number;
  elevatorId: number;
  elevatorBuildingName?: string;
  elevatorIdentityNumber?: string;
  faultSubject: string;
  contactPerson: string;
  buildingAuthorizedMessage?: string | null;
  description?: string | null;
  status: 'OPEN' | 'COMPLETED';
  createdAt?: string; // ISO DateTime
}

// ==================== PAYMENT RECEIPT ====================

interface PaymentReceiptDto {
  id?: number;
  maintenanceId: number;
  amount: number;
  payerName: string;
  date: string; // ISO Date
  note?: string | null;
  createdAt?: string; // ISO DateTime
}

// ==================== USER ====================

interface User {
  id?: number;
  username: string;
  passwordHash?: string; // Never returned, only accepted on CREATE
  role: 'PATRON' | 'PERSONEL';
  active: boolean;
  createdAt?: string; // ISO DateTime
}

interface UserRequestDto {
  username: string;
  password?: string; // Required on CREATE, optional on UPDATE
  role: 'PATRON' | 'PERSONEL';
  active?: boolean; // Default: true
}

// ==================== FIELD MAPPING NOTES ====================

/*
CRITICAL FIELD MAPPING:

Elevator:
- "Mavi Etiket" → map to `blueLabel` (boolean)
- "İnceleme Tarihi" → `inspectionDate` (ISO Date)
- "Bitiş Tarihi" → `expiryDate` (ISO Date, auto-calculated)
- `expiryDate` is NOT sent in POST/PUT, it's calculated server-side

Warning:
- `maintenanceEndDate` = elevator's `expiryDate`
- `status` = "EXPIRED" | "WARNING" (from calculation)

Date Format:
- All dates: "YYYY-MM-DD" (ISO 8601)
- All date-times: "YYYY-MM-DDTHH:mm:ss" (ISO 8601)

Boolean Handling:
- `blueLabel`, `isPaid`, `active` can be `null` in response
- Map `null` → `false` or show as undefined in UI
*/

---

## 4. APP ARCHITECTURE & FOLDER STRUCTURE

### Recommended Structure

```
sara-asansor-mobile/
├── src/
│   ├── api/
│   │   ├── client.ts              # Axios instance + interceptors
│   │   ├── endpoints.ts           # All API endpoint functions
│   │   ├── auth.ts                # Auth API calls
│   │   ├── elevators.ts           # Elevator API calls
│   │   ├── warnings.ts            # Warning API calls
│   │   ├── inspections.ts         # Inspection API calls
│   │   ├── maintenances.ts        # Maintenance API calls
│   │   ├── parts.ts               # Parts API calls
│   │   ├── offers.ts              # Offers API calls
│   │   ├── users.ts               # Users API calls
│   │   ├── faults.ts              # Faults API calls
│   │   └── payments.ts            # Payment receipts API calls
│   │
│   ├── types/
│   │   ├── api.ts                 # All TypeScript interfaces (from section 3)
│   │   ├── navigation.ts          # Navigation types
│   │   └── storage.ts             # Storage keys
│   │
│   ├── services/
│   │   ├── auth.service.ts        # Auth logic (login, refresh, logout)
│   │   ├── storage.service.ts     # AsyncStorage/SecureStore wrapper
│   │   └── cache.service.ts       # Offline caching logic
│   │
│   ├── context/
│   │   ├── AuthContext.tsx        # Auth state (user, tokens)
│   │   └── AppContext.tsx         # Global app state
│   │
│   ├── navigation/
│   │   ├── AppNavigator.tsx       # Root navigator
│   │   ├── AuthNavigator.tsx      # Login/Register screens
│   │   └── MainNavigator.tsx      # Tab/Drawer navigator
│   │
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── LoginScreen.tsx
│   │   │   └── RegisterScreen.tsx (optional)
│   │   │
│   │   ├── dashboard/
│   │   │   └── DashboardScreen.tsx
│   │   │
│   │   ├── elevators/
│   │   │   ├── ElevatorListScreen.tsx
│   │   │   ├── ElevatorDetailScreen.tsx
│   │   │   ├── ElevatorFormScreen.tsx (create/edit)
│   │   │   └── components/
│   │   │       └── ElevatorCard.tsx
│   │   │
│   │   ├── warnings/
│   │   │   ├── WarningListScreen.tsx
│   │   │   ├── WarningGroupedScreen.tsx
│   │   │   └── components/
│   │   │       ├── WarningCard.tsx
│   │   │       └── WarningGroupCard.tsx
│   │   │
│   │   ├── inspections/
│   │   │   ├── InspectionListScreen.tsx
│   │   │   ├── InspectionFormScreen.tsx
│   │   │   └── InspectionDetailScreen.tsx
│   │   │
│   │   ├── maintenances/
│   │   │   ├── MaintenanceListScreen.tsx
│   │   │   ├── MaintenanceFormScreen.tsx
│   │   │   └── MaintenanceDetailScreen.tsx
│   │   │
│   │   ├── parts/
│   │   │   ├── PartListScreen.tsx
│   │   │   └── PartFormScreen.tsx
│   │   │
│   │   ├── offers/
│   │   │   ├── OfferListScreen.tsx
│   │   │   ├── OfferFormScreen.tsx
│   │   │   └── OfferDetailScreen.tsx
│   │   │
│   │   ├── users/
│   │   │   ├── UserListScreen.tsx
│   │   │   └── UserFormScreen.tsx
│   │   │
│   │   └── settings/
│   │       └── SettingsScreen.tsx
│   │
│   ├── components/
│   │   ├── common/
│   │   │   ├── LoadingSpinner.tsx
│   │   │   ├── ErrorMessage.tsx
│   │   │   ├── EmptyState.tsx
│   │   │   └── DatePicker.tsx
│   │   │
│   │   └── layout/
│   │       ├── Header.tsx
│   │       └── TabBar.tsx
│   │
│   ├── utils/
│   │   ├── date.ts                # Date formatting utilities
│   │   ├── validation.ts          # Form validation
│   │   ├── constants.ts           # App constants
│   │   └── translations.ts        # Turkish text constants
│   │
│   └── hooks/
│       ├── useAuth.ts
│       ├── useApi.ts              # Generic API hook with loading/error
│       └── useRefreshToken.ts     # Token refresh logic
│
├── assets/
│   ├── images/
│   └── fonts/
│
├── app.json                        # Expo config
├── package.json
├── tsconfig.json
└── babel.config.js
```

### Navigation Structure

**Recommended:** Tab Navigation (iOS) + Drawer (Android) or Bottom Tabs (both)

```
MainNavigator (Tab/Drawer)
├── Dashboard Tab
├── Elevators Tab
├── Warnings Tab
├── Inspections Tab
├── Maintenances Tab
├── Parts Tab
├── Offers Tab
├── Users Tab (PATRON only)
└── Settings Tab
```

**Alternative:** Drawer Navigation
- Hamburger menu opens drawer
- All screens accessible from drawer
- Dashboard as default home

---

## 5. IMPLEMENTATION STEPS

### Step 1: Project Setup

```bash
# Create Expo app with TypeScript
npx create-expo-app sara-asansor-mobile --template

# Install dependencies
npm install @react-navigation/native @react-navigation/bottom-tabs @react-navigation/drawer
npm install react-native-screens react-native-safe-area-context
npm install axios
npm install @react-native-async-storage/async-storage expo-secure-store
npm install date-fns
npm install react-native-paper  # or NativeBase

# Dev dependencies
npm install --save-dev @types/react @types/react-native
```

### Step 2: API Client Setup

**Create `src/api/client.ts`:**

```typescript
import axios, { AxiosInstance, AxiosError } from 'axios';
import * as SecureStore from 'expo-secure-store';
import { RefreshTokenRequest, LoginResponse } from '../types/api';

const BASE_URL = __DEV__ 
  ? 'http://localhost:8080/api'
  : 'http://51.21.3.85:8080/api';

const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Add access token
apiClient.interceptors.request.use(
  async (config) => {
    const token = await SecureStore.getItemAsync('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Handle token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as any;

    // If 401 and not already retrying
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = await SecureStore.getItemAsync('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        // Call refresh endpoint
        const response = await axios.post<{ data: LoginResponse }>(
          `${BASE_URL}/auth/refresh`,
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken } = response.data.data!;

        // Store new tokens
        await SecureStore.setItemAsync('accessToken', accessToken);
        await SecureStore.setItemAsync('refreshToken', newRefreshToken);

        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed - logout
        await SecureStore.deleteItemAsync('accessToken');
        await SecureStore.deleteItemAsync('refreshToken');
        // Navigate to login (use navigation ref or event emitter)
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

### Step 3: Auth Service

**Create `src/services/auth.service.ts`:**

```typescript
import apiClient from '../api/client';
import { LoginRequest, LoginResponse, RegisterRequest, ApiResponse } from '../types/api';
import * as SecureStore from 'expo-secure-store';

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<ApiResponse<LoginResponse>>(
      '/auth/login',
      credentials
    );
    
    if (response.data.success && response.data.data) {
      const { accessToken, refreshToken } = response.data.data;
      await SecureStore.setItemAsync('accessToken', accessToken);
      await SecureStore.setItemAsync('refreshToken', refreshToken);
      return response.data.data;
    }
    
    throw new Error(response.data.message || 'Login failed');
  },

  async refreshToken(): Promise<LoginResponse> {
    const refreshToken = await SecureStore.getItemAsync('refreshToken');
    if (!refreshToken) {
      throw new Error('No refresh token');
    }

    const response = await apiClient.post<ApiResponse<LoginResponse>>(
      '/auth/refresh',
      { refreshToken }
    );

    if (response.data.success && response.data.data) {
      const { accessToken, refreshToken: newRefreshToken } = response.data.data;
      await SecureStore.setItemAsync('accessToken', accessToken);
      await SecureStore.setItemAsync('refreshToken', newRefreshToken);
      return response.data.data;
    }

    throw new Error(response.data.message || 'Token refresh failed');
  },

  async logout(): Promise<void> {
    try {
      const refreshToken = await SecureStore.getItemAsync('refreshToken');
      if (refreshToken) {
        await apiClient.post('/auth/logout', { refreshToken });
      }
    } catch (error) {
      // Ignore logout errors
    } finally {
      await SecureStore.deleteItemAsync('accessToken');
      await SecureStore.deleteItemAsync('refreshToken');
    }
  },

  async getStoredTokens(): Promise<{ accessToken: string | null; refreshToken: string | null }> {
    const accessToken = await SecureStore.getItemAsync('accessToken');
    const refreshToken = await SecureStore.getItemAsync('refreshToken');
    return { accessToken, refreshToken };
  },
};
```

### Step 4: API Endpoints

**Create `src/api/elevators.ts`:**

```typescript
import apiClient from './client';
import { ElevatorDto, ElevatorStatusDto, ApiResponse } from '../types/api';

export const elevatorApi = {
  getAll: async (): Promise<ElevatorDto[]> => {
    const response = await apiClient.get<ApiResponse<ElevatorDto[]>>('/elevators');
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch elevators');
  },

  getById: async (id: number): Promise<ElevatorDto> => {
    const response = await apiClient.get<ApiResponse<ElevatorDto>>(`/elevators/${id}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch elevator');
  },

  getStatus: async (id: number): Promise<ElevatorStatusDto> => {
    const response = await apiClient.get<ApiResponse<ElevatorStatusDto>>(`/elevators/${id}/status`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch elevator status');
  },

  create: async (dto: ElevatorDto): Promise<ElevatorDto> => {
    const response = await apiClient.post<ApiResponse<ElevatorDto>>('/elevators', dto);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to create elevator');
  },

  update: async (id: number, dto: ElevatorDto): Promise<ElevatorDto> => {
    const response = await apiClient.put<ApiResponse<ElevatorDto>>(`/elevators/${id}`, dto);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to update elevator');
  },

  delete: async (id: number): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<void>>(`/elevators/${id}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete elevator');
    }
  },
};
```

**Similar pattern for other endpoints** (warnings, inspections, maintenances, parts, offers, users, faults, payments).

### Step 5: Auth Context

**Create `src/context/AuthContext.tsx`:**

```typescript
import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authService } from '../services/auth.service';
import { LoginResponse } from '../types/api';
import * as SecureStore from 'expo-secure-store';

interface AuthContextType {
  user: LoginResponse | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<LoginResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    try {
      const { accessToken } = await authService.getStoredTokens();
      if (accessToken) {
        // Optionally verify token by calling a protected endpoint
        // For now, just set user from stored data
        // You might want to decode JWT to get user info
      }
    } catch (error) {
      // Not authenticated
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (username: string, password: string) => {
    const response = await authService.login({ username, password });
    setUser(response);
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  const refreshUser = async () => {
    try {
      const response = await authService.refreshToken();
      setUser(response);
    } catch (error) {
      await logout();
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user,
        login,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

### Step 6: Login Screen

**Create `src/screens/auth/LoginScreen.tsx`:**

```typescript
import React, { useState } from 'react';
import { View, StyleSheet, Alert, KeyboardAvoidingView, Platform } from 'react-native';
import { TextInput, Button, Text, Surface } from 'react-native-paper';
import { useAuth } from '../../context/AuthContext';

export const LoginScreen: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();

  const handleLogin = async () => {
    if (!username || !password) {
      Alert.alert('Hata', 'Kullanıcı adı ve şifre gereklidir');
      return;
    }

    setLoading(true);
    try {
      await login(username, password);
      // Navigation handled by AuthContext or navigation logic
    } catch (error: any) {
      Alert.alert('Giriş Hatası', error.message || 'Giriş yapılamadı');
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
        <TextInput
          label="Kullanıcı Adı"
          value={username}
          onChangeText={setUsername}
          mode="outlined"
          style={styles.input}
          autoCapitalize="none"
        />
        <TextInput
          label="Şifre"
          value={password}
          onChangeText={setPassword}
          mode="outlined"
          secureTextEntry
          style={styles.input}
        />
        <Button
          mode="contained"
          onPress={handleLogin}
          loading={loading}
          disabled={loading}
          style={styles.button}
        >
          Giriş Yap
        </Button>
      </Surface>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 20,
  },
  surface: {
    padding: 20,
    borderRadius: 8,
  },
  title: {
    textAlign: 'center',
    marginBottom: 30,
  },
  input: {
    marginBottom: 15,
  },
  button: {
    marginTop: 10,
  },
});
```

### Step 7: Dashboard Screen

**Create `src/screens/dashboard/DashboardScreen.tsx`:**

```typescript
import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import { Card, Title, Text, Chip } from 'react-native-paper';
import { dashboardApi } from '../../api/dashboard';
import { DashboardSummaryDto } from '../../types/api';

export const DashboardScreen: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummaryDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const loadSummary = async () => {
    try {
      const data = await dashboardApi.getSummary();
      setSummary(data);
    } catch (error: any) {
      console.error('Failed to load dashboard:', error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadSummary();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    loadSummary();
  };

  if (loading) {
    return <Text>Yükleniyor...</Text>;
  }

  if (!summary) {
    return <Text>Veri yüklenemedi</Text>;
  }

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      <Card style={styles.card}>
        <Card.Content>
          <Title>Toplam Asansör</Title>
          <Text variant="headlineMedium">{summary.totalElevators}</Text>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <Title>Toplam Bakım</Title>
          <Text variant="headlineMedium">{summary.totalMaintenances}</Text>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <Title>Toplam Gelir</Title>
          <Text variant="headlineMedium">{summary.totalIncome?.toFixed(2)} ₺</Text>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <Title>Toplam Borç</Title>
          <Text variant="headlineMedium">{summary.totalDebt?.toFixed(2)} ₺</Text>
        </Card.Content>
      </Card>

      <Card style={[styles.card, styles.warningCard]}>
        <Card.Content>
          <Title>Süresi Dolmuş</Title>
          <Chip icon="alert-circle" style={styles.expiredChip}>
            {summary.expiredCount}
          </Chip>
        </Card.Content>
      </Card>

      <Card style={[styles.card, styles.warningCard]}>
        <Card.Content>
          <Title>Yakında Dolacak</Title>
          <Chip icon="alert" style={styles.warningChip}>
            {summary.warningCount}
          </Chip>
        </Card.Content>
      </Card>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  card: {
    marginBottom: 16,
  },
  warningCard: {
    borderLeftWidth: 4,
    borderLeftColor: '#ff9800',
  },
  expiredChip: {
    backgroundColor: '#f44336',
  },
  warningChip: {
    backgroundColor: '#ff9800',
  },
});
```

### Step 8: Warnings Screen (Grouped)

**Create `src/screens/warnings/WarningGroupedScreen.tsx`:**

```typescript
import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import { Card, Title, Text, Chip, Divider, Button } from 'react-native-paper';
import { warningApi } from '../../api/warnings';
import { WarningGroupDto } from '../../types/api';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';

export const WarningGroupedScreen: React.FC = () => {
  const [groups, setGroups] = useState<WarningGroupDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set());

  const loadWarnings = async () => {
    try {
      const data = await warningApi.getGrouped();
      setGroups(data);
    } catch (error: any) {
      console.error('Failed to load warnings:', error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadWarnings();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    loadWarnings();
  };

  const toggleGroup = (key: string) => {
    const newExpanded = new Set(expandedGroups);
    if (newExpanded.has(key)) {
      newExpanded.delete(key);
    } else {
      newExpanded.add(key);
    }
    setExpandedGroups(newExpanded);
  };

  if (loading) {
    return <Text>Yükleniyor...</Text>;
  }

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      {groups.map((group, index) => {
        const groupKey = `${group.buildingName}-${group.address}`;
        const isExpanded = expandedGroups.has(groupKey);
        const statusColor = group.status === 'EXPIRED' ? '#f44336' : '#ff9800';

        return (
          <Card key={index} style={styles.groupCard}>
            <Card.Content>
              <View style={styles.groupHeader}>
                <View style={styles.groupInfo}>
                  <Title>{group.buildingName}</Title>
                  <Text variant="bodyMedium" style={styles.address}>
                    {group.address}
                  </Text>
                  <Chip
                    icon={group.status === 'EXPIRED' ? 'alert-circle' : 'alert'}
                    style={[styles.statusChip, { backgroundColor: statusColor }]}
                  >
                    {group.status === 'EXPIRED' ? 'Süresi Dolmuş' : 'Uyarı'}
                  </Chip>
                </View>
                <Button
                  icon={isExpanded ? 'chevron-up' : 'chevron-down'}
                  onPress={() => toggleGroup(groupKey)}
                >
                  {group.elevators.length} Asansör
                </Button>
              </View>

              {isExpanded && (
                <>
                  <Divider style={styles.divider} />
                  {group.elevators.map((elevator, idx) => (
                    <View key={idx} style={styles.elevatorItem}>
                      <Text variant="bodyLarge">{elevator.identityNo}</Text>
                      <Text variant="bodySmall">
                        Bitiş: {format(new Date(elevator.maintenanceEndDate), 'dd MMM yyyy', { locale: tr })}
                      </Text>
                    </View>
                  ))}
                </>
              )}
            </Card.Content>
          </Card>
        );
      })}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  groupCard: {
    marginBottom: 16,
  },
  groupHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  groupInfo: {
    flex: 1,
  },
  address: {
    color: '#666',
    marginTop: 4,
  },
  statusChip: {
    marginTop: 8,
    alignSelf: 'flex-start',
  },
  divider: {
    marginVertical: 12,
  },
  elevatorItem: {
    paddingVertical: 8,
    borderLeftWidth: 3,
    borderLeftColor: '#2196F3',
    paddingLeft: 12,
    marginBottom: 8,
  },
});
```

### Step 9: Elevator Form Screen

**Create `src/screens/elevators/ElevatorFormScreen.tsx`:**

```typescript
import React, { useState, useEffect } from 'react';
import { View, StyleSheet, ScrollView, Alert } from 'react-native';
import { TextInput, Button, Text } from 'react-native-paper';
import { elevatorApi } from '../../api/elevators';
import { ElevatorDto } from '../../types/api';
import { format } from 'date-fns';

interface Props {
  route: any;
  navigation: any;
}

export const ElevatorFormScreen: React.FC<Props> = ({ route, navigation }) => {
  const elevatorId = route.params?.id;
  const isEdit = !!elevatorId;

  const [formData, setFormData] = useState<Partial<ElevatorDto>>({
    identityNumber: '',
    buildingName: '',
    address: '',
    inspectionDate: format(new Date(), 'yyyy-MM-dd'),
    blueLabel: false,
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEdit) {
      loadElevator();
    }
  }, [elevatorId]);

  const loadElevator = async () => {
    try {
      const data = await elevatorApi.getById(elevatorId!);
      setFormData(data);
    } catch (error: any) {
      Alert.alert('Hata', error.message);
    }
  };

  const handleSubmit = async () => {
    // Validation
    if (!formData.identityNumber || !formData.buildingName || !formData.address) {
      Alert.alert('Hata', 'Kimlik numarası, bina adı ve adres gereklidir');
      return;
    }

    setLoading(true);
    try {
      if (isEdit) {
        await elevatorApi.update(elevatorId!, formData as ElevatorDto);
        Alert.alert('Başarılı', 'Asansör güncellendi', [
          { text: 'Tamam', onPress: () => navigation.goBack() },
        ]);
      } else {
        await elevatorApi.create(formData as ElevatorDto);
        Alert.alert('Başarılı', 'Asansör eklendi', [
          { text: 'Tamam', onPress: () => navigation.goBack() },
        ]);
      }
    } catch (error: any) {
      Alert.alert('Hata', error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={styles.container}>
      <TextInput
        label="Kimlik Numarası *"
        value={formData.identityNumber}
        onChangeText={(text) => setFormData({ ...formData, identityNumber: text })}
        mode="outlined"
        style={styles.input}
      />

      <TextInput
        label="Bina Adı *"
        value={formData.buildingName}
        onChangeText={(text) => setFormData({ ...formData, buildingName: text })}
        mode="outlined"
        style={styles.input}
      />

      <TextInput
        label="Adres *"
        value={formData.address}
        onChangeText={(text) => setFormData({ ...formData, address: text })}
        mode="outlined"
        multiline
        numberOfLines={2}
        style={styles.input}
      />

      <TextInput
        label="Asansör Numarası"
        value={formData.elevatorNumber || ''}
        onChangeText={(text) => setFormData({ ...formData, elevatorNumber: text })}
        mode="outlined"
        style={styles.input}
      />

      <TextInput
        label="İnceleme Tarihi *"
        value={formData.inspectionDate || ''}
        onChangeText={(text) => setFormData({ ...formData, inspectionDate: text })}
        mode="outlined"
        placeholder="YYYY-MM-DD"
        style={styles.input}
      />

      <View style={styles.row}>
        <TextInput
          label="Kat Sayısı"
          value={formData.floorCount?.toString() || ''}
          onChangeText={(text) =>
            setFormData({ ...formData, floorCount: text ? parseInt(text) : null })
          }
          mode="outlined"
          keyboardType="numeric"
          style={[styles.input, styles.half]}
        />

        <TextInput
          label="Kapasite (kg)"
          value={formData.capacity?.toString() || ''}
          onChangeText={(text) =>
            setFormData({ ...formData, capacity: text ? parseInt(text) : null })
          }
          mode="outlined"
          keyboardType="numeric"
          style={[styles.input, styles.half]}
        />
      </View>

      <TextInput
        label="Hız (m/s)"
        value={formData.speed?.toString() || ''}
        onChangeText={(text) =>
          setFormData({ ...formData, speed: text ? parseFloat(text) : null })
        }
        mode="outlined"
        keyboardType="decimal-pad"
        style={styles.input}
      />

      {/* Add other fields: technicalNotes, driveType, machineBrand, etc. */}

      <Button
        mode="contained"
        onPress={handleSubmit}
        loading={loading}
        disabled={loading}
        style={styles.button}
      >
        {isEdit ? 'Güncelle' : 'Kaydet'}
      </Button>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  input: {
    marginBottom: 16,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  half: {
    width: '48%',
  },
  button: {
    marginTop: 16,
    marginBottom: 32,
  },
});
```

---

## 6. CODE BLOCKS

### API Client with Refresh Token

See Step 2 above for full implementation.

### Offline Caching Service

**Create `src/services/cache.service.ts`:**

```typescript
import AsyncStorage from '@react-native-async-storage/async-storage';

const CACHE_KEYS = {
  ELEVATORS: '@cache:elevators',
  PARTS: '@cache:parts',
  MAINTENANCES: '@cache:maintenances',
  // ... other cache keys
};

export const cacheService = {
  async set<T>(key: string, data: T): Promise<void> {
    try {
      await AsyncStorage.setItem(key, JSON.stringify(data));
    } catch (error) {
      console.error('Cache set error:', error);
    }
  },

  async get<T>(key: string): Promise<T | null> {
    try {
      const data = await AsyncStorage.getItem(key);
      return data ? JSON.parse(data) : null;
    } catch (error) {
      console.error('Cache get error:', error);
      return null;
    }
  },

  async clear(key: string): Promise<void> {
    try {
      await AsyncStorage.removeItem(key);
    } catch (error) {
      console.error('Cache clear error:', error);
    }
  },

  async clearAll(): Promise<void> {
    try {
      await AsyncStorage.multiRemove(Object.values(CACHE_KEYS));
    } catch (error) {
      console.error('Cache clear all error:', error);
    }
  },
};
```

### Custom Hook for API Calls

**Create `src/hooks/useApi.ts`:**

```typescript
import { useState, useEffect } from 'react';

interface UseApiResult<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useApi<T>(
  apiCall: () => Promise<T>,
  dependencies: any[] = []
): UseApiResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiCall();
      setData(result);
    } catch (err: any) {
      setError(err.message || 'Bir hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, dependencies);

  return { data, loading, error, refetch: fetchData };
}
```

### Date Utility

**Create `src/utils/date.ts`:**

```typescript
import { format, parseISO } from 'date-fns';
import { tr } from 'date-fns/locale';

export const dateUtils = {
  formatDate: (dateString: string | null | undefined): string => {
    if (!dateString) return '';
    try {
      return format(parseISO(dateString), 'dd MMM yyyy', { locale: tr });
    } catch {
      return dateString;
    }
  },

  formatDateTime: (dateString: string | null | undefined): string => {
    if (!dateString) return '';
    try {
      return format(parseISO(dateString), 'dd MMM yyyy HH:mm', { locale: tr });
    } catch {
      return dateString;
    }
  },

  formatInputDate: (date: Date): string => {
    return format(date, 'yyyy-MM-dd');
  },

  parseInputDate: (dateString: string): Date => {
    return parseISO(dateString);
  },
};
```

### Turkish Error Messages

**Create `src/utils/translations.ts`:**

```typescript
export const errorMessages: Record<string, string> = {
  'Login failed': 'Giriş başarısız. Kullanıcı adı veya şifre hatalı.',
  'Token refresh failed': 'Oturum yenileme başarısız. Lütfen tekrar giriş yapın.',
  'User not found': 'Kullanıcı bulunamadı.',
  'Elevator not found': 'Asansör bulunamadı.',
  'En az bir aktif PATRON bulunmalıdır.': 'En az bir aktif PATRON kullanıcısı olmalıdır.',
  'Username already exists': 'Bu kullanıcı adı zaten kullanılıyor.',
  'Password is required when creating a new user': 'Yeni kullanıcı oluştururken şifre gereklidir.',
  // Add more mappings as needed
};

export const getErrorMessage = (error: string): string => {
  return errorMessages[error] || error || 'Bir hata oluştu';
};
```

---

## 7. QA CHECKLIST

### Authentication
- [ ] Login with valid credentials works
- [ ] Login with invalid credentials shows error
- [ ] Access token stored securely
- [ ] Refresh token stored securely
- [ ] Auto-refresh on 401 works
- [ ] Logout clears tokens
- [ ] App redirects to login when not authenticated

### Dashboard
- [ ] All summary statistics load correctly
- [ ] Pull-to-refresh works
- [ ] Numbers match backend data
- [ ] Expired/Warning counts are correct

### Elevators
- [ ] List loads and displays all elevators
- [ ] Search functionality works (if implemented)
- [ ] Create elevator with all required fields
- [ ] Update elevator preserves all fields
- [ ] `blueLabel` field maps correctly (not "Mavi Etiket")
- [ ] `inspectionDate` updates correctly
- [ ] `expiryDate` is calculated server-side (not sent in POST/PUT)
- [ ] List order remains stable after update (ID ASC)
- [ ] Detail screen shows all fields

### Warnings
- [ ] Flat list shows all warnings
- [ ] Grouped view groups by building correctly
- [ ] EXPIRED appears before WARNING
- [ ] Expand/collapse works
- [ ] Tap elevator navigates to detail
- [ ] Status colors are correct (RED for EXPIRED, ORANGE for WARNING)

### Inspections
- [ ] List loads correctly
- [ ] Create inspection with PASSED/FAILED result
- [ ] Result field accepts uppercase strings
- [ ] Elevator dropdown works
- [ ] Date picker works correctly

### Maintenances
- [ ] List loads with filters (paid/unpaid, date range)
- [ ] Create maintenance works
- [ ] Mark as paid updates `paymentDate`
- [ ] Unpaid maintenances show correct status

### Parts
- [ ] List loads and shows description field
- [ ] Description shows "" instead of null
- [ ] Create/update parts works
- [ ] Stock can be negative

### Offers
- [ ] List loads with items
- [ ] Create offer with items calculates totals
- [ ] VAT rate defaults to 20%
- [ ] Status can be PENDING/ACCEPTED/REJECTED

### Users (PATRON only)
- [ ] List shows only for PATRON role
- [ ] Create user requires password
- [ ] Update user: password is optional
- [ ] Delete is soft delete (active = false)
- [ ] Last PATRON cannot be deleted/deactivated
- [ ] Error message shows when trying to delete last PATRON

### Settings
- [ ] API base URL can be changed
- [ ] Dev/Prod toggle works
- [ ] Logout button works
- [ ] Settings persist across app restarts

### Offline
- [ ] Last successful list data cached
- [ ] Cached data shows when offline (with indicator)
- [ ] Refresh updates cache

### General
- [ ] All dates formatted correctly (Turkish locale)
- [ ] All error messages in Turkish
- [ ] Loading states shown during API calls
- [ ] Empty states shown when no data
- [ ] Navigation works correctly
- [ ] Back button behavior is correct
- [ ] Keyboard dismisses on scroll
- [ ] Forms validate required fields

---

## 8. BACKEND_GAPS

### Missing Features / Clarifications Needed

1. **Low Stock Indicator**
   - Dashboard shows "low stock" but backend doesn't define what "low" means
   - **Action:** Add threshold configuration or use client-side logic (e.g., stock < 5)

2. **Unpaid Maintenances Count**
   - Dashboard summary doesn't explicitly include unpaid count
   - **Current:** `totalDebt` exists but not count
   - **Action:** Either add `unpaidCount` to DashboardSummaryDto or calculate client-side from list

3. **Search/Filter on Elevators**
   - Backend doesn't have search endpoint (by building name, identity number, etc.)
   - **Action:** Implement client-side search or add `/elevators/search?q=` endpoint

4. **User Profile/Details**
   - No endpoint to get current logged-in user details
   - **Current:** User info comes from JWT token (must decode)
   - **Action:** Add `GET /users/me` endpoint or decode JWT client-side

5. **Image/File Upload**
   - `file_attachments` table exists but no endpoints visible
   - **Action:** Check if file upload endpoints exist or mark as future feature

6. **Date Range Validation**
   - No explicit validation for date ranges in requests
   - **Action:** Handle invalid date formats gracefully on client

7. **Pagination**
   - All list endpoints return all records (no pagination)
   - **Action:** For large datasets, implement client-side pagination or add backend pagination

8. **Elevator Status Calculation**
   - Status (EXPIRED/WARNING/OK) calculation logic not documented
   - **Known:** EXPIRED = expiryDate < today, WARNING = expiryDate within 30 days
   - **Action:** Document clearly or use `/elevators/{id}/status` endpoint

9. **MaintenanceSummaryDto Structure**
   - `MaintenanceSummaryDto` fields not fully visible in codebase
   - **Action:** Verify exact structure before implementing monthly summary

10. **Blue Label Field Usage**
    - `blueLabel` field exists but usage/business rule not clear
    - **Action:** Confirm if this is just a flag or if it triggers any logic

### Known Workarounds

- **Token Refresh Navigation:** Use React Navigation's `navigationRef` to navigate from interceptor
- **Date Format:** Always use ISO format (`YYYY-MM-DD`) for dates
- **Boolean Null Handling:** Map `null` to `false` or `undefined` in UI

---

## 9. BUILD & DEPLOY GUIDE

### Development Setup

```bash
# Install dependencies
npm install

# Start Expo dev server
npx expo start

# Run on iOS simulator
npx expo start --ios

# Run on Android emulator
npx expo start --android
```

### Environment Configuration

**Create `app.config.js`:**

```javascript
export default {
  expo: {
    name: 'SaraAsansor Mobile',
    slug: 'sara-asansor-mobile',
    version: '1.0.0',
    orientation: 'portrait',
    icon: './assets/icon.png',
    userInterfaceStyle: 'light',
    splash: {
      image: './assets/splash.png',
      resizeMode: 'contain',
      backgroundColor: '#ffffff',
    },
    assetBundlePatterns: ['**/*'],
    ios: {
      supportsTablet: true,
      bundleIdentifier: 'com.saraasansor.mobile',
    },
    android: {
      adaptiveIcon: {
        foregroundImage: './assets/adaptive-icon.png',
        backgroundColor: '#ffffff',
      },
      package: 'com.saraasansor.mobile',
    },
    web: {
      favicon: './assets/favicon.png',
    },
  },
};
```

### Production Build

```bash
# Build for iOS (requires Apple Developer account)
eas build --platform ios

# Build for Android
eas build --platform android

# Build for both
eas build --platform all
```

### Testing Checklist Before Release

- [ ] Test on iOS device (physical)
- [ ] Test on Android device (physical)
- [ ] Test token refresh flow
- [ ] Test offline caching
- [ ] Test all CRUD operations
- [ ] Test navigation flows
- [ ] Test error handling
- [ ] Test with slow network (throttle)
- [ ] Test with no network (offline mode)
- [ ] Verify all Turkish translations
- [ ] Verify date formats (Turkish locale)

### Deployment

**iOS:**
1. Build with EAS or `expo build:ios`
2. Submit to App Store Connect
3. TestFlight for beta testing
4. App Store release

**Android:**
1. Build with EAS or `expo build:android`
2. Generate signed APK/AAB
3. Upload to Google Play Console
4. Internal testing → Closed testing → Production

### Configuration for Production

**Update API base URL in production:**

```typescript
// In src/api/client.ts or config file
const BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://51.21.3.85:8080/api';
```

**Set environment variable in `app.config.js` or `.env`:**

```javascript
extra: {
  apiUrl: process.env.EXPO_PUBLIC_API_URL || 'http://51.21.3.85:8080/api',
}
```

---

## APPENDIX: CRITICAL FIELD MAPPINGS

### Elevator Fields

| Frontend Label | Backend Field | Type | Notes |
|----------------|---------------|------|-------|
| Kimlik No | `identityNumber` | string | Required, unique |
| Bina Adı | `buildingName` | string | Required |
| Adres | `address` | string | Required |
| Asansör No | `elevatorNumber` | string? | Optional |
| Kat Sayısı | `floorCount` | number? | Optional |
| Kapasite | `capacity` | number? | Optional (kg) |
| Hız | `speed` | number? | Optional (m/s) |
| Mavi Etiket | `blueLabel` | boolean? | Map to boolean, not string |
| İnceleme Tarihi | `inspectionDate` | string | ISO Date, required |
| Bitiş Tarihi | `expiryDate` | string | ISO Date, auto-calculated (don't send in POST/PUT) |

### Warning Fields

| Frontend Label | Backend Field | Source |
|----------------|---------------|--------|
| Kimlik No | `identityNo` | Elevator.identityNumber |
| Bina Adı | `buildingName` | Elevator.buildingName |
| Adres | `address` | Elevator.address |
| Kontrol Bitiş | `maintenanceEndDate` | Elevator.expiryDate |
| Durum | `status` | Calculated (EXPIRED/WARNING) |

### Maintenance Fields

| Frontend Label | Backend Field | Type | Notes |
|----------------|---------------|------|-------|
| Ödendi | `isPaid` | boolean | Default: false |
| Ödeme Tarihi | `paymentDate` | string? | Auto-set when isPaid=true |
| Tutar | `amount` | number? | Optional |

### User Fields

| Frontend Label | Backend Field | Type | Notes |
|----------------|---------------|------|-------|
| Şifre | `password` | string? | Required on CREATE, optional on UPDATE |
| Aktif | `active` | boolean | Default: true |
| Rol | `role` | 'PATRON' \| 'PERSONEL' | Required |

---

**END OF DOCUMENT**

*This document serves as the complete implementation guide for SaraAsansor Mobile App. All endpoints and data models are based on the existing Spring Boot backend. Use this as the single source of truth for mobile app development.*
