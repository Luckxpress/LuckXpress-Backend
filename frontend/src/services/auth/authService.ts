import { apiService } from '../api';
import { AdminUser } from '../../types';

export interface LoginCredentials {
  email: string;
  password: string;
  twoFactorCode?: string;
}

export interface LoginResponse {
  token: string;
  user: AdminUser;
  expiresIn: number;
}

export interface RefreshTokenResponse {
  token: string;
  expiresIn: number;
}

class AuthService {
  private tokenKey = 'admin_token';
  private userKey = 'admin_user';

  async login(credentials: LoginCredentials): Promise<LoginResponse> {
    const response = await apiService.post<LoginResponse>('/auth/login', credentials);
    
    if (response.success && response.data) {
      this.setToken(response.data.token);
      this.setUser(response.data.user);
      return response.data;
    }
    
    throw new Error(response.error || 'Login failed');
  }

  async logout(): Promise<void> {
    try {
      await apiService.post('/auth/logout');
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      this.clearAuth();
    }
  }

  async refreshToken(): Promise<string> {
    const response = await apiService.post<RefreshTokenResponse>('/auth/refresh');
    
    if (response.success && response.data) {
      this.setToken(response.data.token);
      return response.data.token;
    }
    
    throw new Error('Token refresh failed');
  }

  async getCurrentUser(): Promise<AdminUser> {
    const response = await apiService.get<AdminUser>('/auth/me');
    
    if (response.success && response.data) {
      this.setUser(response.data);
      return response.data;
    }
    
    throw new Error('Failed to get current user');
  }

  async changePassword(oldPassword: string, newPassword: string): Promise<void> {
    const response = await apiService.post('/auth/change-password', {
      oldPassword,
      newPassword,
    });
    
    if (!response.success) {
      throw new Error(response.error || 'Password change failed');
    }
  }

  async enableTwoFactor(): Promise<{ qrCode: string; backupCodes: string[] }> {
    const response = await apiService.post<{ qrCode: string; backupCodes: string[] }>('/auth/2fa/enable');
    
    if (response.success && response.data) {
      return response.data;
    }
    
    throw new Error('Failed to enable 2FA');
  }

  async verifyTwoFactor(code: string): Promise<void> {
    const response = await apiService.post('/auth/2fa/verify', { code });
    
    if (!response.success) {
      throw new Error(response.error || '2FA verification failed');
    }
  }

  async disableTwoFactor(password: string): Promise<void> {
    const response = await apiService.post('/auth/2fa/disable', { password });
    
    if (!response.success) {
      throw new Error(response.error || 'Failed to disable 2FA');
    }
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getUser(): AdminUser | null {
    const userStr = localStorage.getItem(this.userKey);
    if (userStr) {
      try {
        return JSON.parse(userStr);
      } catch (error) {
        console.error('Error parsing user data:', error);
        this.clearAuth();
      }
    }
    return null;
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    const user = this.getUser();
    return !!(token && user);
  }

  hasPermission(resource: string, action: string): boolean {
    const user = this.getUser();
    if (!user) return false;

    if (user.role === 'super_admin') return true;

    return user.permissions.some(
      permission => 
        permission.resource === resource && 
        permission.actions.includes(action as any)
    );
  }

  private setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  private setUser(user: AdminUser): void {
    localStorage.setItem(this.userKey, JSON.stringify(user));
  }

  private clearAuth(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  }
}

export const authService = new AuthService();
export default authService;
