import SockJS from 'sockjs-client';
import { Client, StompSubscription } from '@stomp/stompjs';

const WEBSOCKET_URL = 'http://localhost:8080/ws'; // Adjust to your backend URL

export interface ParkingEvent {
  event: 'ENTRY_DETECTED' | 'EXIT_DETECTED';
  reservationId: number;
  driverId: string;
  spotNumber: string;
  startTime?: string;
  endTime?: string;
  totalCost?: number;
  status: 'ACTIVE' | 'COMPLETED';
}

export class WebSocketService {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private isConnected = false;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;

  /**
   * 🔌 Connect to WebSocket server
   */
  connect(driverId: string, onMessage: (event: ParkingEvent) => void): Promise<void> {
    return new Promise((resolve, reject) => {
      console.log('🔌 Connecting to WebSocket...', WEBSOCKET_URL);

      this.client = new Client({
        webSocketFactory: () => new SockJS(WEBSOCKET_URL) as any,

        debug: (str) => {
          console.log('🔍 WebSocket Debug:', str);
        },

        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,

        onConnect: () => {
          console.log('✅ WebSocket connected!');
          this.isConnected = true;
          this.reconnectAttempts = 0;

          // Subscribe to driver-specific channel
          const topic = `/topic/driver/${driverId}`;
          console.log('📡 Subscribing to:', topic);

          this.subscription = this.client!.subscribe(topic, (message) => {
            console.log('📨 WebSocket message received:', message.body);

            try {
              const event: ParkingEvent = JSON.parse(message.body);
              console.log('🎯 Parsed event:', event);
              onMessage(event);
            } catch (error) {
              console.error('❌ Failed to parse WebSocket message:', error);
            }
          });

          resolve();
        },

        onStompError: (frame) => {
          console.error('❌ WebSocket STOMP error:', frame.headers['message']);
          console.error('Details:', frame.body);
          this.isConnected = false;

          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`🔄 Reconnecting... Attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
          } else {
            reject(new Error('WebSocket connection failed'));
          }
        },

        onWebSocketError: (error) => {
          console.error('❌ WebSocket error:', error);
          this.isConnected = false;
        },

        onDisconnect: () => {
          console.log('🔌 WebSocket disconnected');
          this.isConnected = false;
        },
      });

      this.client.activate();
    });
  }

  /**
   * 🔌 Disconnect from WebSocket
   */
  disconnect(): void {
    console.log('🔌 Disconnecting WebSocket...');

    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }

    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }

    this.isConnected = false;
  }

  /**
   * ✅ Check if connected
   */
  getIsConnected(): boolean {
    return this.isConnected;
  }
}

// Singleton instance
export const websocketService = new WebSocketService();
