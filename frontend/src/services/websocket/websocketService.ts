import { io, Socket } from 'socket.io-client';
import { WebSocketMessage, RealTimeUpdate } from '../../types';
import { authService } from '../auth/authService';

type EventHandler = (data: any) => void;

class WebSocketService {
  private socket: Socket | null = null;
  private eventHandlers: Map<string, EventHandler[]> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private isConnecting = false;

  connect(): void {
    if (this.socket?.connected || this.isConnecting) {
      return;
    }

    const token = authService.getToken();
    if (!token) {
      console.warn('No auth token found, skipping WebSocket connection');
      return;
    }

    this.isConnecting = true;

    const wsUrl = process.env.REACT_APP_WS_URL || 'ws://localhost:8080';
    
    this.socket = io(wsUrl, {
      auth: {
        token: token
      },
      transports: ['websocket', 'polling'],
      timeout: 10000,
      reconnection: true,
      reconnectionAttempts: this.maxReconnectAttempts,
      reconnectionDelay: 1000,
    });

    this.setupEventListeners();
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
      this.isConnecting = false;
      this.reconnectAttempts = 0;
    }
  }

  on(event: string, handler: EventHandler): void {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, []);
    }
    this.eventHandlers.get(event)!.push(handler);

    if (this.socket) {
      this.socket.on(event, handler);
    }
  }

  off(event: string, handler?: EventHandler): void {
    if (handler) {
      const handlers = this.eventHandlers.get(event) || [];
      const index = handlers.indexOf(handler);
      if (index > -1) {
        handlers.splice(index, 1);
      }
      if (this.socket) {
        this.socket.off(event, handler);
      }
    } else {
      this.eventHandlers.delete(event);
      if (this.socket) {
        this.socket.removeAllListeners(event);
      }
    }
  }

  emit(event: string, data?: any): void {
    if (this.socket?.connected) {
      this.socket.emit(event, data);
    } else {
      console.warn('WebSocket not connected, cannot emit event:', event);
    }
  }

  // Specific event handlers for admin dashboard
  onUserActivity(handler: (update: RealTimeUpdate) => void): void {
    this.on('user_activity', handler);
  }

  onTransactionUpdate(handler: (update: RealTimeUpdate) => void): void {
    this.on('transaction_update', handler);
  }

  onComplianceAlert(handler: (update: RealTimeUpdate) => void): void {
    this.on('compliance_alert', handler);
  }

  onSystemAlert(handler: (update: RealTimeUpdate) => void): void {
    this.on('system_alert', handler);
  }

  onKycUpdate(handler: (update: RealTimeUpdate) => void): void {
    this.on('kyc_update', handler);
  }

  // Join/leave specific rooms
  joinDashboard(): void {
    this.emit('join_dashboard');
  }

  leaveDashboard(): void {
    this.emit('leave_dashboard');
  }

  joinUserMonitoring(): void {
    this.emit('join_user_monitoring');
  }

  leaveUserMonitoring(): void {
    this.emit('leave_user_monitoring');
  }

  joinTransactionMonitoring(): void {
    this.emit('join_transaction_monitoring');
  }

  leaveTransactionMonitoring(): void {
    this.emit('leave_transaction_monitoring');
  }

  private setupEventListeners(): void {
    if (!this.socket) return;

    this.socket.on('connect', () => {
      console.log('WebSocket connected');
      this.isConnecting = false;
      this.reconnectAttempts = 0;
      
      // Re-register all event handlers
      this.eventHandlers.forEach((handlers, event) => {
        handlers.forEach(handler => {
          this.socket!.on(event, handler);
        });
      });
    });

    this.socket.on('disconnect', (reason) => {
      console.log('WebSocket disconnected:', reason);
      this.isConnecting = false;
    });

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket connection error:', error);
      this.isConnecting = false;
      this.reconnectAttempts++;
      
      if (this.reconnectAttempts >= this.maxReconnectAttempts) {
        console.error('Max reconnection attempts reached');
      }
    });

    this.socket.on('auth_error', (error) => {
      console.error('WebSocket auth error:', error);
      this.disconnect();
      // Redirect to login if auth fails
      authService.logout();
    });

    // Handle real-time updates
    this.socket.on('real_time_update', (update: RealTimeUpdate) => {
      console.log('Real-time update received:', update);
      // Emit specific events based on update type
      this.socket!.emit(update.type, update);
    });
  }

  isConnected(): boolean {
    return this.socket?.connected || false;
  }

  getConnectionState(): string {
    if (!this.socket) return 'disconnected';
    return this.socket.connected ? 'connected' : 'disconnected';
  }
}

export const websocketService = new WebSocketService();
export default websocketService;
