import { useEffect, useRef } from 'react';
import { websocketService, ParkingEvent } from '@/services/websocketService';

interface UseWebSocketNotificationsProps {
  driverId: string;
  onEntryDetected: (event: ParkingEvent) => void;
  onExitDetected: (event: ParkingEvent) => void;
  enabled?: boolean;
}

/**
 * 🔔 React Hook pour recevoir les notifications WebSocket en temps réel
 */
export const useWebSocketNotifications = ({
  driverId,
  onEntryDetected,
  onExitDetected,
  enabled = true,
}: UseWebSocketNotificationsProps) => {
  const isConnectedRef = useRef(false);

  useEffect(() => {
    if (!enabled || !driverId) {
      console.log('⏸️ WebSocket disabled or no driverId');
      return;
    }

    // Avoid duplicate connections
    if (isConnectedRef.current) {
      console.log('⏸️ WebSocket already connected');
      return;
    }

    console.log('🚀 Initializing WebSocket for driver:', driverId);

    const handleMessage = (event: ParkingEvent) => {
      console.log('🎯 Received parking event:', event);

      if (event.event === 'ENTRY_DETECTED') {
        console.log('✅ ENTRY_DETECTED - Starting timer');
        onEntryDetected(event);
      } else if (event.event === 'EXIT_DETECTED') {
        console.log('✅ EXIT_DETECTED - Stopping timer');
        onExitDetected(event);
      }
    };

    websocketService
      .connect(driverId, handleMessage)
      .then(() => {
        console.log('✅ WebSocket connection established');
        isConnectedRef.current = true;
      })
      .catch((error) => {
        console.error('❌ WebSocket connection failed:', error);
        isConnectedRef.current = false;
      });

    // Cleanup on unmount
    return () => {
      console.log('🧹 Cleaning up WebSocket connection');
      websocketService.disconnect();
      isConnectedRef.current = false;
    };
  }, [driverId, enabled, onEntryDetected, onExitDetected]);

  return {
    isConnected: websocketService.getIsConnected(),
  };
};