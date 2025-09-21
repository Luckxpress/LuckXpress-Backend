import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { AdminUser, DashboardMetrics } from '../types';

interface AuthState {
  user: AdminUser | null;
  isAuthenticated: boolean;
  login: (user: AdminUser) => void;
  logout: () => void;
  updateUser: (user: Partial<AdminUser>) => void;
}

interface DashboardState {
  metrics: DashboardMetrics | null;
  isLoading: boolean;
  lastUpdated: Date | null;
  setMetrics: (metrics: DashboardMetrics) => void;
  setLoading: (loading: boolean) => void;
}

interface UIState {
  sidebarOpen: boolean;
  theme: 'light' | 'dark';
  notifications: Notification[];
  toggleSidebar: () => void;
  setTheme: (theme: 'light' | 'dark') => void;
  addNotification: (notification: Omit<Notification, 'id'>) => void;
  removeNotification: (id: string) => void;
}

interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  timestamp: Date;
}

export const useAuthStore = create<AuthState>()(
  devtools(
    persist(
      (set, get) => ({
        user: null,
        isAuthenticated: false,
        login: (user) => set({ user, isAuthenticated: true }),
        logout: () => set({ user: null, isAuthenticated: false }),
        updateUser: (userData) => {
          const currentUser = get().user;
          if (currentUser) {
            set({ user: { ...currentUser, ...userData } });
          }
        },
      }),
      {
        name: 'auth-store',
        partialize: (state) => ({
          user: state.user,
          isAuthenticated: state.isAuthenticated,
        }),
      }
    ),
    { name: 'auth-store' }
  )
);

export const useDashboardStore = create<DashboardState>()(
  devtools(
    (set) => ({
      metrics: null,
      isLoading: false,
      lastUpdated: null,
      setMetrics: (metrics) => set({ metrics, lastUpdated: new Date() }),
      setLoading: (isLoading) => set({ isLoading }),
    }),
    { name: 'dashboard-store' }
  )
);

export const useUIStore = create<UIState>()(
  devtools(
    persist(
      (set, get) => ({
        sidebarOpen: true,
        theme: 'light',
        notifications: [],
        toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
        setTheme: (theme) => set({ theme }),
        addNotification: (notification) => {
          const id = Math.random().toString(36).substr(2, 9);
          const newNotification = { ...notification, id, timestamp: new Date() };
          set((state) => ({
            notifications: [...state.notifications, newNotification],
          }));
          // Auto-remove after 5 seconds
          setTimeout(() => {
            get().removeNotification(id);
          }, 5000);
        },
        removeNotification: (id) =>
          set((state) => ({
            notifications: state.notifications.filter((n) => n.id !== id),
          })),
      }),
      {
        name: 'ui-store',
        partialize: (state) => ({
          sidebarOpen: state.sidebarOpen,
          theme: state.theme,
        }),
      }
    ),
    { name: 'ui-store' }
  )
);
